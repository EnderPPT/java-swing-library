package edu.training.library.ui;

import edu.training.library.model.Models.*;
import edu.training.library.service.LibraryService;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

public final class LibraryFrame extends JFrame {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final LibraryService service;
    private User current;
    private final JTabbedPane tabs = new JTabbedPane(JTabbedPane.LEFT);
    private final List<Runnable> refreshers = new ArrayList<>();
    private List<Book> bookRows = List.of();
    private JTable bookTable;
    private DefaultTableModel bookModel;
    private JComboBox<String> searchField;
    private JTextField keyword;
    private List<Loan> loanRows = List.of();
    private JTable loanTable;
    private DefaultTableModel loanModel;
    private List<Reservation> reservationRows = List.of();
    private JTable reservationTable;
    private DefaultTableModel reservationModel;
    private List<Fine> fineRows = List.of();
    private JTable fineTable;
    private DefaultTableModel fineModel;
    private List<User> userRows = List.of();
    private JTable userTable;
    private DefaultTableModel userModel;
    private JPanel dashboardMetrics;
    private DefaultTableModel rankingModel;

    public LibraryFrame(LibraryService service, User user) {
        super("馆藏通 - " + user.fullName());
        this.service = service;
        this.current = user;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1220, 760);
        setMinimumSize(new Dimension(980, 650));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        add(header(), BorderLayout.NORTH);
        tabs.setBorder(new EmptyBorder(10, 10, 10, 10));
        tabs.addTab("总览", dashboardPanel());
        tabs.addTab("馆藏检索", booksPanel());
        tabs.addTab(user.role() == Role.ADMIN ? "借还工作台" : "我的借阅", loansPanel());
        tabs.addTab("预约管理", reservationsPanel());
        tabs.addTab("罚款记录", finesPanel());
        if (user.role() == Role.ADMIN) tabs.addTab("读者管理", usersPanel());
        tabs.addTab("统计分析", statisticsPanel());
        add(tabs, BorderLayout.CENTER);
        refreshAll();
    }

    private JPanel header() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(12, 20, 12, 20));
        JLabel brand = new JLabel("馆藏通  |  图书馆借阅管理系统");
        brand.setFont(brand.getFont().deriveFont(Font.BOLD, 18f));
        brand.setForeground(Ui.INK);
        p.add(brand, BorderLayout.WEST);
        JPanel right =
                Ui.toolbar(
                        new JLabel(
                                current.fullName()
                                        + " · "
                                        + (current.role() == Role.ADMIN ? "管理员" : "读者")));
        JButton profile = new JButton("个人资料");
        profile.addActionListener(e -> editProfile());
        JButton logout = new JButton("退出");
        logout.addActionListener(
                e -> {
                    dispose();
                    new LoginFrame(service).setVisible(true);
                });
        right.add(profile);
        right.add(logout);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    private JPanel dashboardPanel() {
        JPanel p = Ui.panel(new BorderLayout(0, 16));
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(Ui.title("运行总览"), BorderLayout.WEST);
        JButton refresh = new JButton("刷新");
        refresh.addActionListener(e -> refreshAll());
        top.add(refresh, BorderLayout.EAST);
        p.add(top, BorderLayout.NORTH);
        dashboardMetrics = new JPanel(new GridLayout(2, 3, 12, 12));
        dashboardMetrics.setOpaque(false);
        p.add(dashboardMetrics, BorderLayout.CENTER);
        refreshers.add(this::refreshDashboard);
        return p;
    }

    private void refreshDashboard() {
        Dashboard d = service.dashboard();
        dashboardMetrics.removeAll();
        dashboardMetrics.add(metric("图书品种", d.bookKinds() + " 种"));
        dashboardMetrics.add(metric("馆藏总量", d.totalCopies() + " 册"));
        dashboardMetrics.add(metric("当前可借", d.availableCopies() + " 册"));
        dashboardMetrics.add(metric("在借记录", d.activeLoans() + " 条"));
        dashboardMetrics.add(metric("有效预约", d.waitingReservations() + " 条"));
        dashboardMetrics.add(metric("待缴罚款", "¥ " + d.unpaidFines()));
        dashboardMetrics.revalidate();
        dashboardMetrics.repaint();
    }

    private JPanel metric(String label, String value) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(226, 232, 240)),
                        new EmptyBorder(16, 18, 16, 18)));
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.anchor = GridBagConstraints.WEST;
        JLabel l = Ui.muted(label);
        p.add(l, g);
        g.gridy = 1;
        g.insets = new Insets(8, 0, 0, 0);
        JLabel v = new JLabel(value);
        v.setFont(v.getFont().deriveFont(Font.BOLD, 28f));
        v.setForeground(Ui.INK);
        p.add(v, g);
        return p;
    }

    private JPanel booksPanel() {
        JPanel p = Ui.panel(new BorderLayout(0, 14));
        searchField = new JComboBox<>(new String[] {"书名", "作者", "出版社", "ISBN"});
        keyword = new JTextField(22);
        JButton search = Ui.primary("查询");
        search.addActionListener(e -> refreshBooks());
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(Ui.title("馆藏检索"), BorderLayout.WEST);
        JPanel actions = Ui.toolbar(searchField, keyword, search);
        JButton borrow = Ui.primary("借阅");
        borrow.addActionListener(e -> borrowSelected());
        JButton reserve = new JButton("预约");
        reserve.addActionListener(e -> reserveSelected());
        actions.add(borrow);
        actions.add(reserve);
        if (current.role() == Role.ADMIN) {
            JButton add = new JButton("新增");
            add.addActionListener(e -> editBook(null));
            JButton edit = new JButton("修改");
            edit.addActionListener(
                    e -> {
                        int i = Ui.selected(bookTable);
                        if (i < 0) Ui.info(this, "请先选择图书");
                        else editBook(bookRows.get(i));
                    });
            JButton delete = new JButton("删除");
            delete.addActionListener(e -> deleteBook());
            actions.add(add);
            actions.add(edit);
            actions.add(delete);
        }
        top.add(actions, BorderLayout.EAST);
        p.add(top, BorderLayout.NORTH);
        bookModel = Ui.model("ID", "ISBN", "书名", "作者", "出版社", "分类", "馆藏", "可借", "位置");
        JTable[] h = new JTable[1];
        p.add(Ui.table(bookModel, h), BorderLayout.CENTER);
        bookTable = h[0];
        refreshers.add(this::refreshBooks);
        return p;
    }

    private void refreshBooks() {
        bookRows = service.books((String) searchField.getSelectedItem(), keyword.getText());
        bookModel.setRowCount(0);
        for (Book b : bookRows)
            bookModel.addRow(
                    new Object[] {
                        b.id(),
                        b.isbn(),
                        b.title(),
                        b.author(),
                        b.publisher(),
                        b.category(),
                        b.totalCopies(),
                        b.availableCopies(),
                        b.location()
                    });
    }

    private void borrowSelected() {
        int i = Ui.selected(bookTable);
        if (i < 0) {
            Ui.info(this, "请先选择图书");
            return;
        }
        Book b = bookRows.get(i);
        User reader = current;
        if (current.role() == Role.ADMIN) {
            List<User> readers =
                    service.users().stream().filter(u -> u.role() == Role.READER).toList();
            JComboBox<User> box = new JComboBox<>(readers.toArray(User[]::new));
            if (JOptionPane.showConfirmDialog(this, box, "选择借阅人", JOptionPane.OK_CANCEL_OPTION)
                    != JOptionPane.OK_OPTION) return;
            reader = (User) box.getSelectedItem();
            if (reader == null) return;
        }
        try {
            service.borrow(reader.id(), b.id());
            Ui.info(this, "借阅成功，应还日期为14天后");
            refreshAll();
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

    private void reserveSelected() {
        int i = Ui.selected(bookTable);
        if (i < 0) {
            Ui.info(this, "请先选择图书");
            return;
        }
        if (current.role() != Role.READER) {
            Ui.info(this, "管理员不能以自身身份预约，请使用读者账号");
            return;
        }
        try {
            service.reserve(current.id(), bookRows.get(i).id());
            Ui.info(this, "预约成功，请在预约列表查看状态");
            refreshAll();
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

    private void editBook(Book b) {
        JTextField isbn = new JTextField(b == null ? "" : b.isbn()),
                title = new JTextField(b == null ? "" : b.title()),
                author = new JTextField(b == null ? "" : b.author()),
                publisher = new JTextField(b == null ? "" : b.publisher()),
                category = new JTextField(b == null ? "" : b.category()),
                copies = new JTextField(b == null ? "1" : String.valueOf(b.totalCopies())),
                location = new JTextField(b == null ? "" : b.location());
        JComponent[] fields = {isbn, title, author, publisher, category, copies, location};
        if (JOptionPane.showConfirmDialog(
                        this,
                        Ui.form(
                                new String[] {"ISBN*", "书名*", "作者*", "出版社*", "分类*", "馆藏数量*", "位置*"},
                                fields),
                        b == null ? "新增图书" : "修改图书",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE)
                == JOptionPane.OK_OPTION)
            try {
                int count = Integer.parseInt(copies.getText());
                if (b == null)
                    service.addBook(
                            isbn.getText(),
                            title.getText(),
                            author.getText(),
                            publisher.getText(),
                            category.getText(),
                            count,
                            location.getText());
                else
                    service.updateBook(
                            new Book(
                                    b.id(),
                                    isbn.getText(),
                                    title.getText(),
                                    author.getText(),
                                    publisher.getText(),
                                    category.getText(),
                                    count,
                                    b.availableCopies(),
                                    location.getText()),
                            category.getText());
                refreshAll();
            } catch (Exception e) {
                Ui.error(this, e);
            }
    }

    private void deleteBook() {
        int i = Ui.selected(bookTable);
        if (i < 0) {
            Ui.info(this, "请先选择图书");
            return;
        }
        Book b = bookRows.get(i);
        if (Ui.confirm(this, "确定删除《" + b.title() + "》？"))
            try {
                service.deleteBook(b.id());
                refreshAll();
            } catch (Exception e) {
                Ui.error(this, e);
            }
    }

    private JPanel loansPanel() {
        JPanel p = Ui.panel(new BorderLayout(0, 14));
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(Ui.title(current.role() == Role.ADMIN ? "借还工作台" : "我的借阅"), BorderLayout.WEST);
        JButton renew = new JButton("续借");
        renew.addActionListener(e -> renewLoan());
        JButton giveBack = Ui.primary("归还");
        giveBack.addActionListener(e -> returnLoan());
        JButton refresh = new JButton("刷新");
        refresh.addActionListener(e -> refreshLoans());
        top.add(Ui.toolbar(renew, giveBack, refresh), BorderLayout.EAST);
        p.add(top, BorderLayout.NORTH);
        loanModel = Ui.model("记录号", "读者", "书名", "借出时间", "应还时间", "归还时间", "续借", "状态");
        JTable[] h = new JTable[1];
        p.add(Ui.table(loanModel, h), BorderLayout.CENTER);
        loanTable = h[0];
        refreshers.add(this::refreshLoans);
        return p;
    }

    private void refreshLoans() {
        loanRows = service.loans(current.role() == Role.READER ? current.id() : null, false);
        loanModel.setRowCount(0);
        for (Loan l : loanRows)
            loanModel.addRow(
                    new Object[] {
                        l.id(),
                        l.readerName(),
                        l.bookTitle(),
                        fmt(l.borrowedAt()),
                        fmt(l.dueAt()),
                        fmt(l.returnedAt()),
                        l.renewCount(),
                        status(l.status())
                    });
    }

    private void renewLoan() {
        int i = Ui.selected(loanTable);
        if (i < 0) {
            Ui.info(this, "请先选择借阅记录");
            return;
        }
        try {
            service.renew(loanRows.get(i).id());
            Ui.info(this, "续借成功，应还日期顺延14天");
            refreshAll();
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

    private void returnLoan() {
        int i = Ui.selected(loanTable);
        if (i < 0) {
            Ui.info(this, "请先选择借阅记录");
            return;
        }
        try {
            service.returnBook(loanRows.get(i).id());
            Ui.info(this, "归还成功，逾期费用已自动结算");
            refreshAll();
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

    private JPanel reservationsPanel() {
        JPanel p = Ui.panel(new BorderLayout(0, 14));
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(Ui.title("预约管理"), BorderLayout.WEST);
        JButton cancel = new JButton("取消预约");
        cancel.addActionListener(e -> cancelReservation());
        JButton refresh = new JButton("刷新");
        refresh.addActionListener(e -> refreshReservations());
        top.add(Ui.toolbar(cancel, refresh), BorderLayout.EAST);
        p.add(top, BorderLayout.NORTH);
        reservationModel = Ui.model("预约号", "读者", "书名", "预约时间", "保留至", "状态", "到馆提醒");
        JTable[] h = new JTable[1];
        p.add(Ui.table(reservationModel, h), BorderLayout.CENTER);
        reservationTable = h[0];
        refreshers.add(this::refreshReservations);
        return p;
    }

    private void refreshReservations() {
        reservationRows = service.reservations(current.role() == Role.READER ? current.id() : null);
        reservationModel.setRowCount(0);
        for (Reservation r : reservationRows)
            reservationModel.addRow(
                    new Object[] {
                        r.id(),
                        r.readerName(),
                        r.bookTitle(),
                        fmt(r.reservedAt()),
                        fmt(r.expiresAt()),
                        status(r.status()),
                        r.notified() ? "已提醒" : "-"
                    });
    }

    private void cancelReservation() {
        if (current.role() != Role.READER) {
            Ui.info(this, "请由读者账号取消预约");
            return;
        }
        int i = Ui.selected(reservationTable);
        if (i < 0) {
            Ui.info(this, "请先选择预约记录");
            return;
        }
        try {
            service.cancelReservation(reservationRows.get(i).id(), current.id());
            refreshAll();
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

    private JPanel finesPanel() {
        JPanel p = Ui.panel(new BorderLayout(0, 14));
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(Ui.title("罚款记录"), BorderLayout.WEST);
        JButton pay = Ui.primary("确认缴费");
        pay.addActionListener(e -> payFine());
        JButton refresh = new JButton("刷新");
        refresh.addActionListener(e -> refreshFines());
        top.add(Ui.toolbar(pay, refresh), BorderLayout.EAST);
        p.add(top, BorderLayout.NORTH);
        fineModel = Ui.model("罚款号", "读者", "图书", "金额", "原因", "状态", "缴费时间");
        JTable[] h = new JTable[1];
        p.add(Ui.table(fineModel, h), BorderLayout.CENTER);
        fineTable = h[0];
        refreshers.add(this::refreshFines);
        return p;
    }

    private void refreshFines() {
        fineRows = service.fines(current.role() == Role.READER ? current.id() : null);
        fineModel.setRowCount(0);
        for (Fine f : fineRows)
            fineModel.addRow(
                    new Object[] {
                        f.id(),
                        f.readerName(),
                        f.bookTitle(),
                        "¥ " + f.amount(),
                        f.reason(),
                        status(f.status()),
                        fmt(f.paidAt())
                    });
    }

    private void payFine() {
        if (current.role() != Role.READER) {
            Ui.info(this, "缴费确认需由读者账号操作");
            return;
        }
        int i = Ui.selected(fineTable);
        if (i < 0) {
            Ui.info(this, "请先选择罚款记录");
            return;
        }
        Fine f = fineRows.get(i);
        if (Ui.confirm(this, "确认已缴纳 ¥ " + f.amount() + "？"))
            try {
                service.payFine(f.id(), current.id());
                refreshAll();
            } catch (Exception e) {
                Ui.error(this, e);
            }
    }

    private JPanel usersPanel() {
        JPanel p = Ui.panel(new BorderLayout(0, 14));
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(Ui.title("读者与借阅证"), BorderLayout.WEST);
        JButton add = new JButton("新增读者");
        add.addActionListener(e -> addReader());
        JButton toggle = Ui.primary("启用 / 停用借阅证");
        toggle.addActionListener(e -> toggleCard());
        top.add(Ui.toolbar(add, toggle), BorderLayout.EAST);
        p.add(top, BorderLayout.NORTH);
        userModel = Ui.model("ID", "账号", "姓名", "手机", "邮箱", "角色", "借阅证状态");
        JTable[] h = new JTable[1];
        p.add(Ui.table(userModel, h), BorderLayout.CENTER);
        userTable = h[0];
        refreshers.add(this::refreshUsers);
        return p;
    }

    private void refreshUsers() {
        userRows = service.users();
        userModel.setRowCount(0);
        for (User u : userRows)
            userModel.addRow(
                    new Object[] {
                        u.id(),
                        u.username(),
                        u.fullName(),
                        u.phone(),
                        u.email(),
                        u.role() == Role.ADMIN ? "管理员" : "读者",
                        status(u.cardStatus())
                    });
    }

    private void addReader() {
        JTextField u = new JTextField(),
                n = new JTextField(),
                phone = new JTextField(),
                email = new JTextField();
        JPasswordField pwd = new JPasswordField();
        if (JOptionPane.showConfirmDialog(
                        this,
                        Ui.form(
                                new String[] {"用户名*", "初始密码*", "姓名*", "手机", "邮箱"},
                                new JComponent[] {u, pwd, n, phone, email}),
                        "新增读者",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE)
                == JOptionPane.OK_OPTION)
            try {
                service.register(
                        u.getText(),
                        new String(pwd.getPassword()),
                        n.getText(),
                        phone.getText(),
                        email.getText(),
                        Role.READER);
                refreshAll();
            } catch (Exception e) {
                Ui.error(this, e);
            }
    }

    private void toggleCard() {
        int i = Ui.selected(userTable);
        if (i < 0) {
            Ui.info(this, "请先选择读者");
            return;
        }
        User u = userRows.get(i);
        if (u.role() != Role.READER) {
            Ui.info(this, "管理员没有借阅证状态");
            return;
        }
        try {
            service.updateCard(u.id(), "ACTIVE".equals(u.cardStatus()) ? "SUSPENDED" : "ACTIVE");
            refreshAll();
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

    private JPanel statisticsPanel() {
        JPanel p = Ui.panel(new BorderLayout(0, 14));
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(Ui.title("热门图书排行榜"), BorderLayout.WEST);
        JButton refresh = new JButton("刷新统计");
        refresh.addActionListener(e -> refreshStats());
        top.add(refresh, BorderLayout.EAST);
        p.add(top, BorderLayout.NORTH);
        rankingModel = Ui.model("排名", "书名", "作者", "累计借阅次数");
        JTable[] h = new JTable[1];
        p.add(Ui.table(rankingModel, h), BorderLayout.CENTER);
        refreshers.add(this::refreshStats);
        return p;
    }

    private void refreshStats() {
        List<Ranking> rows = service.rankings();
        rankingModel.setRowCount(0);
        for (int i = 0; i < rows.size(); i++) {
            Ranking r = rows.get(i);
            rankingModel.addRow(new Object[] {i + 1, r.title(), r.author(), r.borrowCount()});
        }
    }

    private void editProfile() {
        JTextField name = new JTextField(current.fullName()),
                phone = new JTextField(current.phone()),
                email = new JTextField(current.email());
        if (JOptionPane.showConfirmDialog(
                        this,
                        Ui.form(
                                new String[] {"姓名*", "手机", "邮箱"},
                                new JComponent[] {name, phone, email}),
                        "个人资料",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE)
                == JOptionPane.OK_OPTION)
            try {
                service.updateProfile(
                        current.id(), name.getText(), phone.getText(), email.getText());
                current =
                        new User(
                                current.id(),
                                current.username(),
                                name.getText().trim(),
                                phone.getText().trim(),
                                email.getText().trim(),
                                current.role(),
                                current.cardStatus());
                Ui.info(this, "资料已更新");
            } catch (Exception e) {
                Ui.error(this, e);
            }
    }

    private void refreshAll() {
        try {
            for (Runnable r : refreshers) r.run();
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

    private static String fmt(java.time.LocalDateTime t) {
        return t == null ? "-" : TIME.format(t);
    }

    private static String status(String s) {
        return switch (s) {
            case "ACTIVE" -> "正常";
            case "SUSPENDED" -> "停用";
            case "BORROWED" -> "借阅中";
            case "RETURNED" -> "已归还";
            case "WAITING" -> "排队中";
            case "READY" -> "待取书";
            case "FULFILLED" -> "已完成";
            case "CANCELLED" -> "已取消";
            case "EXPIRED" -> "已过期";
            case "UNPAID" -> "待缴";
            case "PAID" -> "已缴";
            default -> s;
        };
    }
}
