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
                                {"title":"Java 实战","author":"张三","price":59.90,"isbn":"978-7-111-11111-1"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Java 实战"))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(created).get("id").asLong();

        mvc.perform(get("/api/books/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.author").value("张三"));
        mvc.perform(put("/api/books/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Java 深入","author":"李四","price":69.00,"isbn":"9787111111128"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Java 深入"));
        mvc.perform(delete("/api/books/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/books/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectInvalidBookPayload() throws Exception {
        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON)
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

    @Test
    void shouldSearchAndPaginateBooks() throws Exception {
        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"分页搜索 Java A","author":"测试作者","price":10,"isbn":"9787111111135"}
                        """)).andExpect(status().isCreated());
        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"分页搜索 Java B","author":"测试作者","price":20,"isbn":"9787111111142"}
                        """)).andExpect(status().isCreated());
        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"其他书籍","author":"另一位作者","price":30,"isbn":"9787111111159"}
                        """)).andExpect(status().isCreated());

        mvc.perform(get("/api/books").param("q", "分页搜索 Java").param("page", "0")
                        .param("size", "1").param("sortBy", "title").param("direction", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].title").value("分页搜索 Java B"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1));
    }

    @Test
    void shouldFilterByPriceRangeAndReturnStableErrorCodes() throws Exception {
        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"价格过滤测试 A","author":"价格作者","price":10,"isbn":"9787111222239"}
                        """)).andExpect(status().isCreated());
        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"价格过滤测试 B","author":"价格作者","price":20,"isbn":"9787111222246"}
                        """)).andExpect(status().isCreated());

        mvc.perform(get("/api/books").param("q", "价格过滤测试").param("minPrice", "15").param("maxPrice", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].title").value("价格过滤测试 B"));
        mvc.perform(get("/api/books").param("minPrice", "30").param("maxPrice", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_QUERY"));
        mvc.perform(get("/api/books/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOOK_NOT_FOUND"));
    }

    @Test
    void shouldRejectDuplicateIsbnAfterNormalization() throws Exception {
        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"ISBN 唯一性 A","author":"测试作者","price":10,"isbn":"978-7-111-22222-2"}
                                """))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"ISBN 唯一性 B","author":"测试作者","price":12,"isbn":"9787111222222"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_ISBN"));
    }

    @Test
    void shouldRejectInvalidPaginationParameters() throws Exception {
        mvc.perform(get("/api/books").param("page", "-1"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/books").param("size", "101"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/books").param("sortBy", "title desc, id"))
                .andExpect(status().isBadRequest());
    }
}