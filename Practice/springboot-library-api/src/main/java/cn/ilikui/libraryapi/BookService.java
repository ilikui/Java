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
import java.util.Set;

@Service
public class BookService {
    private static final RowMapper<Book> BOOK_MAPPER = (rs, rowNum) ->
            new Book(rs.getLong("id"), rs.getString("title"),
                    rs.getString("author"), rs.getBigDecimal("price"));
    private static final Set<String> SORT_COLUMNS = Set.of("id", "title", "author", "price");
    private final JdbcTemplate jdbc;

    public BookService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Book> findAll() {
        return jdbc.query("SELECT id, title, author, price FROM books ORDER BY id", BOOK_MAPPER);
    }

    public BookPageResponse findPage(String query, int page, int size, String sortBy, String direction) {
        if (page < 0) throw new IllegalArgumentException("page 不能小于 0");
        if (size < 1 || size > 100) throw new IllegalArgumentException("size 必须在 1 到 100 之间");
        if (!SORT_COLUMNS.contains(sortBy)) throw new IllegalArgumentException("sortBy 仅支持 id、title、author、price");
        String normalizedDirection = direction == null ? "" : direction.toLowerCase(java.util.Locale.ROOT);
        if (!normalizedDirection.equals("asc") && !normalizedDirection.equals("desc")) {
            throw new IllegalArgumentException("direction 仅支持 asc 或 desc");
        }

        String normalizedQuery = query == null ? "" : query.trim();
        String filter = "%" + normalizedQuery + "%";
        long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM books WHERE (? = '' OR LOWER(title) LIKE LOWER(?) OR LOWER(author) LIKE LOWER(?))",
                Long.class, normalizedQuery, filter, filter);
        String sql = "SELECT id, title, author, price FROM books " +
                "WHERE (? = '' OR LOWER(title) LIKE LOWER(?) OR LOWER(author) LIKE LOWER(?)) " +
                "ORDER BY " + sortBy + " " + normalizedDirection +
                " LIMIT ? OFFSET ?";
        List<Book> items = jdbc.query(sql, BOOK_MAPPER, normalizedQuery, filter, filter, size, (long) page * size);
        int totalPages = total == 0 ? 0 : (int) ((total + size - 1) / size);
        return new BookPageResponse(items, total, page, size, totalPages);
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
        if (key == null) throw new IllegalStateException("数据库未返回新图书 ID");
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