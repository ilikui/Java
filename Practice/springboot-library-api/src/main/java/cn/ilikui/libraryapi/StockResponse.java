package cn.ilikui.libraryapi;

public record StockResponse(long bookId, int totalCopies, int availableCopies, int borrowedCopies) {
}
