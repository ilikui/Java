package cn.ilikui.libraryapi;

public class LoanNotFoundException extends RuntimeException {
    public LoanNotFoundException(long id) {
        super("未找到 ID 为 " + id + " 的借阅记录");
    }
}
