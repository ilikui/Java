package cn.ilikui.library;

import java.math.BigDecimal;
import java.util.Objects;

public record Book(long id, String title, String author, BigDecimal price) {
    public Book {
        if (id < 0) throw new IllegalArgumentException("id must not be negative");
        title = requireText(title, "title");
        author = requireText(author, "author");
        Objects.requireNonNull(price, "price");
        if (price.signum() < 0) throw new IllegalArgumentException("price must not be negative");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value.trim();
    }
}