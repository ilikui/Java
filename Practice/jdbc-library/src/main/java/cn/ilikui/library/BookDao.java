package cn.ilikui.library;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface BookDao {
    long create(Book book) throws SQLException;
    Optional<Book> findById(long id) throws SQLException;
    List<Book> findAll() throws SQLException;
    boolean update(Book book) throws SQLException;
    boolean deleteById(long id) throws SQLException;
}