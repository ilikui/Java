package cn.ilikui.libraryapi;

import java.time.OffsetDateTime;

public record LoanRecord(
        long id,
        long bookId,
        String borrowerName,
        OffsetDateTime borrowedAt,
        OffsetDateTime returnedAt,
        String status
) {
}
