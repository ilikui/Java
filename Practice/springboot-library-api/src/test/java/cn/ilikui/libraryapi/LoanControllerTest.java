package cn.ilikui.libraryapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@SpringBootTest
@AutoConfigureMockMvc
class LoanControllerTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void shouldBorrowReturnAndAuditStockAtomically() throws Exception {
        String created = mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"借阅流程测试书","author":"测试作者","price":25}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long bookId = objectMapper.readTree(created).get("id").asLong();

        mvc.perform(get("/api/books/{bookId}/stock", bookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCopies").value(1))
                .andExpect(jsonPath("$.availableCopies").value(1))
                .andExpect(jsonPath("$.borrowedCopies").value(0));

        String loanJson = mvc.perform(post("/api/books/{bookId}/loans", bookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"borrowerName":"测试读者"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookId").value(bookId))
                .andExpect(jsonPath("$.borrowerName").value("测试读者"))
                .andExpect(jsonPath("$.status").value("BORROWED"))
                .andReturn().getResponse().getContentAsString();
        long loanId = objectMapper.readTree(loanJson).get("id").asLong();

        mvc.perform(get("/api/books/{bookId}/stock", bookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableCopies").value(0))
                .andExpect(jsonPath("$.borrowedCopies").value(1));

        mvc.perform(post("/api/books/{bookId}/loans", bookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"borrowerName":"第二位读者"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OUT_OF_STOCK"));

        mvc.perform(post("/api/loans/{loanId}/return", loanId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"))
                .andExpect(jsonPath("$.returnedAt").isNotEmpty());

        mvc.perform(post("/api/loans/{loanId}/return", loanId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LOAN_ALREADY_RETURNED"));

        mvc.perform(delete("/api/books/{bookId}", bookId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOK_HAS_LOAN_HISTORY"));

        mvc.perform(get("/api/books/{bookId}/loans", bookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("RETURNED"));

        mvc.perform(get("/api/books/{bookId}/stock", bookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableCopies").value(1))
                .andExpect(jsonPath("$.borrowedCopies").value(0));

        mvc.perform(get("/api/books/{bookId}/stock-audit", bookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].action").value("BORROW"))
                .andExpect(jsonPath("$[0].availableAfter").value(0))
                .andExpect(jsonPath("$[1].action").value("RETURN"))
                .andExpect(jsonPath("$[1].availableAfter").value(1));
    }

    @Test
    void concurrentBorrowRequestsMustNotOverdrawLastCopy() throws Exception {
        String created = mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"并发借阅最后一本测试书","author":"并发测试","price":19}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long bookId = objectMapper.readTree(created).get("id").asLong();

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = pool.submit(() -> {
                start.await();
                return mvc.perform(post("/api/books/{bookId}/loans", bookId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"borrowerName":"并发读者甲"}
                                        """))
                        .andReturn().getResponse().getStatus();
            });
            Future<Integer> second = pool.submit(() -> {
                start.await();
                return mvc.perform(post("/api/books/{bookId}/loans", bookId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"borrowerName":"并发读者乙"}
                                        """))
                        .andReturn().getResponse().getStatus();
            });
            start.countDown();
            List<Integer> statuses = List.of(first.get(), second.get());
            org.junit.jupiter.api.Assertions.assertEquals(1,
                    statuses.stream().filter(status -> status == 201).count());
            org.junit.jupiter.api.Assertions.assertEquals(1,
                    statuses.stream().filter(status -> status == 409).count());
            mvc.perform(get("/api/books/{bookId}/stock", bookId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.availableCopies").value(0))
                    .andExpect(jsonPath("$.borrowedCopies").value(1));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void shouldValidateBorrowerAndReportMissingLoan() throws Exception {
        mvc.perform(post("/api/books/1/loans").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"borrowerName":" "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mvc.perform(post("/api/loans/999999/return"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LOAN_NOT_FOUND"));
    }
}
