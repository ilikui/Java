package cn.ilikui.libraryapi;

import java.time.OffsetDateTime;

public record StockAdjustmentRecord(
        long id,
        long bookId,
        int oldTotalCopies,
        int newTotalCopies,
        int oldAvailableCopies,
        int newAvailableCopies,
        OffsetDateTime occurredAt
) {
}
