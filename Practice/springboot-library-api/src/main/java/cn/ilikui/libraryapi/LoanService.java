package cn.ilikui.libraryapi;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class LoanService {
    private static final RowMapper<LoanRecord> LOAN_MAPPER = (rs, rowNum) -> {
        Timestamp borrowed = rs.getTimestamp("borrowed_at");
        Timestamp returned = rs.getTimestamp("returned_at");
        return new LoanRecord(rs.getLong("id"), rs.getLong("book_id"), rs.getString("borrower_name"),
                borrowed == null ? null : borrowed.toInstant().atOffset(ZoneOffset.UTC),
                returned == null ? null : returned.toInstant().atOffset(ZoneOffset.UTC),
                rs.getString("status"));
    };
    private static final RowMapper<StockAuditRecord> AUDIT_MAPPER = (rs, rowNum) -> {
        long loanId = rs.getLong("loan_id");
        Long nullableLoanId = rs.wasNull() ? null : loanId;
        Timestamp occurred = rs.getTimestamp("occurred_at");
        return new StockAuditRecord(rs.getLong("id"), rs.getLong("book_id"), nullableLoanId,
                rs.getString("action"), rs.getInt("quantity"), rs.getInt("available_after"),
                occurred.toInstant().atOffset(ZoneOffset.UTC));
    };

    private record StockLevels(int totalCopies, int availableCopies) { }

    private final JdbcTemplate jdbc;

    public LoanService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public LoanRecord borrow(long bookId, BorrowRequest request) {
        Integer available;
        try {
            available = jdbc.queryForObject(
                    "SELECT available_copies FROM books WHERE id = ? FOR UPDATE", Integer.class, bookId);
        } catch (EmptyResultDataAccessException ex) {
            throw new BookNotFoundException(bookId);
        }
        if (available == null) throw new BookNotFoundException(bookId);
        if (available < 1) {
            throw new LibraryConflictException("OUT_OF_STOCK", "当前图书没有可借库存");
        }

        int remaining = available - 1;
        jdbc.update("UPDATE books SET available_copies = ? WHERE id = ?", remaining, bookId);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO book_loans(book_id, borrower_name, borrowed_at, status) VALUES (?, ?, ?, 'BORROWED')",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, bookId);
            statement.setString(2, request.borrowerName().trim());
            statement.setObject(3, now);
            return statement;
        }, keys);
        Number generatedKey = keys.getKey();
        if (generatedKey == null) throw new IllegalStateException("数据库未返回借阅记录 ID");
        long loanId = generatedKey.longValue();
        jdbc.update("INSERT INTO stock_audit(book_id, loan_id, action, quantity, available_after, occurred_at) VALUES (?, ?, 'BORROW', 1, ?, ?)",
                bookId, loanId, remaining, now);
        return findLoan(loanId);
    }

    @Transactional
    public LoanRecord returnBook(long loanId) {
        LoanRecord current = findLoanForUpdate(loanId);
        if ("RETURNED".equals(current.status())) {
            throw new LibraryConflictException("LOAN_ALREADY_RETURNED", "该借阅记录已经归还，不能重复归还");
        }

        Integer available;
        try {
            available = jdbc.queryForObject(
                    "SELECT available_copies FROM books WHERE id = ? FOR UPDATE", Integer.class, current.bookId());
        } catch (EmptyResultDataAccessException ex) {
            throw new BookNotFoundException(current.bookId());
        }
        if (available == null) throw new BookNotFoundException(current.bookId());
        int remaining = available + 1;
        jdbc.update("UPDATE books SET available_copies = ? WHERE id = ?", remaining, current.bookId());
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbc.update("UPDATE book_loans SET status = 'RETURNED', returned_at = ? WHERE id = ? AND status = 'BORROWED'",
                now, loanId);
        jdbc.update("INSERT INTO stock_audit(book_id, loan_id, action, quantity, available_after, occurred_at) VALUES (?, ?, 'RETURN', 1, ?, ?)",
                current.bookId(), loanId, remaining, now);
        return findLoan(loanId);
    }

    @Transactional(readOnly = true)
    public List<LoanRecord> listByBook(long bookId) {
        ensureBookExists(bookId);
        return jdbc.query("SELECT id, book_id, borrower_name, borrowed_at, returned_at, status FROM book_loans WHERE book_id = ? ORDER BY id DESC",
                LOAN_MAPPER, bookId);
    }

    @Transactional(readOnly = true)
    public StockResponse stock(long bookId) {
        ensureBookExists(bookId);
        return jdbc.queryForObject("SELECT id, total_copies, available_copies FROM books WHERE id = ?",
                (rs, rowNum) -> new StockResponse(rs.getLong("id"), rs.getInt("total_copies"),
                        rs.getInt("available_copies"), rs.getInt("total_copies") - rs.getInt("available_copies")), bookId);
    }
    @Transactional
    public StockResponse adjustStock(long bookId, StockAdjustmentRequest request) {
        StockLevels locked = jdbc.query(
                "SELECT total_copies, available_copies FROM books WHERE id = ? FOR UPDATE",
                (rs, rowNum) -> new StockLevels(
                        rs.getInt("total_copies"), rs.getInt("available_copies")),
                bookId).stream().findFirst().orElseThrow(() -> new BookNotFoundException(bookId));
        int oldTotal = locked.totalCopies();
        int oldAvailable = locked.availableCopies();
        int borrowed = oldTotal - oldAvailable;
        int newTotal = request.totalCopies();
        if (newTotal < borrowed) {
            throw new LibraryConflictException("STOCK_BELOW_BORROWED",
                    "总库存不能小于当前借出数量 " + borrowed);
        }

        int newAvailable = newTotal - borrowed;
        if (newTotal != oldTotal) {
            jdbc.update("UPDATE books SET total_copies = ?, available_copies = ? WHERE id = ?",
                    newTotal, newAvailable, bookId);
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            jdbc.update("INSERT INTO stock_adjustment_audit(book_id, old_total_copies, new_total_copies, old_available_copies, new_available_copies, occurred_at) VALUES (?, ?, ?, ?, ?, ?)",
                    bookId, oldTotal, newTotal, oldAvailable, newAvailable, now);
        }
        return new StockResponse(bookId, newTotal, newAvailable, borrowed);
    }

    @Transactional(readOnly = true)
    public List<StockAuditRecord> stockAudit(long bookId) {
        ensureBookExists(bookId);
        String sql = """
                SELECT id, book_id, loan_id, action, quantity, available_after, occurred_at
                FROM stock_audit WHERE book_id = ?
                UNION ALL
                SELECT id, book_id, CAST(NULL AS BIGINT) AS loan_id, 'ADJUST' AS action,
                       ABS(new_total_copies - old_total_copies) AS quantity,
                       new_available_copies AS available_after, occurred_at
                FROM stock_adjustment_audit WHERE book_id = ?
                ORDER BY occurred_at, id
                """;
        return jdbc.query(sql, AUDIT_MAPPER, bookId, bookId);
    }

    private LoanRecord findLoan(long id) {
        try {
            return jdbc.queryForObject("SELECT id, book_id, borrower_name, borrowed_at, returned_at, status FROM book_loans WHERE id = ?",
                    LOAN_MAPPER, id);
        } catch (EmptyResultDataAccessException ex) {
            throw new LoanNotFoundException(id);
        }
    }

    private LoanRecord findLoanForUpdate(long id) {
        try {
            return jdbc.queryForObject("SELECT id, book_id, borrower_name, borrowed_at, returned_at, status FROM book_loans WHERE id = ? FOR UPDATE",
                    LOAN_MAPPER, id);
        } catch (EmptyResultDataAccessException ex) {
            throw new LoanNotFoundException(id);
        }
    }

    private void ensureBookExists(long bookId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM books WHERE id = ?", Integer.class, bookId);
        if (count == null || count == 0) throw new BookNotFoundException(bookId);
    }
}
