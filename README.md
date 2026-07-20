# 图书馆借阅管理系统

基于 Java 21、Swing、JDBC 和 MySQL 开发的课程实训项目，分为管理员和读者两类账号。

## 功能

- 读者注册、登录、资料修改和借阅证管理
- 图书增删改、分类管理和馆藏查询
- 图书借阅、归还、续借和历史记录查询
- 图书预约、取消预约、到馆提醒和超时释放
- 逾期罚款计算及缴费状态管理
- 热门排行、月借阅统计、库存分析和 Excel 报表导出

主要业务规则：每位读者最多同时借 5 本书，借期为 14 天，每条记录最多续借一次；逾期费为每天 0.50 元。待取图书保留 3 天，到馆提醒在程序界面中显示。

## 环境

- JDK 21
- Maven 3.9+
- MySQL 8.x 或 9.x

## 运行

先创建数据库和账号：

```sql
CREATE DATABASE IF NOT EXISTS library
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'library'@'localhost' IDENTIFIED BY 'library_pass';
GRANT ALL PRIVILEGES ON library.* TO 'library'@'localhost';
FLUSH PRIVILEGES;
```

默认连接信息在 `src/main/resources/application.properties` 中，也可以通过环境变量覆盖：

```powershell
$env:LIBRARY_DB_URL='jdbc:mysql://localhost:3306/library?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false&createDatabaseIfNotExist=true'
$env:LIBRARY_DB_USER='library'
$env:LIBRARY_DB_PASSWORD='library_pass'
```

打包并启动：

```powershell
mvn clean package
java -jar target/library-system.jar
```

程序会自动创建表结构，并在空库中写入演示数据。

| 角色 | 用户名 | 密码 |
|---|---|---|
| 管理员 | `admin` | `admin123` |
| 读者 | `reader` | `reader123` |

## 测试

集成测试使用独立的 MySQL 测试库，库名必须包含 `library_test`：

```sql
CREATE DATABASE IF NOT EXISTS library_test
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON library_test.* TO 'library'@'localhost';
```

```powershell
$env:LIBRARY_TEST_DB_URL='jdbc:mysql://localhost:3306/library_test?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false&createDatabaseIfNotExist=true'
$env:LIBRARY_TEST_DB_USER='library'
$env:LIBRARY_TEST_DB_PASSWORD='library_pass'
mvn verify
```

项目共有 29 项测试，包括 20 项 MySQL 业务集成测试、1 项 Excel 导出测试和 8 项界面设计测试。未配置 `LIBRARY_TEST_DB_URL` 时，数据库测试会跳过。
