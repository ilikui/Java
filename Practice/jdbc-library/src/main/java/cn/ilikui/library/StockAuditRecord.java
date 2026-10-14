package cn.ilikui.library;

import java.time.LocalDateTime;

/** Immutable audit entry describing one committed stock adjustment. */
public record StockAuditRecord(long id, long bookId, String operation,
                               int beforeCount, int afterCount, String operator,
                               String reason, LocalDateTime occurredAt) {
}
