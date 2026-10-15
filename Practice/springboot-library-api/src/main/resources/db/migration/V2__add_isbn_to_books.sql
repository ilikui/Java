ALTER TABLE books ADD COLUMN isbn VARCHAR(20);

-- ISBN 统一去掉连字符并转为大写后写入；数据库负责兜底保证唯一性。
CREATE UNIQUE INDEX uk_books_isbn ON books(isbn);
