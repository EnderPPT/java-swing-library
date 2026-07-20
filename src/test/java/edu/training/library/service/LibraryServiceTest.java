package edu.training.library.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import edu.training.library.db.Database;
import edu.training.library.db.LibraryRepository;
import edu.training.library.model.Models.*;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LibraryServiceTest {
    private static Database database;
    private LibraryRepository repository;
    private LibraryService service;
    private User reader;
    private User reader2;
    private Book singleCopyBook;

    private static void connectToMySql() {
        if (database != null) return;
        String url = setting("LIBRARY_TEST_DB_URL");
        assumeTrue(url != null && !url.isBlank(), "未设置 LIBRARY_TEST_DB_URL，跳过 MySQL 集成测试");
        assumeTrue(url.startsWith("jdbc:mysql:"), "测试数据库必须是 MySQL");
        int queryStart = url.indexOf('?');
        String base = queryStart < 0 ? url : url.substring(0, queryStart);
        String databaseName = base.substring(base.lastIndexOf('/') + 1).toLowerCase();
        assumeTrue(databaseName.contains("library_test"), "测试库名称必须包含 library_test");
        String user = setting("LIBRARY_TEST_DB_USER", "library");
        String password = setting("LIBRARY_TEST_DB_PASSWORD", "");
        Database candidate = new Database(url, user, password);
        try {
            candidate.initialize();
            database = candidate;
        } catch (RuntimeException e) {
            fail("无法连接 MySQL 测试库：" + e.getMessage());
        }
    }

    private static String setting(String name) {
        return setting(name, null);
    }

    private static String setting(String name, String defaultValue) {
        return System.getProperty(name, System.getenv().getOrDefault(name, defaultValue));
    }

    @BeforeEach
    void setUp() throws Exception {
        connectToMySql();
        clearTestData();
        repository = new LibraryRepository(database);
        service = new LibraryService(repository, at("2026-07-01T09:00:00"));
        reader =
                service.register(
                        "reader01",
                        "reader123",
                        "张同学",
                        "13800138001",
                        "reader01@test.cn",
                        Role.READER);
        reader2 =
                service.register(
                        "reader02",
                        "reader123",
                        "李同学",
                        "13800138002",
                        "reader02@test.cn",
                        Role.READER);
        service.addBook("9780000000001", "测试图书", "测试作者", "测试出版社", "计算机", 1, "T-01");
        singleCopyBook = service.books("书名", "测试图书").getFirst();
    }

    private static void clearTestData() throws Exception {
        try (Connection connection = database.connect();
                Statement statement = connection.createStatement()) {
            statement.execute("SET FOREIGN_KEY_CHECKS=0");
            for (String table :
                    new String[] {
                        "fines", "reservations", "loans", "books", "categories", "users"
                    }) {
                statement.execute("TRUNCATE TABLE " + table);
            }
            statement.execute("SET FOREIGN_KEY_CHECKS=1");
        }
    }

    @Test
    void authenticatesWithHashedPassword() {
        assertEquals(reader.id(), service.login("reader01", "reader123").id());
        assertThrows(
                IllegalArgumentException.class, () -> service.login("reader01", "wrong-password"));
    }

    @Test
    void borrowsBookAndReducesAvailableStock() {
        service.borrow(reader.id(), singleCopyBook.id());
        assertEquals(0, service.books("书名", "测试图书").getFirst().availableCopies());
        assertEquals(1, service.loans(reader.id(), true).size());
    }

    @Test
    void preventsDuplicateAndOutOfStockBorrowing() {
        service.borrow(reader.id(), singleCopyBook.id());
        assertThrows(
                IllegalArgumentException.class,
                () -> service.borrow(reader.id(), singleCopyBook.id()));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.borrow(reader2.id(), singleCopyBook.id()));
    }

    @Test
    void overdueReturnCreatesFineAndRestoresStock() {
        long loanId = service.borrow(reader.id(), singleCopyBook.id());
        LibraryService lateService = new LibraryService(repository, at("2026-07-20T09:00:00"));
        lateService.returnBook(loanId);
        Fine fine = lateService.fines(reader.id()).getFirst();
        assertEquals(new BigDecimal("2.50"), fine.amount());
        assertEquals("UNPAID", fine.status());
        assertEquals(1, lateService.books("书名", "测试图书").getFirst().availableCopies());
    }

    @Test
    void returnPromotesFirstWaitingReservationToReady() {
        long loanId = service.borrow(reader.id(), singleCopyBook.id());
        service.reserve(reader2.id(), singleCopyBook.id());
        assertEquals("WAITING", service.reservations(reader2.id()).getFirst().status());
        service.returnBook(loanId);
        Reservation ready = service.reservations(reader2.id()).getFirst();
        assertEquals("READY", ready.status());
        assertTrue(ready.notified());
        assertEquals(LocalDateTime.parse("2026-07-04T09:00:00"), ready.expiresAt());
        assertEquals(0, service.books("书名", "测试图书").getFirst().availableCopies());
    }

    @Test
    void readyReservationHoldsStockUntilReaderBorrows() {
        service.reserve(reader.id(), singleCopyBook.id());

        assertEquals(0, service.books("书名", "测试图书").getFirst().availableCopies());
        assertThrows(
                IllegalArgumentException.class,
                () -> service.borrow(reader2.id(), singleCopyBook.id()));

        assertDoesNotThrow(() -> service.borrow(reader.id(), singleCopyBook.id()));
        assertEquals("FULFILLED", service.reservations(reader.id()).getFirst().status());
        assertEquals(0, service.books("书名", "测试图书").getFirst().availableCopies());
    }

    @Test
    void cancellingReadyReservationPromotesNextReader() {
        long firstReservation = service.reserve(reader.id(), singleCopyBook.id());
        service.reserve(reader2.id(), singleCopyBook.id());

        service.cancelReservation(firstReservation, reader.id());

        assertEquals("CANCELLED", service.reservations(reader.id()).getFirst().status());
        Reservation next = service.reservations(reader2.id()).getFirst();
        assertEquals("READY", next.status());
        assertTrue(next.notified());
        assertEquals(0, service.books("书名", "测试图书").getFirst().availableCopies());
    }

    @Test
    void expiredReservationPromotesNextReader() {
        service.reserve(reader.id(), singleCopyBook.id());
        service.reserve(reader2.id(), singleCopyBook.id());
        LibraryService lateService = new LibraryService(repository, at("2026-07-05T09:00:00"));

        lateService.expireReservations();

        assertEquals("EXPIRED", lateService.reservations(reader.id()).getFirst().status());
        Reservation next = lateService.reservations(reader2.id()).getFirst();
        assertEquals("READY", next.status());
        assertEquals(LocalDateTime.parse("2026-07-08T09:00:00"), next.expiresAt());
        assertEquals(0, lateService.books("书名", "测试图书").getFirst().availableCopies());
    }

    @Test
    void increasingBookCopiesPromotesWaitingReaders() {
        service.reserve(reader.id(), singleCopyBook.id());
        service.reserve(reader2.id(), singleCopyBook.id());

        Book current = service.books("书名", "测试图书").getFirst();
        service.updateBook(
                new Book(
                        current.id(),
                        current.isbn(),
                        current.title(),
                        current.author(),
                        current.publisher(),
                        current.category(),
                        2,
                        current.availableCopies(),
                        current.location()),
                current.category());

        assertEquals("READY", service.reservations(reader2.id()).getFirst().status());
        assertEquals(0, service.books("书名", "测试图书").getFirst().availableCopies());
    }

    @Test
    void reservationAndRenewalRulesAreEnforced() {
        long loanId = service.borrow(reader.id(), singleCopyBook.id());
        service.reserve(reader2.id(), singleCopyBook.id());
        assertThrows(
                IllegalArgumentException.class,
                () -> service.reserve(reader2.id(), singleCopyBook.id()));
        assertThrows(IllegalArgumentException.class, () -> service.renew(loanId));
    }

    @Test
    void renewalExtendsDueDateOnlyOnce() {
        long loanId = service.borrow(reader.id(), singleCopyBook.id());
        service.renew(loanId);
        Loan loan = service.loans(reader.id(), true).getFirst();
        assertEquals(LocalDateTime.parse("2026-07-29T09:00:00"), loan.dueAt());
        assertEquals(1, loan.renewCount());
        assertThrows(IllegalArgumentException.class, () -> service.renew(loanId));
    }

    @Test
    void unpaidFineBlocksBorrowingUntilPaid() {
        long loanId = service.borrow(reader.id(), singleCopyBook.id());
        LibraryService lateService = new LibraryService(repository, at("2026-07-20T09:00:00"));
        lateService.returnBook(loanId);
        Fine fine = lateService.fines(reader.id()).getFirst();
        assertThrows(
                IllegalArgumentException.class,
                () -> lateService.borrow(reader.id(), singleCopyBook.id()));
        lateService.payFine(fine.id(), reader.id());
        assertDoesNotThrow(() -> lateService.borrow(reader.id(), singleCopyBook.id()));
    }

    @Test
    void suspendedCardCannotBorrow() {
        service.updateCard(reader.id(), "SUSPENDED");
        assertThrows(
                IllegalArgumentException.class,
                () -> service.borrow(reader.id(), singleCopyBook.id()));
    }

    @Test
    void concurrentBorrowDoesNotOversellSingleCopy() throws Exception {
        int threads = 8;
        java.util.concurrent.ExecutorService pool =
                java.util.concurrent.Executors.newFixedThreadPool(threads);
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicInteger success =
                new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger rejected =
                new java.util.concurrent.atomic.AtomicInteger();
        List<User> contenders = new ArrayList<>();
        contenders.add(reader);
        contenders.add(reader2);
        for (int i = 3; i <= threads; i++)
            contenders.add(
                    service.register(
                            "race" + i,
                            "reader123",
                            "并发读者" + i,
                            "13800138" + String.format("%03d", i),
                            "race" + i + "@test.cn",
                            Role.READER));

        List<java.util.concurrent.Future<?>> futures = new ArrayList<>();
        for (User contender : contenders) {
            futures.add(
                    pool.submit(
                            () -> {
                                try {
                                    start.await();
                                    service.borrow(contender.id(), singleCopyBook.id());
                                    success.incrementAndGet();
                                } catch (IllegalArgumentException e) {
                                    rejected.incrementAndGet();
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            }));
        }
        start.countDown();
        for (java.util.concurrent.Future<?> future : futures) future.get();
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS));

        assertEquals(1, success.get(), "单册图书并发借阅只能成功一次");
        assertEquals(threads - 1, rejected.get());
        assertEquals(0, service.books("书名", "测试图书").getFirst().availableCopies());
        assertEquals(1, service.loans(null, true).size());
    }

    @Test
    void monthlyStatsAndCategoryStocksAreAvailable() {
        service.borrow(reader.id(), singleCopyBook.id());
        assertEquals(12, service.monthlyStats().size());
        assertEquals("2025-08", service.monthlyStats().getFirst().month());
        assertEquals("2026-07", service.monthlyStats().getLast().month());
        assertEquals(1, service.monthlyStats().getLast().borrowCount());
        assertFalse(service.categoryStocks().isEmpty());
        assertTrue(
                service.categoryStocks().stream()
                        .anyMatch(s -> "计算机".equals(s.category()) && s.totalCopies() >= 1));
    }

    @Test
    void userCanChangePassword() {
        service.changePassword(reader.id(), "reader123", "newPassword123", "newPassword123");

        assertThrows(
                IllegalArgumentException.class, () -> service.login("reader01", "reader123"));
        assertEquals(reader.id(), service.login("reader01", "newPassword123").id());
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        service.changePassword(
                                reader.id(), "wrong", "anotherPassword", "anotherPassword"));
    }

    @Test
    void readerDashboardOnlyCountsOwnBusinessData() {
        service.borrow(reader.id(), singleCopyBook.id());
        service.addBook("9780000000002", "第二本图书", "作者", "出版社", "计算机", 1, "T-02");
        Book second = service.books("书名", "第二本图书").getFirst();
        service.borrow(reader2.id(), second.id());

        Dashboard readerView = service.dashboard(reader.id());
        Dashboard reader2View = service.dashboard(reader2.id());
        Dashboard administratorView = service.dashboard();

        assertEquals(1, readerView.activeLoans());
        assertEquals(1, reader2View.activeLoans());
        assertEquals(2, administratorView.activeLoans());
        assertEquals(administratorView.totalCopies(), readerView.totalCopies());
        assertEquals(administratorView.availableCopies(), readerView.availableCopies());
    }

    @Test
    void categoryManagementProtectsCategoriesInUse() {
        service.addCategory("临时分类");
        assertTrue(service.categories().contains("临时分类"));

        service.renameCategory("临时分类", "新分类");
        assertFalse(service.categories().contains("临时分类"));
        assertTrue(service.categories().contains("新分类"));

        service.deleteCategory("新分类");
        assertFalse(service.categories().contains("新分类"));
        assertThrows(IllegalArgumentException.class, () -> service.deleteCategory("新分类"));
        assertThrows(IllegalArgumentException.class, () -> service.addCategory("计算机"));
        assertThrows(IllegalArgumentException.class, () -> service.deleteCategory("计算机"));
    }

    @Test
    void demoDataIsNotInjectedIntoExistingBusinessDatabase() {
        int users = service.users().size();
        int books = service.books("书名", "").size();

        service.seedDemoData();

        assertEquals(users, service.users().size());
        assertEquals(books, service.books("书名", "").size());
        assertTrue(service.users().stream().noneMatch(u -> "admin".equals(u.username())));
    }

    @Test
    void demoDataSeedingIsCompleteAndIdempotent() throws Exception {
        clearTestData();
        repository = new LibraryRepository(database);
        service = new LibraryService(repository, at("2026-07-20T09:00:00"));

        service.seedDemoData();
        Dashboard first = service.dashboard();
        int users = service.users().size();
        int loans = service.loans(null, false).size();
        int reservations = service.reservations(null).size();

        service.seedDemoData();
        Dashboard second = service.dashboard();

        assertEquals(first, second);
        assertEquals(users, service.users().size());
        assertEquals(loans, service.loans(null, false).size());
        assertEquals(reservations, service.reservations(null).size());
        assertTrue(first.bookKinds() >= 18);
        assertTrue(first.activeLoans() >= 6);
        assertTrue(first.waitingReservations() >= 2);
        assertFalse(service.monthlyStats().isEmpty());
    }

    private static Clock at(String localDateTime) {
        return Clock.fixed(
                LocalDateTime.parse(localDateTime).toInstant(ZoneOffset.ofHours(8)),
                ZoneId.of("Asia/Shanghai"));
    }
}
