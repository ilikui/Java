package cn.ilikui.libraryapi;

public class LibraryConflictException extends RuntimeException {
    private final String code;

    public LibraryConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
