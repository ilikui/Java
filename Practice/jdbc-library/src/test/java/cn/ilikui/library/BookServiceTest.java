package cn.ilikui.library;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.*;

class BookServiceTest {
    private Connection connection;
    private BookService service;
    private BookDao dao;

    @BeforeEach void setUp() throws SQLException {
        connection = DriverManager.getConnection("jdbc:h2:mem:service" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        dao = new JdbcBookDao(connection);
        service = new BookService(connection, dao);
    }
    @AfterEach void tearDown() throws SQLException { connection.close(); }

    @Test void borrowAndReturnUpdateStockAtomically() throws SQLException {
        long bookId = service.register(new Book(0, "Effective Java", "Joshua Bloch", new BigDecimal("58.00")), 1);
        assertEquals(1, service.availableCopies(bookId));
        long loanId = service.borrow(bookId, "Alice");
        assertEquals(0, service.availableCopies(bookId));
        assertThrows(IllegalStateException.class, () -> service.borrow(bookId, "Bob"));
        assertEquals(0, service.availableCopies(bookId), "failed borrowing must not change stock");
        service.returnBook(loanId);
        assertEquals(1, service.availableCopies(bookId));
        assertThrows(IllegalStateException.class, () -> service.returnBook(loanId));
        assertEquals(1, service.availableCopies(bookId), "duplicate return must not inflate stock");

        var history = service.findLoanHistory(bookId);
        assertEquals(1, history.size());
        assertEquals("Alice", history.get(0).borrower());
        assertNotNull(history.get(0).borrowedAt());
        assertNotNull(history.get(0).returnedAt());
        assertThrows(IllegalArgumentException.class, () -> service.findLoanHistory(0));
    }

    @Test void invalidRegistrationDoesNotCreateBook() throws SQLException {
        assertThrows(IllegalArgumentException.class, () -> service.register(new Book(0, "Title", "Author", BigDecimal.ONE), -1));
        assertTrue(dao.findAll().isEmpty());
    }

    @Test void missingBookCannotBeBorrowed() {
        assertThrows(IllegalArgumentException.class, () -> service.borrow(999, "Alice"));
    }
}