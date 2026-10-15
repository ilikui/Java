package cn.ilikui.libraryapi;

import java.time.OffsetDateTime;

public record StockAuditRecord(
        long id,
        long bookId,
        Long loanId,
        String action,
        int quantity,
        int availableAfter,
        OffsetDateTime occurredAt
) {
}
