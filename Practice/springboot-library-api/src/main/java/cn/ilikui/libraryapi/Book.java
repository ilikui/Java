package cn.ilikui.libraryapi;

import java.math.BigDecimal;

public record Book(long id, String title, String author, BigDecimal price) {
}