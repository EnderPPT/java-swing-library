package edu.training.library.service;

import edu.training.library.db.LibraryRepository;
import edu.training.library.model.Models.*;
import edu.training.library.security.PasswordHasher;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class LibraryService {
    public static final int MAX_ACTIVE_LOANS = 5;
    public static final int LOAN_DAYS = 14;
    public static final BigDecimal DAILY_FINE = new BigDecimal("0.50");
    private final LibraryRepository repository;
    private final Clock clock;

    public LibraryService(LibraryRepository repository) {
        this(repository, Clock.systemDefaultZone());
    }

    public LibraryService(LibraryRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void seedDemoData() {
        if (!repository.users().isEmpty() || !repository.books("书名", "").isEmpty()) return;
        ensureAccount(
                "admin", "admin123", "系统管理员", "13800000000", "admin@library.local", Role.ADMIN);
        ensureAccount(
                "reader", "reader123", "演示读者", "13900000000", "reader@library.local", Role.READER);
        String[][] extraReaders = {
            {"reader01", "张同学", "13800138001", "reader01@library.local"},
            {"reader02", "李同学", "13800138002", "reader02@library.local"},
            {"reader03", "王同学", "13800138003", "reader03@library.local"},
            {"reader04", "赵同学", "13800138004", "reader04@library.local"},
            {"reader05", "陈同学", "13800138005", "reader05@library.local"},
            {"reader06", "刘同学", "13800138006", "reader06@library.local"}
        };
        for (String[] r : extraReaders)
            ensureAccount(r[0], "reader123", r[1], r[2], r[3], Role.READER);

        Object[][] catalog = {
            {"9787111213826", "Java编程思想", "Bruce Eckel", "机械工业出版社", "计算机", 5, "A-01"},
            {"9787115428028", "深入理解计算机系统", "Randal E. Bryant", "人民邮电出版社", "计算机", 3, "A-02"},
            {"9787111544937", "算法导论", "Thomas H. Cormen", "机械工业出版社", "计算机", 4, "A-03"},
            {"9787115547484", "设计模式", "Erich Gamma", "机械工业出版社", "计算机", 3, "A-04"},
            {"9787115352118", "重构", "Martin Fowler", "人民邮电出版社", "计算机", 2, "A-05"},
            {"9787020002207", "红楼梦", "曹雪芹", "人民文学出版社", "文学", 4, "B-01"},
            {"9787544253994", "百年孤独", "加西亚·马尔克斯", "南海出版公司", "文学", 3, "B-02"},
            {"9787020008735", "围城", "钱钟书", "人民文学出版社", "文学", 3, "B-03"},
            {"9787020024759", "平凡的世界", "路遥", "人民文学出版社", "文学", 4, "B-04"},
            {"9787101003048", "史记", "司马迁", "中华书局", "历史", 2, "C-01"},
            {"9787101003055", "资治通鉴", "司马光", "中华书局", "历史", 2, "C-02"},
            {"9787101003062", "中国通史", "吕思勉", "中华书局", "历史", 3, "C-03"},
            {"9787108011381", "人类简史", "尤瓦尔·赫拉利", "中信出版社", "社科", 3, "D-01"},
            {"9787508647357", "未来简史", "尤瓦尔·赫拉利", "中信出版社", "社科", 2, "D-02"},
            {"9787508684031", "原则", "瑞·达利欧", "中信出版社", "社科", 2, "D-03"},
            {"9787536692930", "三体", "刘慈欣", "重庆出版社", "科幻", 5, "E-01"},
            {"9787536692947", "球状闪电", "刘慈欣", "重庆出版社", "科幻", 3, "E-02"},
            {"9787532731152", "银河帝国", "阿西莫夫", "上海译文出版社", "科幻", 2, "E-03"}
        };
        boolean initialSetup = repository.books("书名", "").isEmpty();
        if (initialSetup)
            for (Object[] row : catalog) {
                String isbn = (String) row[0];
                if (repository.books("ISBN", isbn).isEmpty())
                    repository.addBook(
                            isbn,
                            (String) row[1],
                            (String) row[2],
                            (String) row[3],
                            (String) row[4],
                            (Integer) row[5],
                            (String) row[6]);
            }

        if (initialSetup) seedCirculationHistory();
    }

    private void ensureAccount(
            String username, String password, String name, String phone, String email, Role role) {
        if (repository.findAuth(username).isEmpty())
            register(username, password, name, phone, email, role);
    }

    private void seedCirculationHistory() {
        List<User> readers =
                repository.users().stream().filter(u -> u.role() == Role.READER).toList();
        List<Book> books = repository.books("书名", "");
        if (readers.size() < 6 || books.size() < 10) return;

        User r0 = readerByUsername(readers, "reader");
        User r1 = readerByUsername(readers, "reader01");
        User r2 = readerByUsername(readers, "reader02");
        User r3 = readerByUsername(readers, "reader03");
        User r4 = readerByUsername(readers, "reader04");
        User r5 = readerByUsername(readers, "reader05");
        User r6 = readerByUsername(readers, "reader06");

        Book java = bookByIsbn(books, "9787111213826");
        Book csapp = bookByIsbn(books, "9787115428028");
        Book clrs = bookByIsbn(books, "9787111544937");
        Book dream = bookByIsbn(books, "9787020002207");
        Book solitude = bookByIsbn(books, "9787544253994");
        Book history = bookByIsbn(books, "9787101003048");
        Book tongjian = bookByIsbn(books, "9787101003055");
        Book santi = bookByIsbn(books, "9787536692930");

        LibraryService early = withClock("2026-01-08T09:00:00");
        LibraryService mid = withClock("2026-02-12T10:00:00");
        LibraryService spring = withClock("2026-03-05T11:00:00");
        LibraryService april = withClock("2026-04-10T09:30:00");
        LibraryService may = withClock("2026-05-15T14:00:00");
        LibraryService june = withClock("2026-06-08T09:00:00");
        LibraryService juneReturn = withClock("2026-06-18T16:00:00");
        LibraryService july = withClock("2026-07-05T10:00:00");
        LibraryService currentService = new LibraryService(repository, clock);

        early.returnBook(early.borrow(r0.id(), java.id()));
        mid.returnBook(mid.borrow(r1.id(), java.id()));
        spring.returnBook(spring.borrow(r2.id(), csapp.id()));
        april.returnBook(april.borrow(r0.id(), clrs.id()));
        may.returnBook(may.borrow(r3.id(), dream.id()));
        juneReturn.returnBook(june.borrow(r4.id(), solitude.id()));
        juneReturn.returnBook(june.borrow(r1.id(), santi.id()));

        long overdue = may.borrow(r2.id(), history.id());
        july.returnBook(overdue);

        long active1 = july.borrow(r0.id(), java.id());
        july.renew(active1);
        july.borrow(r1.id(), clrs.id());
        july.borrow(r3.id(), dream.id());
        currentService.borrow(r4.id(), santi.id());

        Book scarce =
                repository.books("ISBN", tongjian.isbn()).stream().findFirst().orElse(tongjian);
        User[] fillers = {r0, r1};
        for (int i = 0; i < scarce.availableCopies() && i < fillers.length; i++)
            currentService.borrow(fillers[i].id(), scarce.id());
        currentService.reserve(r5.id(), scarce.id());
        currentService.reserve(r4.id(), scarce.id());
        currentService.reserve(r6.id(), bookByIsbn(books, "9787536692947").id());
    }

    private static User readerByUsername(List<User> readers, String username) {
        return readers.stream()
                .filter(u -> username.equals(u.username()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("演示读者缺失：" + username));
    }

    private static Book bookByIsbn(List<Book> books, String isbn) {
        return books.stream()
                .filter(b -> isbn.equals(b.isbn()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("演示图书缺失：" + isbn));
    }

    private LibraryService withClock(String localDateTime) {
        return new LibraryService(
                repository,
                Clock.fixed(
                        LocalDateTime.parse(localDateTime).toInstant(ZoneOffset.ofHours(8)),
                        ZoneId.of("Asia/Shanghai")));
    }

    public User login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank())
            throw new IllegalArgumentException("请输入用户名和密码");
        var row =
                repository
                        .findAuth(username.trim())
                        .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));
        if (!PasswordHasher.verify(password, row.hash(), row.salt()))
            throw new IllegalArgumentException("用户名或密码错误");
        return row.user();
    }

    public void changePassword(
            long userId, String currentPassword, String newPassword, String confirmation) {
        if (currentPassword == null || currentPassword.isBlank())
            throw new IllegalArgumentException("请输入当前密码");
        if (newPassword == null || newPassword.length() < 6)
            throw new IllegalArgumentException("新密码不能少于6位");
        if (!newPassword.equals(confirmation)) throw new IllegalArgumentException("两次输入的新密码不一致");
        if (newPassword.equals(currentPassword)) throw new IllegalArgumentException("新密码不能与当前密码相同");
        var row =
                repository
                        .findAuth(userId)
                        .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        if (!PasswordHasher.verify(currentPassword, row.hash(), row.salt()))
            throw new IllegalArgumentException("当前密码错误");
        var credentials = PasswordHasher.hash(newPassword);
        repository.updatePassword(userId, credentials.hash(), credentials.salt());
    }

    public User register(
            String username, String password, String name, String phone, String email, Role role) {
        username = required(username, "用户名");
        name = required(name, "姓名");
        if (!username.matches("[A-Za-z0-9_]{3,20}"))
            throw new IllegalArgumentException("用户名须为3至20位字母、数字或下划线");
        if (password == null || password.length() < 6)
            throw new IllegalArgumentException("密码不能少于6位");
        if (email != null && !email.isBlank() && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            throw new IllegalArgumentException("邮箱格式不正确");
        var credentials = PasswordHasher.hash(password);
        return repository.createUser(
                username,
                credentials.hash(),
                credentials.salt(),
                name,
                blank(phone),
                blank(email),
                role);
    }

    public void addBook(
            String isbn,
            String title,
            String author,
            String publisher,
            String category,
            int copies,
            String location) {
        if (copies < 1) throw new IllegalArgumentException("馆藏数量至少为1");
        repository.addBook(
                required(isbn, "ISBN"),
                required(title, "书名"),
                required(author, "作者"),
                required(publisher, "出版社"),
                required(category, "分类"),
                copies,
                required(location, "馆藏位置"));
    }

    public void updateBook(Book book, String category) {
        if (book.totalCopies() < 1) throw new IllegalArgumentException("馆藏数量至少为1");
        repository.updateBook(book, required(category, "分类"));
        reconcileReservationInventory();
    }

    public long borrow(long readerId, long bookId) {
        return repository
                .database()
                .transaction(
                        c -> {
                            ensureReader(c, readerId);
                            expireReadyReservations(c);
                            if (count(
                                            c,
                                            "SELECT COUNT(*) FROM loans WHERE user_id=? AND status='BORROWED'",
                                            readerId)
                                    >= MAX_ACTIVE_LOANS)
                                throw new IllegalArgumentException("每位读者最多同时借阅5本图书");
                            if (count(
                                            c,
                                            "SELECT COUNT(*) FROM loans WHERE user_id=? AND book_id=? AND status='BORROWED'",
                                            readerId,
                                            bookId)
                                    > 0) throw new IllegalArgumentException("同一种图书不能重复借阅");
                            if (count(
                                            c,
                                            "SELECT COUNT(*) FROM fines WHERE user_id=? AND status='UNPAID'",
                                            readerId)
                                    > 0) throw new IllegalArgumentException("存在未缴罚款，暂不能借阅");
                            int available =
                                    integer(
                                            c,
                                            "SELECT available_copies FROM books WHERE id=? FOR UPDATE",
                                            bookId);
                            Long readyReservationId = readyReservationId(c, readerId, bookId);
                            if (readyReservationId == null && available <= 0)
                                throw new IllegalArgumentException("该书当前无可借库存，请先预约");
                            LocalDateTime now = LocalDateTime.now(clock),
                                    due = now.plusDays(LOAN_DAYS);
                            long id =
                                    insert(
                                            c,
                                            "INSERT INTO loans(user_id,book_id,borrowed_at,due_at,status) VALUES(?,?,?,?,'BORROWED')",
                                            readerId,
                                            bookId,
                                            now,
                                            due);
                            if (readyReservationId == null) {
                                update(
                                        c,
                                        "UPDATE books SET available_copies=available_copies-1 WHERE id=?",
                                        bookId);
                            } else {
                                update(
                                        c,
                                        "UPDATE reservations SET status='FULFILLED' WHERE id=?",
                                        readyReservationId);
                            }
                            return id;
                        });
    }

    public void returnBook(long loanId) {
        repository
                .database()
                .transaction(
                        c -> {
                            LoanRow loan = loanForUpdate(c, loanId);
                            if (!"BORROWED".equals(loan.status))
                                throw new IllegalArgumentException("该借阅记录已归还");
                            LocalDateTime now = LocalDateTime.now(clock);
                            update(
                                    c,
                                    "UPDATE loans SET returned_at=?,status='RETURNED' WHERE id=?",
                                    now,
                                    loanId);
                            update(
                                    c,
                                    "UPDATE books SET available_copies=available_copies+1 WHERE id=?",
                                    loan.bookId);
                            long overdue =
                                    Math.max(
                                            0,
                                            ChronoUnit.DAYS.between(
                                                    loan.dueAt.toLocalDate(), now.toLocalDate()));
                            if (overdue > 0) {
                                BigDecimal amount =
                                        DAILY_FINE
                                                .multiply(BigDecimal.valueOf(overdue))
                                                .setScale(2, RoundingMode.HALF_UP);
                                update(
                                        c,
                                        "INSERT INTO fines(loan_id,user_id,amount,reason,status) VALUES(?,?,?,?, 'UNPAID')",
                                        loanId,
                                        loan.userId,
                                        amount,
                                        "逾期" + overdue + "天");
                            }
                            promoteWaitingReservations(c, loan.bookId, now);
                            return null;
                        });
    }

    public void renew(long loanId) {
        repository
                .database()
                .transaction(
                        c -> {
                            LoanRow loan = loanForUpdate(c, loanId);
                            if (!"BORROWED".equals(loan.status))
                                throw new IllegalArgumentException("只能续借尚未归还的图书");
                            if (loan.renewCount >= 1)
                                throw new IllegalArgumentException("每次借阅最多续借一次");
                            LocalDateTime now = LocalDateTime.now(clock);
                            if (loan.dueAt.isBefore(now))
                                throw new IllegalArgumentException("图书已逾期，不能续借");
                            if (count(
                                            c,
                                            "SELECT COUNT(*) FROM reservations WHERE book_id=? AND status IN ('WAITING','READY')",
                                            loan.bookId)
                                    > 0) throw new IllegalArgumentException("已有其他读者预约，不能续借");
                            update(
                                    c,
                                    "UPDATE loans SET due_at=?,renew_count=renew_count+1 WHERE id=?",
                                    loan.dueAt.plusDays(LOAN_DAYS),
                                    loanId);
                            return null;
                        });
    }

    public long reserve(long readerId, long bookId) {
        return repository
                .database()
                .transaction(
                        c -> {
                            ensureReader(c, readerId);
                            expireReadyReservations(c);
                            if (count(
                                            c,
                                            "SELECT COUNT(*) FROM loans WHERE user_id=? AND book_id=? AND status='BORROWED'",
                                            readerId,
                                            bookId)
                                    > 0) throw new IllegalArgumentException("您已借阅该书");
                            if (count(
                                            c,
                                            "SELECT COUNT(*) FROM reservations WHERE user_id=? AND book_id=? AND status IN ('WAITING','READY')",
                                            readerId,
                                            bookId)
                                    > 0) throw new IllegalArgumentException("请勿重复预约");
                            int available =
                                    integer(
                                            c,
                                            "SELECT available_copies FROM books WHERE id=? FOR UPDATE",
                                            bookId);
                            String status = available > 0 ? "READY" : "WAITING";
                            LocalDateTime expires =
                                    "READY".equals(status)
                                            ? LocalDateTime.now(clock).plusDays(3)
                                            : null;
                            long reservationId =
                                    insert(
                                            c,
                                            "INSERT INTO reservations(user_id,book_id,reserved_at,expires_at,status,notified) VALUES(?,?,?,?,?,?)",
                                            readerId,
                                            bookId,
                                            LocalDateTime.now(clock),
                                            expires,
                                            status,
                                            "READY".equals(status));
                            if ("READY".equals(status)) {
                                update(
                                        c,
                                        "UPDATE books SET available_copies=available_copies-1 WHERE id=?",
                                        bookId);
                            }
                            return reservationId;
                        });
    }

    public void cancelReservation(long reservationId, long readerId) {
        repository
                .database()
                .transaction(
                        c -> {
                            ReservationState reservation =
                                    reservationState(c, reservationId, readerId, false);
                            lockBook(c, reservation.bookId());
                            reservation = reservationState(c, reservationId, readerId, true);
                            if (!"WAITING".equals(reservation.status())
                                    && !"READY".equals(reservation.status())) {
                                throw new IllegalArgumentException("该预约不能取消");
                            }
                            update(
                                    c,
                                    "UPDATE reservations SET status='CANCELLED' WHERE id=?",
                                    reservationId);
                            if ("READY".equals(reservation.status())) {
                                update(
                                        c,
                                        "UPDATE books SET available_copies=available_copies+1 WHERE id=?",
                                        reservation.bookId());
                                promoteWaitingReservations(
                                        c, reservation.bookId(), LocalDateTime.now(clock));
                            }
                            return null;
                        });
    }

    public void expireReservations() {
        repository
                .database()
                .transaction(
                        c -> {
                            expireReadyReservations(c);
                            return null;
                        });
    }

    public void reconcileReservationInventory() {
        repository
                .database()
                .transaction(
                        c -> {
                            LocalDateTime now = LocalDateTime.now(clock);
                            for (long bookId : bookIds(c)) {
                                int totalCopies = totalCopiesForUpdate(c, bookId);
                                int activeLoans =
                                        integer(
                                                c,
                                                "SELECT COUNT(*) FROM loans WHERE book_id=? AND status='BORROWED'",
                                                bookId);
                                int reservableCopies = Math.max(0, totalCopies - activeLoans);
                                List<Long> readyIds = readyReservationIdsForUpdate(c, bookId);
                                int heldCopies = Math.min(reservableCopies, readyIds.size());
                                for (int i = heldCopies; i < readyIds.size(); i++) {
                                    update(
                                            c,
                                            "UPDATE reservations SET status='WAITING',expires_at=NULL,notified=FALSE WHERE id=?",
                                            readyIds.get(i));
                                }
                                update(
                                        c,
                                        "UPDATE books SET available_copies=? WHERE id=?",
                                        reservableCopies - heldCopies,
                                        bookId);
                                promoteWaitingReservations(c, bookId, now);
                            }
                            return null;
                        });
    }

    public void payFine(long fineId, long readerId) {
        repository
                .database()
                .transaction(
                        c -> {
                            try (PreparedStatement p =
                                    c.prepareStatement(
                                            "UPDATE fines SET status='PAID',paid_at=? WHERE id=? AND user_id=? AND status='UNPAID'")) {
                                p.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now(clock)));
                                p.setLong(2, fineId);
                                p.setLong(3, readerId);
                                if (p.executeUpdate() == 0)
                                    throw new IllegalArgumentException("该罚款无需缴纳");
                            }
                            return null;
                        });
    }

    public List<Book> books(String field, String keyword) {
        return repository.books(field, blank(keyword));
    }

    public List<User> users() {
        return repository.users();
    }

    public List<Loan> loans(Long userId, boolean active) {
        return repository.loans(userId, active);
    }

    public List<Reservation> reservations(Long userId) {
        expireReservations();
        return repository.reservations(userId);
    }

    public List<Fine> fines(Long userId) {
        return repository.fines(userId);
    }

    public Dashboard dashboard() {
        expireReservations();
        return repository.dashboard(null);
    }

    public Dashboard dashboard(Long userId) {
        expireReservations();
        return repository.dashboard(userId);
    }

    public List<Ranking> rankings() {
        return repository.rankings();
    }

    public List<MonthlyStat> monthlyStats() {
        YearMonth current = YearMonth.from(LocalDateTime.now(clock));
        YearMonth first = current.minusMonths(11);
        Map<String, Long> counts =
                repository.monthlyLoanCounts(first.atDay(1).atStartOfDay()).stream()
                        .collect(
                                java.util.stream.Collectors.toMap(
                                        MonthlyStat::month, MonthlyStat::borrowCount));
        List<MonthlyStat> rows = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            String month = first.plusMonths(i).toString();
            rows.add(new MonthlyStat(month, counts.getOrDefault(month, 0L)));
        }
        return rows;
    }

    public List<CategoryStock> categoryStocks() {
        return repository.categoryStocks();
    }

    public List<String> categories() {
        return repository.categories();
    }

    public void addCategory(String name) {
        String category = required(name, "分类名称");
        if (repository.categories().contains(category)) throw new IllegalArgumentException("分类名称已存在");
        repository.addCategory(category);
    }

    public void renameCategory(String currentName, String newName) {
        String current = required(currentName, "原分类名称");
        String target = required(newName, "新分类名称");
        List<String> categories = repository.categories();
        if (!categories.contains(current)) throw new IllegalArgumentException("原分类不存在");
        if (current.equals(target)) throw new IllegalArgumentException("分类名称未发生变化");
        if (categories.contains(target)) throw new IllegalArgumentException("分类名称已存在");
        repository.renameCategory(current, target);
    }

    public void deleteCategory(String name) {
        String category = required(name, "分类名称");
        if (!repository.categories().contains(category)) throw new IllegalArgumentException("分类不存在");
        if (repository.categoryInUse(category)) throw new IllegalArgumentException("该分类仍有图书，不能删除");
        repository.deleteCategory(category);
    }

    public void deleteBook(long id) {
        repository.deleteBook(id);
    }

    public void updateCard(long id, String status) {
        repository.updateCardStatus(id, status);
    }

    public void updateProfile(long id, String name, String phone, String email) {
        repository.updateProfile(id, required(name, "姓名"), blank(phone), blank(email));
    }

    private void ensureReader(Connection c, long id) throws SQLException {
        try (PreparedStatement p =
                c.prepareStatement("SELECT role,card_status FROM users WHERE id=? FOR UPDATE")) {
            p.setLong(1, id);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next() || !"READER".equals(r.getString(1)))
                    throw new IllegalArgumentException("读者不存在");
                if (!"ACTIVE".equals(r.getString(2))) throw new IllegalArgumentException("借阅证已停用");
            }
        }
    }

    private void expireReadyReservations(Connection c) throws SQLException {
        LocalDateTime now = LocalDateTime.now(clock);
        Set<Long> bookIds = new LinkedHashSet<>();
        try (PreparedStatement statement =
                c.prepareStatement(
                        "SELECT DISTINCT book_id FROM reservations WHERE status='READY' AND expires_at<?")) {
            statement.setTimestamp(1, Timestamp.valueOf(now));
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) bookIds.add(result.getLong(1));
            }
        }
        for (long bookId : bookIds) {
            lockBook(c, bookId);
            int expired =
                    updateCount(
                            c,
                            "UPDATE reservations SET status='EXPIRED' WHERE book_id=? AND status='READY' AND expires_at<?",
                            bookId,
                            now);
            if (expired == 0) continue;
            update(
                    c,
                    "UPDATE books SET available_copies=available_copies+? WHERE id=?",
                    expired,
                    bookId);
            promoteWaitingReservations(c, bookId, now);
        }
    }

    private void promoteWaitingReservations(Connection c, long bookId, LocalDateTime now)
            throws SQLException {
        int available = lockBook(c, bookId);
        while (available > 0) {
            Long reservationId = firstWaitingReservationId(c, bookId);
            if (reservationId == null) return;
            update(
                    c,
                    "UPDATE reservations SET status='READY',expires_at=?,notified=TRUE WHERE id=?",
                    now.plusDays(3),
                    reservationId);
            update(c, "UPDATE books SET available_copies=available_copies-1 WHERE id=?", bookId);
            available--;
        }
    }

    private static Long readyReservationId(Connection c, long readerId, long bookId)
            throws SQLException {
        try (PreparedStatement statement =
                c.prepareStatement(
                        "SELECT id FROM reservations WHERE user_id=? AND book_id=? AND status='READY' FOR UPDATE")) {
            statement.setLong(1, readerId);
            statement.setLong(2, bookId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getLong(1) : null;
            }
        }
    }

    private static Long firstWaitingReservationId(Connection c, long bookId) throws SQLException {
        try (PreparedStatement statement =
                c.prepareStatement(
                        "SELECT id FROM reservations WHERE book_id=? AND status='WAITING' ORDER BY reserved_at,id LIMIT 1 FOR UPDATE")) {
            statement.setLong(1, bookId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getLong(1) : null;
            }
        }
    }

    private static int lockBook(Connection c, long bookId) throws SQLException {
        return integer(c, "SELECT available_copies FROM books WHERE id=? FOR UPDATE", bookId);
    }

    private static int totalCopiesForUpdate(Connection c, long bookId) throws SQLException {
        return integer(c, "SELECT total_copies FROM books WHERE id=? FOR UPDATE", bookId);
    }

    private static List<Long> bookIds(Connection c) throws SQLException {
        List<Long> ids = new ArrayList<>();
        try (PreparedStatement statement = c.prepareStatement("SELECT id FROM books ORDER BY id");
                ResultSet result = statement.executeQuery()) {
            while (result.next()) ids.add(result.getLong(1));
        }
        return ids;
    }

    private static List<Long> readyReservationIdsForUpdate(Connection c, long bookId)
            throws SQLException {
        List<Long> ids = new ArrayList<>();
        try (PreparedStatement statement =
                c.prepareStatement(
                        "SELECT id FROM reservations WHERE book_id=? AND status='READY' ORDER BY reserved_at,id FOR UPDATE")) {
            statement.setLong(1, bookId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) ids.add(result.getLong(1));
            }
        }
        return ids;
    }

    private static ReservationState reservationState(
            Connection c, long reservationId, long readerId, boolean forUpdate)
            throws SQLException {
        String sql =
                "SELECT book_id,status FROM reservations WHERE id=? AND user_id=?"
                        + (forUpdate ? " FOR UPDATE" : "");
        try (PreparedStatement statement = c.prepareStatement(sql)) {
            statement.setLong(1, reservationId);
            statement.setLong(2, readerId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new IllegalArgumentException("该预约不能取消");
                return new ReservationState(result.getLong(1), result.getString(2));
            }
        }
    }

    private LoanRow loanForUpdate(Connection c, long id) throws SQLException {
        try (PreparedStatement p =
                c.prepareStatement(
                        "SELECT user_id,book_id,due_at,renew_count,status FROM loans WHERE id=? FOR UPDATE")) {
            p.setLong(1, id);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) throw new IllegalArgumentException("借阅记录不存在");
                return new LoanRow(
                        r.getLong(1),
                        r.getLong(2),
                        r.getTimestamp(3).toLocalDateTime(),
                        r.getInt(4),
                        r.getString(5));
            }
        }
    }

    private static long count(Connection c, String sql, Object... args) throws SQLException {
        return integer(c, sql, args);
    }

    private static int integer(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(sql)) {
            bind(p, args);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) throw new IllegalArgumentException("记录不存在");
                return r.getInt(1);
            }
        }
    }

    private static long insert(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(p, args);
            p.executeUpdate();
            try (ResultSet r = p.getGeneratedKeys()) {
                r.next();
                return r.getLong(1);
            }
        }
    }

    private static void update(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(sql)) {
            bind(p, args);
            p.executeUpdate();
        }
    }

    private static int updateCount(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(sql)) {
            bind(p, args);
            return p.executeUpdate();
        }
    }

    private static void bind(PreparedStatement p, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i++) {
            Object v = args[i];
            if (v instanceof LocalDateTime t) p.setTimestamp(i + 1, Timestamp.valueOf(t));
            else p.setObject(i + 1, v);
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + "不能为空");
        return value.trim();
    }

    private static String blank(String value) {
        return value == null ? "" : value.trim();
    }

    private record LoanRow(
            long userId, long bookId, LocalDateTime dueAt, int renewCount, String status) {}

    private record ReservationState(long bookId, String status) {}
}
