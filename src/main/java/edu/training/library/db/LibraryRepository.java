package edu.training.library.db;

import edu.training.library.model.Models.*;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class LibraryRepository {
    private final Database database;
    public LibraryRepository(Database database) { this.database = database; }

    public Optional<AuthRow> findAuth(String username) {
        String sql = "SELECT id, username, password_hash, password_salt, full_name, phone, email, role, card_status FROM users WHERE username=?";
        try (Connection c = database.connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, username);
            try (ResultSet r = p.executeQuery()) { return r.next() ? Optional.of(auth(r)) : Optional.empty(); }
        } catch (SQLException e) { throw failure(e); }
    }

    public User createUser(String username, String hash, String salt, String fullName, String phone,
                           String email, Role role) {
        String sql = "INSERT INTO users(username,password_hash,password_salt,full_name,phone,email,role,card_status) VALUES(?,?,?,?,?,?,?,'ACTIVE')";
        try (Connection c = database.connect(); PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(p, username, hash, salt, fullName, phone, email, role.name());
            p.executeUpdate();
            try (ResultSet keys = p.getGeneratedKeys()) {
                keys.next(); return new User(keys.getLong(1), username, fullName, phone, email, role, "ACTIVE");
            }
        } catch (SQLException e) { throw failure(e); }
    }

    public List<User> users() {
        String sql = "SELECT id,username,full_name,phone,email,role,card_status FROM users ORDER BY role,full_name";
        try (Connection c = database.connect(); PreparedStatement p = c.prepareStatement(sql); ResultSet r = p.executeQuery()) {
            List<User> rows = new ArrayList<>();
            while (r.next()) rows.add(user(r));
            return rows;
        } catch (SQLException e) { throw failure(e); }
    }

    public void updateProfile(long id, String fullName, String phone, String email) {
        execute("UPDATE users SET full_name=?,phone=?,email=? WHERE id=?", fullName, phone, email, id);
    }

    public void updateCardStatus(long id, String status) {
        execute("UPDATE users SET card_status=? WHERE id=? AND role='READER'", status, id);
    }

    public List<String> categories() {
        try (Connection c = database.connect(); PreparedStatement p = c.prepareStatement("SELECT name FROM categories ORDER BY name"); ResultSet r = p.executeQuery()) {
            List<String> result = new ArrayList<>(); while (r.next()) result.add(r.getString(1)); return result;
        } catch (SQLException e) { throw failure(e); }
    }

    public void addCategory(String name) { execute("INSERT INTO categories(name) VALUES(?)", name); }

    public List<Book> books(String field, String keyword) {
        String column = switch (field) { case "作者" -> "b.author"; case "出版社" -> "b.publisher"; case "ISBN" -> "b.isbn"; default -> "b.title"; };
        String sql = "SELECT b.id,b.isbn,b.title,b.author,b.publisher,c.name,b.total_copies,b.available_copies,b.location " +
                "FROM books b LEFT JOIN categories c ON c.id=b.category_id WHERE LOWER(" + column + ") LIKE ? ORDER BY b.title";
        try (Connection c = database.connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, "%" + keyword.toLowerCase() + "%");
            try (ResultSet r = p.executeQuery()) { List<Book> rows = new ArrayList<>(); while (r.next()) rows.add(book(r)); return rows; }
        } catch (SQLException e) { throw failure(e); }
    }

    public long addBook(String isbn, String title, String author, String publisher, String category,
                        int copies, String location) {
        return database.transaction(c -> {
            long categoryId = categoryId(c, category);
            try (PreparedStatement p = c.prepareStatement("INSERT INTO books(isbn,title,author,publisher,category_id,total_copies,available_copies,location) VALUES(?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                bind(p, isbn, title, author, publisher, categoryId, copies, copies, location); p.executeUpdate();
                try (ResultSet keys = p.getGeneratedKeys()) { keys.next(); return keys.getLong(1); }
            }
        });
    }

    public void updateBook(Book book, String category) {
        database.transaction(c -> {
            int borrowed;
            try (PreparedStatement p = c.prepareStatement("SELECT total_copies-available_copies FROM books WHERE id=? FOR UPDATE")) {
                p.setLong(1, book.id()); try (ResultSet r=p.executeQuery()) { if(!r.next()) throw new IllegalArgumentException("图书不存在"); borrowed=r.getInt(1); }
            }
            if (book.totalCopies() < borrowed) throw new IllegalArgumentException("总馆藏不能小于当前借出数量 " + borrowed);
            long categoryId = categoryId(c, category);
            try (PreparedStatement p=c.prepareStatement("UPDATE books SET isbn=?,title=?,author=?,publisher=?,category_id=?,total_copies=?,available_copies=?,location=? WHERE id=?")) {
                bind(p,book.isbn(),book.title(),book.author(),book.publisher(),categoryId,book.totalCopies(),book.totalCopies()-borrowed,book.location(),book.id()); p.executeUpdate();
            }
            return null;
        });
    }

    public void deleteBook(long id) { execute("DELETE FROM books WHERE id=?", id); }

    public List<Loan> loans(Long userId, boolean activeOnly) {
        StringBuilder sql = new StringBuilder("SELECT l.id,l.user_id,u.full_name,l.book_id,b.title,l.borrowed_at,l.due_at,l.returned_at,l.renew_count,l.status FROM loans l JOIN users u ON u.id=l.user_id JOIN books b ON b.id=l.book_id WHERE 1=1");
        if (userId != null) sql.append(" AND l.user_id=?"); if (activeOnly) sql.append(" AND l.status='BORROWED'"); sql.append(" ORDER BY l.borrowed_at DESC");
        try (Connection c=database.connect(); PreparedStatement p=c.prepareStatement(sql.toString())) {
            if(userId!=null)p.setLong(1,userId); try(ResultSet r=p.executeQuery()){List<Loan> rows=new ArrayList<>();while(r.next())rows.add(loan(r));return rows;}
        } catch(SQLException e){throw failure(e);}
    }

    public List<Reservation> reservations(Long userId) {
        String sql="SELECT r.id,r.user_id,u.full_name,r.book_id,b.title,r.reserved_at,r.expires_at,r.status,r.notified FROM reservations r JOIN users u ON u.id=r.user_id JOIN books b ON b.id=r.book_id"+(userId==null?"":" WHERE r.user_id=?")+" ORDER BY r.reserved_at DESC";
        try(Connection c=database.connect();PreparedStatement p=c.prepareStatement(sql)){if(userId!=null)p.setLong(1,userId);try(ResultSet r=p.executeQuery()){List<Reservation> rows=new ArrayList<>();while(r.next())rows.add(reservation(r));return rows;}}catch(SQLException e){throw failure(e);}
    }

    public List<Fine> fines(Long userId) {
        String sql="SELECT f.id,f.loan_id,f.user_id,u.full_name,b.title,f.amount,f.reason,f.status,f.paid_at FROM fines f JOIN users u ON u.id=f.user_id JOIN loans l ON l.id=f.loan_id JOIN books b ON b.id=l.book_id"+(userId==null?"":" WHERE f.user_id=?")+" ORDER BY f.id DESC";
        try(Connection c=database.connect();PreparedStatement p=c.prepareStatement(sql)){if(userId!=null)p.setLong(1,userId);try(ResultSet r=p.executeQuery()){List<Fine> rows=new ArrayList<>();while(r.next())rows.add(fine(r));return rows;}}catch(SQLException e){throw failure(e);}
    }

    public Dashboard dashboard() {
        String sql="SELECT (SELECT COUNT(*) FROM books),(SELECT COALESCE(SUM(total_copies),0) FROM books),(SELECT COALESCE(SUM(available_copies),0) FROM books),(SELECT COUNT(*) FROM loans WHERE status='BORROWED'),(SELECT COUNT(*) FROM reservations WHERE status IN ('WAITING','READY')),(SELECT COALESCE(SUM(amount),0) FROM fines WHERE status='UNPAID')";
        try(Connection c=database.connect();PreparedStatement p=c.prepareStatement(sql);ResultSet r=p.executeQuery()){r.next();return new Dashboard(r.getLong(1),r.getLong(2),r.getLong(3),r.getLong(4),r.getLong(5),r.getBigDecimal(6));}catch(SQLException e){throw failure(e);}
    }

    public List<Ranking> rankings() {
        String sql="SELECT b.title,b.author,COUNT(l.id) borrow_count FROM books b LEFT JOIN loans l ON l.book_id=b.id GROUP BY b.id,b.title,b.author ORDER BY borrow_count DESC,b.title LIMIT 10";
        try(Connection c=database.connect();PreparedStatement p=c.prepareStatement(sql);ResultSet r=p.executeQuery()){List<Ranking> rows=new ArrayList<>();while(r.next())rows.add(new Ranking(r.getString(1),r.getString(2),r.getLong(3)));return rows;}catch(SQLException e){throw failure(e);}
    }

    public Database database() { return database; }

    private long categoryId(Connection c,String name)throws SQLException{try(PreparedStatement q=c.prepareStatement("SELECT id FROM categories WHERE name=?")){q.setString(1,name);try(ResultSet r=q.executeQuery()){if(r.next())return r.getLong(1);}}try(PreparedStatement p=c.prepareStatement("INSERT INTO categories(name) VALUES(?)",Statement.RETURN_GENERATED_KEYS)){p.setString(1,name);p.executeUpdate();try(ResultSet r=p.getGeneratedKeys()){r.next();return r.getLong(1);}}}
    private void execute(String sql,Object...args){try(Connection c=database.connect();PreparedStatement p=c.prepareStatement(sql)){bind(p,args);p.executeUpdate();}catch(SQLException e){throw failure(e);}}
    private static void bind(PreparedStatement p,Object...args)throws SQLException{for(int i=0;i<args.length;i++)p.setObject(i+1,args[i]);}
    private static User user(ResultSet r)throws SQLException{return new User(r.getLong("id"),r.getString("username"),r.getString("full_name"),r.getString("phone"),r.getString("email"),Role.valueOf(r.getString("role")),r.getString("card_status"));}
    private static AuthRow auth(ResultSet r)throws SQLException{return new AuthRow(user(r),r.getString("password_hash"),r.getString("password_salt"));}
    private static Book book(ResultSet r)throws SQLException{return new Book(r.getLong(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getString(6),r.getInt(7),r.getInt(8),r.getString(9));}
    private static Loan loan(ResultSet r)throws SQLException{return new Loan(r.getLong(1),r.getLong(2),r.getString(3),r.getLong(4),r.getString(5),r.getTimestamp(6).toLocalDateTime(),r.getTimestamp(7).toLocalDateTime(),time(r,8),r.getInt(9),r.getString(10));}
    private static Reservation reservation(ResultSet r)throws SQLException{return new Reservation(r.getLong(1),r.getLong(2),r.getString(3),r.getLong(4),r.getString(5),r.getTimestamp(6).toLocalDateTime(),time(r,7),r.getString(8),r.getBoolean(9));}
    private static Fine fine(ResultSet r)throws SQLException{return new Fine(r.getLong(1),r.getLong(2),r.getLong(3),r.getString(4),r.getString(5),r.getBigDecimal(6),r.getString(7),r.getString(8),time(r,9));}
    private static LocalDateTime time(ResultSet r,int i)throws SQLException{Timestamp t=r.getTimestamp(i);return t==null?null:t.toLocalDateTime();}
    private static IllegalStateException failure(SQLException e){String message=e.getMessage();if("23505".equals(e.getSQLState())||"23000".equals(e.getSQLState()))message="数据已存在或仍被业务记录引用";return new IllegalStateException(message,e);}
    public record AuthRow(User user,String hash,String salt){}
}
