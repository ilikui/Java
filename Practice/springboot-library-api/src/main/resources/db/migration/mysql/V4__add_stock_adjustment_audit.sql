CREATE TABLE stock_adjustment_audit (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    book_id BIGINT NOT NULL,
    old_total_copies INTEGER NOT NULL,
    new_total_copies INTEGER NOT NULL,
    old_available_copies INTEGER NOT NULL,
    new_available_copies INTEGER NOT NULL,
    occurred_at DATETIME(6) NOT NULL COMMENT 'UTC instant',
    CONSTRAINT chk_stock_adjustment_old_total CHECK (old_total_copies >= 0),
    CONSTRAINT chk_stock_adjustment_new_total CHECK (new_total_copies >= 0),
    CONSTRAINT chk_stock_adjustment_old_available CHECK (old_available_copies >= 0),
    CONSTRAINT chk_stock_adjustment_new_available CHECK (new_available_copies >= 0),
    CONSTRAINT fk_stock_adjustment_book FOREIGN KEY (book_id) REFERENCES books(id)
) ENGINE=InnoDB AUTO_INCREMENT=1000000000;

CREATE INDEX idx_stock_adjustment_book_time ON stock_adjustment_audit(book_id, occurred_at);
