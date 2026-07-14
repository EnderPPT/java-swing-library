# 图书馆借阅管理系统

基于 Java 21、Swing 和 JDBC 的课程实训项目。系统包含读者管理、图书管理、馆藏查询、借阅归还、预约、罚款和统计分析七个模块，并区分管理员与读者权限。

## 直接运行

环境要求：JDK 21、Maven 3.9+。

```powershell
mvn clean package
java -jar target/library-system.jar
```

默认使用本地 H2 文件数据库，首次启动自动建表并写入演示数据。数据保存在 `data/` 目录。

| 角色 | 用户名 | 密码 |
|---|---|---|
| 管理员 | `admin` | `admin123` |
| 读者 | `reader` | `reader123` |

## 使用 MySQL

先配置当前终端并启动应用：

```powershell
$env:LIBRARY_DB_URL='jdbc:mysql://localhost:3306/library?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false'
$env:LIBRARY_DB_USER='library'
$env:LIBRARY_DB_PASSWORD='library_pass'
java -jar target/library-system.jar
```

程序会通过 `src/main/resources/db/schema.sql` 自动创建表结构。密码使用 PBKDF2-HMAC-SHA256 加盐后存储。

## 测试

```powershell
mvn test
```

## 业务规则

- 每位读者最多同时借阅 5 本，同一种图书不可重复借阅。
- 默认借期 14 天，最多续借 1 次；有预约或已经逾期时不能续借。
- 无库存时预约进入排队；归还后最早预约转为“待取书”，保留 3 天。
- 逾期费为每天 0.50 元；存在未缴罚款或借阅证停用时不能继续借阅。
