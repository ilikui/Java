package cn.ilikui.libraryapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises Flyway migrations and stock transactions against a real PostgreSQL engine.
 * This test is skipped automatically when Docker is unavailable; regular H2 tests remain independent.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class PostgresLibraryApiIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void postgresMigrationsAndStockAdjustmentRemainConsistent() throws Exception {
        String created = mvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"PostgreSQL 集成测试图书","author":"集成测试","price":39.90}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long bookId = objectMapper.readTree(created).get("id").asLong();

        mvc.perform(post("/api/books/{bookId}/loans", bookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"borrowerName":"PostgreSQL 测试读者"}
                                """))
                .andExpect(status().isCreated());

        mvc.perform(patch("/api/books/{bookId}/stock", bookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"totalCopies":0}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STOCK_BELOW_BORROWED"));

        mvc.perform(patch("/api/books/{bookId}/stock", bookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"totalCopies":4}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCopies").value(4))
                .andExpect(jsonPath("$.availableCopies").value(3))
                .andExpect(jsonPath("$.borrowedCopies").value(1));

        mvc.perform(get("/api/books/{bookId}/stock-audit", bookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].action").value("BORROW"))
                .andExpect(jsonPath("$[1].action").value("ADJUST"));
    }
}
