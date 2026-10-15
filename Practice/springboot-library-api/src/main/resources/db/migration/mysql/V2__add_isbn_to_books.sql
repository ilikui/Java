ALTER TABLE books ADD COLUMN isbn VARCHAR(20) NULL;
CREATE UNIQUE INDEX uk_books_isbn ON books(isbn);
