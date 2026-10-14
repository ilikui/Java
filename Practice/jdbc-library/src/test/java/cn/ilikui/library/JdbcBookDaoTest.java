package cn.ilikui.library;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.*;

class JdbcBookDaoTest {
    private Connection connection;
    private BookDao dao;

    @BeforeEach void setUp() throws SQLException {
        connection = DriverManager.getConnection("jdbc:h2:mem:test" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        dao = new JdbcBookDao(connection);
    }

    @org.junit.jupiter.api.AfterEach void tearDown() throws SQLException { connection.close(); }

    @Test void createFindUpdateAndDelete() throws SQLException {
        long id = dao.create(new Book(0, "Java Basics", "Kai", new BigDecimal("19.90")));
        Book inserted = dao.findById(id).orElseThrow();
        assertEquals("Java Basics", inserted.title());
        assertTrue(dao.update(new Book(id, "Java Advanced", "Kai", new BigDecimal("29.90"))));
        assertEquals(new BigDecimal("29.90"), dao.findById(id).orElseThrow().price());
        assertEquals(1, dao.findAll().size());
        assertTrue(dao.deleteById(id));
        assertTrue(dao.findById(id).isEmpty());
        assertFalse(dao.deleteById(id));
    }

    @Test void usesPreparedStatementsAndStoresTextLiterally() throws SQLException {
        String title = "Java' OR '1'='1";
        long id = dao.create(new Book(0, title, "Author", new BigDecimal("1.00")));
        assertEquals(title, dao.findById(id).orElseThrow().title());
        assertEquals(1, dao.findAll().size());
    }

    @Test void rejectsInvalidBook() {
        assertThrows(IllegalArgumentException.class, () -> new Book(0, " ", "Author", BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> new Book(0, "Title", "Author", new BigDecimal("-1")));
    }
}