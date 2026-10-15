package cn.ilikui.libraryapi;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class LoanController {
    private final LoanService service;

    public LoanController(LoanService service) {
        this.service = service;
    }

    @PostMapping("/api/books/{bookId}/loans")
    @ResponseStatus(HttpStatus.CREATED)
    public LoanRecord borrow(@PathVariable long bookId, @Valid @RequestBody BorrowRequest request) {
        return service.borrow(bookId, request);
    }

    @PostMapping("/api/loans/{loanId}/return")
    public LoanRecord returnBook(@PathVariable long loanId) {
        return service.returnBook(loanId);
    }

    @GetMapping("/api/books/{bookId}/loans")
    public java.util.List<LoanRecord> listByBook(@PathVariable long bookId) {
        return service.listByBook(bookId);
    }

    @GetMapping("/api/books/{bookId}/stock")
    public StockResponse stock(@PathVariable long bookId) {
        return service.stock(bookId);
    }

    @PatchMapping("/api/books/{bookId}/stock")
    public StockResponse adjustStock(@PathVariable long bookId,
                                     @Valid @RequestBody StockAdjustmentRequest request) {
        return service.adjustStock(bookId, request);
    }

    @GetMapping("/api/books/{bookId}/stock-audit")
    public java.util.List<StockAuditRecord> stockAudit(@PathVariable long bookId) {
        return service.stockAudit(bookId);
    }
}
