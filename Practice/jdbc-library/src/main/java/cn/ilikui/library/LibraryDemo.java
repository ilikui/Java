package cn.ilikui.library;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;

public final class LibraryDemo {
    private LibraryDemo() {}
    public static void main(String[] args) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:library;DB_CLOSE_DELAY=-1")) {
            BookDao dao = new JdbcBookDao(connection);
            BookService service = new BookService(connection, dao);
            long id = service.register(new Book(0, "Effective Java", "Joshua Bloch", new BigDecimal("58.00")), 2);
            System.out.println("Created book: " + dao.findById(id).orElseThrow());
            System.out.println("Available copies: " + service.availableCopies(id));
            long loanId = service.borrow(id, "Alice");
            System.out.println("Loan id: " + loanId);
            System.out.println("After borrowing: " + service.availableCopies(id));
            service.returnBook(loanId);
            System.out.println("After return: " + service.availableCopies(id));
        }
    }
}
