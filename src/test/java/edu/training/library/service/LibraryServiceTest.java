package edu.training.library.service;

import edu.training.library.db.Database;
import edu.training.library.db.LibraryRepository;
import edu.training.library.model.Models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.time.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class LibraryServiceTest {
    private static Database database;
    private LibraryRepository repository;
    private LibraryService service;
    private User reader;
    private User reader2;
    private Book singleCopyBook;

    private static void connectToMySql() {
        if (database != null) return;
        String url=System.getenv("LIBRARY_TEST_DB_URL");
        assumeTrue(url!=null&&!url.isBlank(),"未设置 LIBRARY_TEST_DB_URL，跳过 MySQL 集成测试");
        assumeTrue(url.startsWith("jdbc:mysql:"),"测试数据库必须是 MySQL");
        int nameStart=url.lastIndexOf('/')+1;
        int nameEnd=url.indexOf('?',nameStart);
        String databaseName=url.substring(nameStart,nameEnd<0?url.length():nameEnd).toLowerCase();
        assumeTrue(databaseName.contains("library_test"),"测试库名称必须包含 library_test");
        String user=System.getenv().getOrDefault("LIBRARY_TEST_DB_USER","library");
        String password=System.getenv().getOrDefault("LIBRARY_TEST_DB_PASSWORD","");
        Database candidate=new Database(url,user,password);
        try {
            candidate.initialize();
            database=candidate;
        } catch (RuntimeException e) {
            fail("无法连接 MySQL 测试库："+e.getMessage());
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        connectToMySql();
        clearTestData();
        repository=new LibraryRepository(database);
        service=new LibraryService(repository,at("2026-07-01T09:00:00"));
        reader=service.register("reader01","reader123","张同学","13800138001","reader01@test.cn",Role.READER);
        reader2=service.register("reader02","reader123","李同学","13800138002","reader02@test.cn",Role.READER);
        service.addBook("9780000000001","测试图书","测试作者","测试出版社","计算机",1,"T-01");
        singleCopyBook=service.books("书名","测试图书").getFirst();
    }

    private static void clearTestData() throws Exception {
        try(Connection connection=database.connect();Statement statement=connection.createStatement()) {
            statement.execute("SET FOREIGN_KEY_CHECKS=0");
            for(String table:new String[]{"fines","reservations","loans","books","categories","users"}) {
                statement.execute("TRUNCATE TABLE "+table);
            }
            statement.execute("SET FOREIGN_KEY_CHECKS=1");
        }
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
