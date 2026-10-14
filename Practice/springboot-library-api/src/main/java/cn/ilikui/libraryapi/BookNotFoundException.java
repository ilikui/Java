package cn.ilikui.libraryapi;

public class BookNotFoundException extends RuntimeException {
    public BookNotFoundException(long id) {
        super("未找到 ID 为 " + id + " 的图书");
    }
}