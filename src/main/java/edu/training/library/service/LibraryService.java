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
        if (repository.findAuth("admin").isEmpty())
            register(
                    "admin", "admin123", "系统管理员", "13800000000", "admin@library.local", Role.ADMIN);
        if (repository.findAuth("reader").isEmpty())
            register(
                    "reader",
                    "reader123",
                    "演示读者",
                    "13900000000",
                    "reader@library.local",
                    Role.READER);
        if (repository.books("书名", "").isEmpty()) {
            repository.addBook(
                    "9787111213826", "Java编程思想", "Bruce Eckel", "机械工业出版社", "计算机", 5, "A-01");
            repository.addBook(
                    "9787115428028", "深入理解计算机系统", "Randal E. Bryant", "人民邮电出版社", "计算机", 3, "A-02");
            repository.addBook("9787020002207", "红楼梦", "曹雪芹", "人民文学出版社", "文学", 4, "B-01");
            repository.addBook("9787101003048", "史记", "司马迁", "中华书局", "历史", 2, "C-01");
            repository.addBook("9787544253994", "百年孤独", "加西亚·马尔克斯", "南海出版公司", "文学", 3, "B-02");
        }
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
        return repository.dashboard();
    }

    public List<Ranking> rankings() {
        return repository.rankings();
    }

    public List<String> categories() {
        return repository.categories();
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
