package cn.ilikui.library;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ConcurrentBorrowingTest {

    @Test
    void onlyOneBorrowSucceedsWhenSingleCopyIsRequestedConcurrently() throws Exception {
        String dbName = "concurrent" + System.nanoTime();
        String url = "jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000";
        long bookId;
        try (Connection setupConnection = DriverManager.getConnection(url)) {
            BookDao setupDao = new JdbcBookDao(setupConnection);
            BookService setupService = new BookService(setupConnection, setupDao);
            bookId = setupService.register(
                    new Book(0, "Java Concurrency in Practice", "Brian Goetz", new BigDecimal("68.00")), 1);
        }

        try (Connection firstConnection = DriverManager.getConnection(url);
             Connection secondConnection = DriverManager.getConnection(url)) {
            // Each worker gets its own Connection and service; no JDBC Connection is shared between threads.
            BookService firstService = new BookService(firstConnection, new JdbcBookDao(firstConnection));
            BookService secondService = new BookService(secondConnection, new JdbcBookDao(secondConnection));
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            ExecutorService pool = Executors.newFixedThreadPool(2);
            try {
                Future<Boolean> first = pool.submit(() -> tryBorrow(firstService, bookId, "Alice", ready, start));
                Future<Boolean> second = pool.submit(() -> tryBorrow(secondService, bookId, "Bob", ready, start));

                assertTrue(ready.await(5, TimeUnit.SECONDS), "workers did not become ready");
                start.countDown();

                boolean firstSucceeded = first.get(10, TimeUnit.SECONDS);
                boolean secondSucceeded = second.get(10, TimeUnit.SECONDS);
                assertNotEquals(firstSucceeded, secondSucceeded,
                        "exactly one borrower should succeed when stock is one");
                assertEquals(0, firstService.availableCopies(bookId));

                try (PreparedStatement ps = firstConnection.prepareStatement(
                        "SELECT COUNT(*) FROM loans WHERE book_id = ? AND returned_at IS NULL")) {
                    ps.setLong(1, bookId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertTrue(rs.next());
                        assertEquals(1, rs.getInt(1), "only one active loan should be persisted");
                    }
                }
            } finally {
                pool.shutdownNow();
                assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS), "worker pool did not stop");
            }
        }
    }

    private static boolean tryBorrow(BookService service, long bookId, String borrower,
                                     CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("timed out waiting for concurrent start");
        }
        try {
            service.borrow(bookId, borrower);
            return true;
        } catch (IllegalStateException noStock) {
            return false;
        }
    }
}
