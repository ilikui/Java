package cn.ilikui.libraryapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BookControllerTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void shouldCreateReadUpdateAndDeleteBook() throws Exception {
        String created = mvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Java 实战","author":"张三","price":59.90}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Java 实战"))
                .andReturn().getResponse().getContentAsString();

        JsonNode node = objectMapper.readTree(created);
        long id = node.get("id").asLong();

        mvc.perform(get("/api/books/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.author").value("张三"));

        mvc.perform(put("/api/books/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Java 深入","author":"李四","price":69.00}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Java 深入"))
                .andExpect(jsonPath("$.price").value(69.0));

        mvc.perform(delete("/api/books/{id}", id))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/books/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectInvalidBookPayload() throws Exception {
        mvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":" ","author":"","price":-1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("书名不能为空")));
    }

    @Test
    void shouldReturnNotFoundForUnknownId() throws Exception {
        mvc.perform(get("/api/books/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail", containsString("未找到")));
    }
}