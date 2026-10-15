ALTER TABLE books
    ADD COLUMN total_copies INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN available_copies INTEGER NOT NULL DEFAULT 1,
    ADD CONSTRAINT chk_books_total_copies CHECK (total_copies >= 0),
    ADD CONSTRAINT chk_books_available_copies CHECK (
        available_copies >= 0 AND available_copies <= total_copies
    );

CREATE TABLE book_loans (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    book_id BIGINT NOT NULL,
    borrower_name VARCHAR(120) NOT NULL,
    borrowed_at DATETIME(6) NOT NULL COMMENT 'UTC instant',
    returned_at DATETIME(6) NULL COMMENT 'UTC instant',
    status VARCHAR(16) NOT NULL,
    CONSTRAINT chk_book_loans_status CHECK (status IN ('BORROWED', 'RETURNED')),
    CONSTRAINT fk_book_loans_book FOREIGN KEY (book_id) REFERENCES books(id)
) ENGINE=InnoDB;

CREATE INDEX idx_book_loans_book_status ON book_loans(book_id, status);
CREATE INDEX idx_book_loans_borrower ON book_loans(borrower_name);

CREATE TABLE stock_audit (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    book_id BIGINT NOT NULL,
    loan_id BIGINT NULL,
    action VARCHAR(16) NOT NULL,
    quantity INTEGER NOT NULL,
    available_after INTEGER NOT NULL,
    occurred_at DATETIME(6) NOT NULL COMMENT 'UTC instant',
    CONSTRAINT chk_stock_audit_action CHECK (action IN ('BORROW', 'RETURN')),
    CONSTRAINT chk_stock_audit_quantity CHECK (quantity > 0),
    CONSTRAINT chk_stock_audit_available CHECK (available_after >= 0),
    CONSTRAINT fk_stock_audit_book FOREIGN KEY (book_id) REFERENCES books(id),
    CONSTRAINT fk_stock_audit_loan FOREIGN KEY (loan_id) REFERENCES book_loans(id)
) ENGINE=InnoDB;

CREATE INDEX idx_stock_audit_book_time ON stock_audit(book_id, occurred_at);
