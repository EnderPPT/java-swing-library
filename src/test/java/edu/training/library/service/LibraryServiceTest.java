package edu.training.library.service;

import edu.training.library.db.Database;
import edu.training.library.db.LibraryRepository;
import edu.training.library.model.Models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LibraryServiceTest {
    private LibraryRepository repository;
    private LibraryService service;
    private User reader;
    private User reader2;
    private Book singleCopyBook;

    @BeforeEach
    void setUp() {
        String url="jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        Database database=new Database(url,"sa","");
        database.initialize();
        repository=new LibraryRepository(database);
        service=new LibraryService(repository,at("2026-07-01T09:00:00"));
        reader=service.register("reader01","reader123","张同学","13800138001","reader01@test.cn",Role.READER);
        reader2=service.register("reader02","reader123","李同学","13800138002","reader02@test.cn",Role.READER);
        service.addBook("9780000000001","测试图书","测试作者","测试出版社","计算机",1,"T-01");
        singleCopyBook=service.books("书名","测试图书").getFirst();
    }

    @Test
    void authenticatesWithHashedPassword() {
        assertEquals(reader.id(),service.login("reader01","reader123").id());
        assertThrows(IllegalArgumentException.class,()->service.login("reader01","wrong-password"));
    }

    @Test
    void borrowsBookAndReducesAvailableStock() {
        service.borrow(reader.id(),singleCopyBook.id());
        assertEquals(0,service.books("书名","测试图书").getFirst().availableCopies());
        assertEquals(1,service.loans(reader.id(),true).size());
    }

    @Test
    void preventsDuplicateAndOutOfStockBorrowing() {
        service.borrow(reader.id(),singleCopyBook.id());
        assertThrows(IllegalArgumentException.class,()->service.borrow(reader.id(),singleCopyBook.id()));
        assertThrows(IllegalArgumentException.class,()->service.borrow(reader2.id(),singleCopyBook.id()));
    }

    @Test
    void overdueReturnCreatesFineAndRestoresStock() {
        long loanId=service.borrow(reader.id(),singleCopyBook.id());
        LibraryService lateService=new LibraryService(repository,at("2026-07-20T09:00:00"));
        lateService.returnBook(loanId);
        Fine fine=lateService.fines(reader.id()).getFirst();
        assertEquals(new BigDecimal("2.50"),fine.amount());
        assertEquals("UNPAID",fine.status());
        assertEquals(1,lateService.books("书名","测试图书").getFirst().availableCopies());
    }

    @Test
    void returnPromotesFirstWaitingReservationToReady() {
        long loanId=service.borrow(reader.id(),singleCopyBook.id());
        service.reserve(reader2.id(),singleCopyBook.id());
        assertEquals("WAITING",service.reservations(reader2.id()).getFirst().status());
        service.returnBook(loanId);
        Reservation ready=service.reservations(reader2.id()).getFirst();
        assertEquals("READY",ready.status());
        assertTrue(ready.notified());
        assertEquals(LocalDateTime.parse("2026-07-04T09:00:00"),ready.expiresAt());
    }

    @Test
    void reservationAndRenewalRulesAreEnforced() {
        long loanId=service.borrow(reader.id(),singleCopyBook.id());
        service.reserve(reader2.id(),singleCopyBook.id());
        assertThrows(IllegalArgumentException.class,()->service.reserve(reader2.id(),singleCopyBook.id()));
        assertThrows(IllegalArgumentException.class,()->service.renew(loanId));
    }

    @Test
    void renewalExtendsDueDateOnlyOnce() {
        long loanId=service.borrow(reader.id(),singleCopyBook.id());
        service.renew(loanId);
        Loan loan=service.loans(reader.id(),true).getFirst();
        assertEquals(LocalDateTime.parse("2026-07-29T09:00:00"),loan.dueAt());
        assertEquals(1,loan.renewCount());
        assertThrows(IllegalArgumentException.class,()->service.renew(loanId));
    }

    @Test
    void unpaidFineBlocksBorrowingUntilPaid() {
        long loanId=service.borrow(reader.id(),singleCopyBook.id());
        LibraryService lateService=new LibraryService(repository,at("2026-07-20T09:00:00"));
        lateService.returnBook(loanId);
        Fine fine=lateService.fines(reader.id()).getFirst();
        assertThrows(IllegalArgumentException.class,()->lateService.borrow(reader.id(),singleCopyBook.id()));
        lateService.payFine(fine.id(),reader.id());
        assertDoesNotThrow(()->lateService.borrow(reader.id(),singleCopyBook.id()));
    }

    @Test
    void suspendedCardCannotBorrow() {
        service.updateCard(reader.id(),"SUSPENDED");
        assertThrows(IllegalArgumentException.class,()->service.borrow(reader.id(),singleCopyBook.id()));
    }

    private static Clock at(String localDateTime) {
        return Clock.fixed(LocalDateTime.parse(localDateTime).toInstant(ZoneOffset.ofHours(8)),ZoneId.of("Asia/Shanghai"));
    }
}
