package cn.ilikui.libraryapi;

import java.util.List;

public record BookPageResponse(
        List<Book> items,
        long totalElements,
        int page,
        int size,
        int totalPages
) {
}