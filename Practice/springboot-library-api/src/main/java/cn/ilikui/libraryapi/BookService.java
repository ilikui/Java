package cn.ilikui.libraryapi;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.PreparedStatement;
import java.util.List;

@Service
public class BookService {
    private static final RowMapper<Book> BOOK_MAPPER = (rs, rowNum) ->
            new Book(rs.getLong("id"), rs.getString("title"),
                    rs.getString("author"), rs.getBigDecimal("price"));
    private final JdbcTemplate jdbc;

    public BookService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Book> findAll() {
        return jdbc.query("SELECT id, title, author, price FROM books ORDER BY id", BOOK_MAPPER);
    }

    public Book findById(long id) {
        try {
            return jdbc.queryForObject("SELECT id, title, author, price FROM books WHERE id = ?", BOOK_MAPPER, id);
        } catch (EmptyResultDataAccessException ex) {
            throw new BookNotFoundException(id);
        }
    }

    @Transactional
    public Book create(BookRequest request) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO books(title, author, price) VALUES (?, ?, ?)",
                    new String[]{"id"});
            statement.setString(1, request.title().trim());
            statement.setString(2, request.author().trim());
            statement.setBigDecimal(3, request.price());
            return statement;
        }, keys);
        Number key = keys.getKey();
        if (key == null) {
            throw new IllegalStateException("数据库未返回新图书 ID");
        }
        return findById(key.longValue());
    }

    @Transactional
    public Book update(long id, BookRequest request) {
        int changed = jdbc.update("UPDATE books SET title = ?, author = ?, price = ? WHERE id = ?",
                request.title().trim(), request.author().trim(), request.price(), id);
        if (changed == 0) throw new BookNotFoundException(id);
        return findById(id);
    }

    @Transactional
    public void delete(long id) {
        int changed = jdbc.update("DELETE FROM books WHERE id = ?", id);
        if (changed == 0) throw new BookNotFoundException(id);
    }
}