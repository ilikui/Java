CREATE TABLE books (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    author VARCHAR(120) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    CONSTRAINT chk_books_price CHECK (price >= 0)
) ENGINE=InnoDB;

CREATE INDEX idx_books_title ON books(title);
CREATE INDEX idx_books_author ON books(author);
