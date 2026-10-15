package cn.ilikui.libraryapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Runs the MySQL-specific Flyway migrations and transactional behavior against MySQL 8.4. */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("mysql")
class MySqlLibraryApiIntegrationTest {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("library_api")
            .withUsername("library")
            .withPassword("library");

    private static String mysqlUrl() {
        return MYSQL.getJdbcUrl() + (MYSQL.getJdbcUrl().contains("?") ? "&" : "?")
                + "connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true";
    }

    @DynamicPropertySource
    static void configureMySql(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MySqlLibraryApiIntegrationTest::mysqlUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        registry.add("spring.flyway.url", MySqlLibraryApiIntegrationTest::mysqlUrl);
        registry.add("spring.flyway.user", MYSQL::getUsername);
        registry.add("spring.flyway.password", MYSQL::getPassword);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration/mysql");
    }

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void mysqlMigrationsReachV4AndBookCanBeBorrowedAndAdjusted() throws Exception {
        assertEquals("4", jdbc.queryForObject(
                "SELECT version FROM flyway_schema_history WHERE success = TRUE ORDER BY installed_rank DESC LIMIT 1",
                String.class));

        String created = mvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"MySQL 集成测试图书","author":"集成测试","price":39.90,"isbn":"978-7-111-11111-1"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long bookId = objectMapper.readTree(created).get("id").asLong();

        mvc.perform(post("/api/books/{bookId}/loans", bookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"borrowerName":"MySQL 测试读者"}
                                """))
                .andExpect(status().isCreated());

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

    @Test
    void concurrentBorrowersCannotTakeTheLastCopyTwice() throws Exception {
        String created = mvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"MySQL 并发库存测试","author":"集成测试","price":19.90}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long bookId = objectMapper.readTree(created).get("id").asLong();

        int workers = 2;
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(workers);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < workers; i++) {
                final int workerId = i;
                results.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    return mvc.perform(post("/api/books/{bookId}/loans", bookId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                                            {"borrowerName":"MySQL 并发读者-%d"}
                                            """.formatted(workerId)))
                            .andReturn().getResponse().getStatus();
                }));
            }
            ready.await();
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> result : results) statuses.add(result.get());
            assertEquals(1, statuses.stream().filter(code -> code == 201).count());
            assertEquals(1, statuses.stream().filter(code -> code == 409).count());
        } finally {
            pool.shutdownNow();
        }

        mvc.perform(get("/api/books/{bookId}/stock", bookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableCopies").value(0))
                .andExpect(jsonPath("$.borrowedCopies").value(1));
    }

    @Test
    void stockAdjustmentRollsBackWhenAuditInsertFails() throws Exception {
        String created = mvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"MySQL 事务回滚测试","author":"集成测试","price":29.90}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long bookId = objectMapper.readTree(created).get("id").asLong();

        jdbc.execute("""
                CREATE TRIGGER fail_stock_adjustment_audit
                BEFORE INSERT ON stock_adjustment_audit
                FOR EACH ROW
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'forced stock audit failure'
                """);
        try {
            boolean requestFailed;
            try {
                int responseStatus = mvc.perform(patch("/api/books/{bookId}/stock", bookId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"totalCopies":5}
                                        """))
                        .andReturn().getResponse().getStatus();
                requestFailed = responseStatus >= 500;
            } catch (Exception expectedDatabaseFailure) {
                requestFailed = true;
            }
            assertTrue(requestFailed, "audit trigger should fail the stock adjustment request");

            mvc.perform(get("/api/books/{bookId}/stock", bookId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCopies").value(1))
                    .andExpect(jsonPath("$.availableCopies").value(1))
                    .andExpect(jsonPath("$.borrowedCopies").value(0));
            mvc.perform(get("/api/books/{bookId}/stock-audit", bookId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        } finally {
            jdbc.execute("DROP TRIGGER IF EXISTS fail_stock_adjustment_audit");
        }
    }
}
