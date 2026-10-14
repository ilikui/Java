package cn.ilikui.library;

import java.time.LocalDateTime;

/** Read model for a book's borrowing history; returnedAt is null while the loan is active. */
public record LoanRecord(long id, long bookId, String borrower,
                         LocalDateTime borrowedAt, LocalDateTime returnedAt) {
}
