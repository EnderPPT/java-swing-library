package edu.training.library.db;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Objects;
import java.util.Properties;

public final class Database {
    private final String url;
    private final String user;
    private final String password;

    public Database(String url, String user, String password) {
        this.url = Objects.requireNonNull(url);
        this.user = Objects.requireNonNull(user);
        this.password = Objects.requireNonNull(password);
    }

    public static Database fromEnvironment() {
        Properties defaults = new Properties();
        try (var in = Database.class.getResourceAsStream("/application.properties")) {
            if (in != null) defaults.load(in);
        } catch (Exception e) {
            throw new IllegalStateException("无法读取数据库配置", e);
        }
        return new Database(value("LIBRARY_DB_URL", defaults), value("LIBRARY_DB_USER", defaults),
                value("LIBRARY_DB_PASSWORD", defaults));
    }

    private static String value(String name, Properties defaults) {
        return System.getenv().getOrDefault(name, defaults.getProperty(name, ""));
    }

    public Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public void initialize() {
        try (Connection connection = connect()) {
            String script;
            try (var input = Database.class.getResourceAsStream("/db/schema.sql")) {
                if (input == null) throw new IllegalStateException("缺少数据库初始化脚本");
                try (var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                    script = reader.lines().filter(line -> !line.stripLeading().startsWith("--"))
                            .reduce("", (a, b) -> a + "\n" + b);
                }
            }
            for (String sql : script.split(";")) {
                if (!sql.isBlank()) try (Statement statement = connection.createStatement()) {
                    statement.execute(sql);
                }
            }
            ensureIndex(connection,"books","idx_books_title","title");
            ensureIndex(connection,"books","idx_books_author","author");
            ensureIndex(connection,"loans","idx_loans_user_status","user_id,status");
            ensureIndex(connection,"reservations","idx_reservations_book_status","book_id,status");
        } catch (Exception e) {
            throw new IllegalStateException("数据库初始化失败：" + e.getMessage(), e);
        }
    }

    private static void ensureIndex(Connection connection,String table,String index,String columns)throws SQLException{
        boolean exists=false;
        try(ResultSet result=connection.getMetaData().getIndexInfo(connection.getCatalog(),null,table,false,false)){
            while(result.next())if(index.equalsIgnoreCase(result.getString("INDEX_NAME"))){exists=true;break;}
        }
        if(!exists)try(Statement statement=connection.createStatement()){
            statement.execute("CREATE INDEX "+index+" ON "+table+"("+columns+")");
        }catch(SQLException e){
            // Another process may have created the same index after the metadata check.
            String message=String.valueOf(e.getMessage()).toLowerCase();
            if(!message.contains("already exists")&&!message.contains("duplicate"))throw e;
        }
    }

    public <T> T transaction(SqlFunction<Connection, T> action) {
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try {
                T result = action.apply(connection);
                connection.commit();
                return result;
            } catch (Exception e) {
                connection.rollback();
                if (e instanceof RuntimeException runtime) throw runtime;
                throw new IllegalStateException(e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("数据库操作失败：" + e.getMessage(), e);
        }
    }

    @FunctionalInterface
    public interface SqlFunction<T, R> { R apply(T value) throws Exception; }
}
