package cn.ilikui.libraryapi;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
public class BookService {
    private static final RowMapper<Book> BOOK_MAPPER = (rs, rowNum) ->
            new Book(rs.getLong("id"), rs.getString("title"),
                    rs.getString("author"), rs.getBigDecimal("price"), rs.getString("isbn"));
    private static final Set<String> SORT_COLUMNS = Set.of("id", "title", "author", "price", "isbn");
    private final JdbcTemplate jdbc;

    public BookService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Book> findAll() {
        return jdbc.query("SELECT id, title, author, price, isbn FROM books ORDER BY id", BOOK_MAPPER);
    }

    public BookPageResponse findPage(String query, int page, int size, String sortBy, String direction,
                                     BigDecimal minPrice, BigDecimal maxPrice) {
        if (page < 0) throw new IllegalArgumentException("page 不能小于 0");
        if (size < 1 || size > 100) throw new IllegalArgumentException("size 必须在 1 到 100 之间");
        if (!SORT_COLUMNS.contains(sortBy)) throw new IllegalArgumentException("sortBy 仅支持 id、title、author、price、isbn");
        String normalizedDirection = direction == null ? "" : direction.toLowerCase(java.util.Locale.ROOT);
        if (!normalizedDirection.equals("asc") && !normalizedDirection.equals("desc")) {
            throw new IllegalArgumentException("direction 仅支持 asc 或 desc");
        }
        if (minPrice != null && minPrice.signum() < 0) throw new IllegalArgumentException("minPrice 不能小于 0");
        if (maxPrice != null && maxPrice.signum() < 0) throw new IllegalArgumentException("maxPrice 不能小于 0");
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("minPrice 不能大于 maxPrice");
        }

        String normalizedQuery = query == null ? "" : query.trim();
        String filter = "%" + normalizedQuery + "%";
        String where = " WHERE (? = '' OR LOWER(title) LIKE LOWER(?) OR LOWER(author) LIKE LOWER(?))" +
                " AND (? IS NULL OR price >= ?) AND (? IS NULL OR price <= ?)";
        Object[] filterArgs = {normalizedQuery, filter, filter, minPrice, minPrice, maxPrice, maxPrice};
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM books" + where, Long.class, filterArgs);
        long total = count == null ? 0L : count;
        String sql = "SELECT id, title, author, price, isbn FROM books" + where +
                " ORDER BY " + sortBy + " " + normalizedDirection + ", id ASC LIMIT ? OFFSET ?";
        Object[] pageArgs = {normalizedQuery, filter, filter, minPrice, minPrice, maxPrice, maxPrice,
                size, (long) page * size};
        List<Book> items = jdbc.query(sql, BOOK_MAPPER, pageArgs);
        int totalPages = total == 0 ? 0 : (int) ((total + size - 1) / size);
        return new BookPageResponse(items, total, page, size, totalPages);
    }

    public Book findById(long id) {
        try {
            return jdbc.queryForObject("SELECT id, title, author, price, isbn FROM books WHERE id = ?", BOOK_MAPPER, id);
        } catch (EmptyResultDataAccessException ex) {
            throw new BookNotFoundException(id);
        }
    }

    @Transactional
    public Book create(BookRequest request) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO books(title, author, price, isbn) VALUES (?, ?, ?, ?)",
                    new String[]{"id"});
            statement.setString(1, request.title().trim());
            statement.setString(2, request.author().trim());
            statement.setBigDecimal(3, request.price());
            statement.setString(4, normalizeIsbn(request.isbn()));
            return statement;
        }, keys);
        Number key = keys.getKey();
        if (key == null) throw new IllegalStateException("数据库未返回新图书 ID");
        return findById(key.longValue());
    }

    @Transactional
    public Book update(long id, BookRequest request) {
        int changed = jdbc.update("UPDATE books SET title = ?, author = ?, price = ?, isbn = ? WHERE id = ?",
                request.title().trim(), request.author().trim(), request.price(), normalizeIsbn(request.isbn()), id);
        if (changed == 0) throw new BookNotFoundException(id);
        return findById(id);
    }

    private String normalizeIsbn(String isbn) {
        if (isbn == null || isbn.isBlank()) return null;
        return isbn.trim().replace("-", "").toUpperCase(java.util.Locale.ROOT);
    }

    @Transactional
    public void delete(long id) {
        int changed = jdbc.update("DELETE FROM books WHERE id = ?", id);
        if (changed == 0) throw new BookNotFoundException(id);
    }
}
