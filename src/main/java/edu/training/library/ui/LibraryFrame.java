package edu.training.library.ui;

import edu.training.library.model.Models.*;
import edu.training.library.service.LibraryService;
import edu.training.library.service.StatisticsExport;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableModel;

public final class LibraryFrame extends JFrame {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final LibraryService service;
    private User current;
    private final CardLayout pageLayout = new CardLayout();
    private final JPanel pages = new JPanel(pageLayout);
    private final JLabel pageGlyph = new JLabel("01");
    private final JLabel pageTitle = new JLabel("总览");
    private final JLabel accountAvatar = new JLabel("", SwingConstants.CENTER);
    private final JLabel accountNameLabel = new JLabel();
    private final JLabel accountMetaLabel = new JLabel();
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
    private final RankingChart dashboardRanking = new RankingChart();
    private DefaultTableModel recentLoanModel;
    private JLabel dashboardMeta;
    private DefaultTableModel rankingModel;
    private RankingChart statisticsChart;
    private final RankingChart monthlyChart = new RankingChart("月借阅统计图", "月借阅统计", "暂无月借阅数据");
    private DefaultTableModel categoryModel;

    public LibraryFrame(LibraryService service, User user) {
        super("馆藏通 - " + user.fullName());
        this.service = service;
        this.current = user;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1220, 760);
        setMinimumSize(new Dimension(980, 650));
        setLocationRelativeTo(null);
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Ui.BACKGROUND);
        Map<String, JPanel> pagePanels = buildPages();
        for (Map.Entry<String, JPanel> page : pagePanels.entrySet()) {
            pages.add(page.getValue(), page.getKey());
        }
        JPanel workspace = new JPanel(new BorderLayout());
        workspace.setOpaque(false);
        workspace.add(header(), BorderLayout.NORTH);
        workspace.add(pages, BorderLayout.CENTER);
        root.add(sidebar(), BorderLayout.WEST);
        root.add(workspace, BorderLayout.CENTER);
        setContentPane(root);
        refreshAll();
        SwingUtilities.invokeLater(this::showReadyReservationNotice);
    }

    private Map<String, JPanel> buildPages() {
        Map<String, JPanel> result = new LinkedHashMap<>();
        result.put("dashboard", dashboardPanel());
        result.put("books", booksPanel());
        if (current.role() == Role.ADMIN) result.put("users", usersPanel());
        result.put("loans", loansPanel());
        result.put("reservations", reservationsPanel());
        result.put("fines", finesPanel());
        result.put("statistics", statisticsPanel());
        return result;
    }

    private JPanel header() {
        JPanel panel = new JPanel(new BorderLayout(18, 0));
        panel.setBackground(new Color(255, 253, 249));
        panel.setBorder(
                BorderFactory.createCompoundBorder(
                        new MatteBorder(0, 0, 1, 0, Ui.OUTLINE_SOFT),
                        new EmptyBorder(0, 24, 0, 24)));
        panel.setPreferredSize(new Dimension(1, 72));

        JPanel heading = Ui.toolbar(pageGlyph, pageTitle);
        pageGlyph.setFont(Ui.bodyFont(Font.BOLD, 24f));
        pageTitle.setFont(Ui.serifFont(Font.BOLD, 22f));
        panel.add(heading, BorderLayout.WEST);
        return panel;
    }

    private JPanel sidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(Ui.SIDEBAR);
        sidebar.setPreferredSize(new Dimension(236, 1));
        sidebar.setMinimumSize(new Dimension(200, 0));
        sidebar.setBorder(new MatteBorder(0, 0, 0, 2, Ui.OUTLINE));

        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setBorder(new EmptyBorder(18, 20, 12, 16));
        JLabel name = new JLabel("馆藏通");
        name.setFont(Ui.serifFont(Font.BOLD, 26f));
        name.setForeground(Ui.INK);
        JLabel edition = Ui.eyebrow("LIBRARY DESK / 2026");
        JLabel role = Ui.muted(current.role() == Role.ADMIN ? "管理员工作台" : "读者服务台");
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        edition.setAlignmentX(Component.LEFT_ALIGNMENT);
        role.setAlignmentX(Component.LEFT_ALIGNMENT);
        brand.add(name);
        brand.add(Box.createVerticalStrut(4));
        brand.add(edition);
        brand.add(Box.createVerticalStrut(4));
        brand.add(role);
        sidebar.add(brand, BorderLayout.NORTH);

        JPanel navigation = new FillWidthBox();
        navigation.setOpaque(false);
        navigation.setBackground(Ui.SIDEBAR);
        navigation.setBorder(new EmptyBorder(8, 12, 8, 12));
        ButtonGroup group = new ButtonGroup();
        JToggleButton first = null;
        for (LibraryNavigation.Item item : LibraryNavigation.forRole(current.role())) {
            JToggleButton button = Ui.navigation(item.label(), item.glyph());
            button.addActionListener(event -> showPage(item));
            group.add(button);
            navigation.add(button);
            navigation.add(Box.createVerticalStrut(4));
            if (first == null) first = button;
        }
        if (first != null) first.setSelected(true);
        JScrollPane navScroll = new JScrollPane(navigation);
        navScroll.setBorder(null);
        navScroll.setOpaque(false);
        navScroll.getViewport().setOpaque(false);
        navScroll.getViewport().setBackground(Ui.SIDEBAR);
        navScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        navScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        navScroll.getVerticalScrollBar().setUnitIncrement(16);
        navScroll.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
        sidebar.add(navScroll, BorderLayout.CENTER);
        sidebar.add(accountPanel(), BorderLayout.SOUTH);
        return sidebar;
    }

    /** 侧栏导航：宽跟视口，高超出才滚。 */
    private static final class FillWidthBox extends JPanel implements Scrollable {
        private FillWidthBox() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension preferred = super.getPreferredSize();
            Container parent = getParent();
            if (parent != null && parent.getWidth() > 0) {
                preferred.width = Math.max(preferred.width, parent.getWidth());
            }
            return preferred;
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return Math.max(16, visible.height - 16);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private JPanel accountPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        panel.setBorder(
                BorderFactory.createCompoundBorder(
                        new MatteBorder(1, 0, 0, 0, Ui.OUTLINE_SOFT),
                        new EmptyBorder(10, 12, 12, 12)));

        JPanel identity = new JPanel(new BorderLayout(10, 0));
        identity.setOpaque(false);

        accountAvatar.setOpaque(true);
        accountAvatar.setBackground(Ui.PAPER_DARK);
        accountAvatar.setForeground(Ui.INK);
        accountAvatar.setFont(Ui.serifFont(Font.BOLD, 15f));
        accountAvatar.setBorder(BorderFactory.createLineBorder(Ui.OUTLINE_SOFT));
        accountAvatar.setPreferredSize(new Dimension(36, 36));
        accountAvatar.setMinimumSize(new Dimension(36, 36));
        accountAvatar.setMaximumSize(new Dimension(36, 36));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        accountNameLabel.setFont(Ui.bodyFont(Font.BOLD, 13f));
        accountNameLabel.setForeground(Ui.INK);
        accountNameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        accountMetaLabel.setFont(Ui.bodyFont(Font.PLAIN, 11f));
        accountMetaLabel.setForeground(Ui.MUTED);
        accountMetaLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(accountNameLabel);
        text.add(Box.createVerticalStrut(2));
        text.add(accountMetaLabel);
        identity.add(accountAvatar, BorderLayout.WEST);
        identity.add(text, BorderLayout.CENTER);
        refreshAccountLabels();

        JButton profile = Ui.sidebarAction("资料");
        profile.setToolTipText("编辑个人资料");
        profile.addActionListener(e -> editProfile());
        JButton password = Ui.sidebarAction("密码");
        password.setToolTipText("修改登录密码");
        password.addActionListener(e -> changePassword());
        JButton logout = Ui.sidebarDanger("退出");
        logout.setToolTipText("退出当前账号");
        logout.addActionListener(e -> logout());

        JPanel actions = new JPanel(new GridLayout(1, 3, 6, 0));
        actions.setOpaque(false);
        actions.add(profile);
        actions.add(password);
        actions.add(logout);

        panel.add(identity, BorderLayout.NORTH);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private void refreshAccountLabels() {
        String displayName =
                current.fullName() == null || current.fullName().isBlank()
                        ? current.username()
                        : current.fullName().trim();
        accountAvatar.setText(avatarInitial(displayName));
        accountNameLabel.setText(displayName);
        accountMetaLabel.setText(
                (current.role() == Role.ADMIN ? "管理员" : "读者") + " · " + current.username());
        accountNameLabel.setToolTipText(displayName);
        accountMetaLabel.setToolTipText(current.username());
        setTitle("馆藏通 - " + displayName);
    }

    private static String avatarInitial(String name) {
        if (name == null || name.isBlank()) return "?";
        int codePoint = name.codePointAt(0);
        return new String(Character.toChars(codePoint)).toUpperCase(java.util.Locale.ROOT);
    }

    private void showPage(LibraryNavigation.Item item) {
        pageGlyph.setText(item.glyph());
        pageTitle.setText(item.label());
        pageLayout.show(pages, item.id());
    }

    private void logout() {
        if (!Ui.confirm(this, "确认退出当前账号？")) return;
        dispose();
        new LoginFrame(service).setVisible(true);
    }

    private JPanel dashboardPanel() {
        JPanel p = Ui.panel(new BorderLayout(0, 14));
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JPanel context = new JPanel();
        context.setOpaque(false);
        context.setLayout(new BoxLayout(context, BoxLayout.Y_AXIS));
        JLabel eyebrow = Ui.eyebrow("COLLECTION STATUS");
        JLabel note = Ui.muted("馆藏、流通与预约状态实时汇总");
        eyebrow.setAlignmentX(Component.LEFT_ALIGNMENT);
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        context.add(eyebrow);
        context.add(Box.createVerticalStrut(4));
        context.add(note);
        top.add(context, BorderLayout.WEST);
        JButton refresh = Ui.secondary("刷新数据");
        refresh.addActionListener(e -> refreshAll());
        top.add(refresh, BorderLayout.EAST);
        p.add(top, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setOpaque(false);
        dashboardMetrics = new JPanel(new GridLayout(1, 4, 14, 0));
        dashboardMetrics.setOpaque(false);
        dashboardMetrics.setPreferredSize(new Dimension(1, 132));
        body.add(dashboardMetrics, BorderLayout.NORTH);

        JPanel details = new JPanel(new GridBagLayout());
        details.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weighty = 1;

        PaperPanel chartPaper = Ui.paper(new BorderLayout(0, 8));
        JPanel chartHeader = new JPanel(new BorderLayout());
        chartHeader.setOpaque(false);
        chartHeader.add(Ui.sectionTitle("热门借阅趋势"), BorderLayout.WEST);
        dashboardMeta = Ui.muted("");
        chartHeader.add(dashboardMeta, BorderLayout.EAST);
        chartPaper.add(chartHeader, BorderLayout.NORTH);
        chartPaper.add(dashboardRanking, BorderLayout.CENTER);
        constraints.gridx = 0;
        constraints.weightx = 0.60;
        constraints.insets = new Insets(0, 0, 0, 14);
        details.add(chartPaper, constraints);

        PaperPanel recentPaper = Ui.paper(new BorderLayout(0, 8));
        recentPaper.add(Ui.sectionTitle("近期借阅记录"), BorderLayout.NORTH);
        recentPaper.setPadding(16, 14, 16, 14);
        recentLoanModel = Ui.model("借阅记录", "状态");
        JTable[] recentHolder = new JTable[1];
        recentPaper.add(Ui.table(recentLoanModel, recentHolder), BorderLayout.CENTER);
        constraints.gridx = 1;
        constraints.weightx = 0.40;
        constraints.insets = new Insets(0, 0, 0, 0);
        details.add(recentPaper, constraints);
        body.add(details, BorderLayout.CENTER);
        p.add(body, BorderLayout.CENTER);
        refreshers.add(this::refreshDashboard);
        return p;
    }

    private void refreshDashboard() {
        Long readerId = current.role() == Role.READER ? current.id() : null;
        Dashboard d = service.dashboard(readerId);
        dashboardMetrics.removeAll();
        dashboardMetrics.add(metric("馆藏总量", d.totalCopies() + " 册", Ui.OUTLINE));
        dashboardMetrics.add(metric("当前可借", d.availableCopies() + " 册", Ui.SUCCESS));
        dashboardMetrics.add(
                metric(
                        current.role() == Role.READER ? "我的在借" : "在借记录",
                        d.activeLoans() + " 条",
                        Ui.INK));
        dashboardMetrics.add(
                metric(
                        current.role() == Role.READER ? "我的预约" : "有效预约",
                        d.waitingReservations() + " 条",
                        Ui.ACCENT));
        List<Ranking> rankings = service.rankings();
        dashboardRanking.setRows(rankings);
        if (current.role() == Role.READER) {
            long ready =
                    service.reservations(current.id()).stream()
                            .filter(r -> "READY".equals(r.status()))
                            .count();
            dashboardMeta.setText(
                    ready > 0
                            ? "有 " + ready + " 本预约图书待取 · 待缴 ¥" + d.unpaidFines()
                            : "我的待缴 ¥" + d.unpaidFines());
        } else {
            dashboardMeta.setText("图书品种 " + d.bookKinds() + " · 待缴 ¥" + d.unpaidFines());
        }
        List<Loan> recent = service.loans(readerId, false).stream().limit(6).toList();
        recentLoanModel.setRowCount(0);
        for (Loan loan : recent) {
            recentLoanModel.addRow(
                    new Object[] {loan.readerName() + " · " + loan.bookTitle(), loanStatus(loan)});
        }
        if (recent.isEmpty()) recentLoanModel.addRow(new Object[] {"暂无借阅记录", "—"});
        dashboardMetrics.revalidate();
        dashboardMetrics.repaint();
    }

    private JPanel metric(String label, String value, Color accent) {
        PaperPanel p = Ui.paper(new GridBagLayout(), accent);
        p.setPadding(16, 18, 14, 18);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.weightx = 1;
        g.anchor = GridBagConstraints.WEST;
        JLabel l = Ui.eyebrow(label);
        p.add(l, g);
        g.gridy = 1;
        g.insets = new Insets(8, 0, 0, 0);
        JLabel v = new JLabel(value);
        v.setFont(Ui.serifFont(Font.BOLD, 30f));
        v.setForeground(Ui.INK);
        p.add(v, g);
        return p;
    }

    private JPanel booksPanel() {
        JPanel p = Ui.panel(new BorderLayout(0, 14));
        searchField = new JComboBox<>(new String[] {"书名", "作者", "出版社", "ISBN"});
        keyword = new JTextField(22);
        Ui.styleField(searchField);
        Ui.styleField(keyword);
        JButton search = Ui.primary("查询");
        search.addActionListener(e -> refreshBooks());
        keyword.addActionListener(e -> refreshBooks());

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JLabel heading = Ui.title("馆藏目录");
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel primaryBar = new JPanel(new BorderLayout(12, 0));
        primaryBar.setOpaque(false);
        primaryBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel filters = Ui.toolbar(searchField, keyword, search);
        JPanel borrowBar = Ui.toolbar();
        JButton borrow = Ui.primary("借阅");
        borrow.addActionListener(e -> borrowSelected());
        borrowBar.add(borrow);
        if (current.role() == Role.READER) {
            JButton reserve = Ui.secondary("预约");
            reserve.addActionListener(e -> reserveSelected());
            borrowBar.add(reserve);
        }
        primaryBar.add(filters, BorderLayout.WEST);
        primaryBar.add(borrowBar, BorderLayout.EAST);

        top.add(heading);
        top.add(Box.createVerticalStrut(10));
        top.add(primaryBar);

        if (current.role() == Role.ADMIN) {
            JPanel manageBar = Ui.toolbar();
            manageBar.setAlignmentX(Component.LEFT_ALIGNMENT);
            JButton category = Ui.secondary("分类管理");
            category.addActionListener(e -> manageCategories());
            JButton add = Ui.secondary("新增");
            add.addActionListener(e -> editBook(null));
            JButton edit = Ui.secondary("修改");
            edit.addActionListener(
                    e -> {
                        int i = Ui.selected(bookTable);
                        if (i < 0) Ui.info(this, "请先选择图书");
                        else editBook(bookRows.get(i));
                    });
            JButton delete = Ui.danger("删除");
            delete.addActionListener(e -> deleteBook());
            manageBar.add(category);
            manageBar.add(add);
            manageBar.add(edit);
            manageBar.add(delete);
            top.add(Box.createVerticalStrut(8));
            top.add(manageBar);
        }
        p.add(top, BorderLayout.NORTH);
        bookModel = Ui.model("ISBN", "书名", "作者", "出版社", "分类", "馆藏", "可借", "位置");
        JTable[] h = new JTable[1];
        p.add(Ui.table(bookModel, h), BorderLayout.CENTER);
        bookTable = h[0];
        refreshers.add(this::refreshBooks);
        return p;
    }

    private void manageCategories() {
        DefaultListModel<String> model = new DefaultListModel<>();
        service.categories().forEach(model::addElement);
        JList<String> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setVisibleRowCount(10);
        list.setFont(Ui.bodyFont(Font.PLAIN, 14f));
        JScrollPane scroll = new JScrollPane(list);
        scroll.setPreferredSize(new Dimension(280, 250));

        JButton add = Ui.secondary("新增");
        JButton rename = Ui.secondary("重命名");
        JButton delete = Ui.danger("删除");
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.add(scroll, BorderLayout.CENTER);
        content.add(Ui.toolbar(add, rename, delete), BorderLayout.SOUTH);

        add.addActionListener(
                e -> {
                    String name =
                            JOptionPane.showInputDialog(
                                    this, "请输入分类名称", "新增分类", JOptionPane.PLAIN_MESSAGE);
                    if (name == null) return;
                    try {
                        service.addCategory(name);
                        reloadCategories(model);
                    } catch (Exception exception) {
                        Ui.error(this, exception);
                    }
                });
        rename.addActionListener(
                e -> {
                    String selected = list.getSelectedValue();
                    if (selected == null) {
                        Ui.info(this, "请先选择分类");
                        return;
                    }
                    String name =
                            (String)
                                    JOptionPane.showInputDialog(
                                            this,
                                            "请输入新的分类名称",
                                            "重命名分类",
                                            JOptionPane.PLAIN_MESSAGE,
                                            null,
                                            null,
                                            selected);
                    if (name == null) return;
                    try {
                        service.renameCategory(selected, name);
                        reloadCategories(model);
                        refreshAll();
                    } catch (Exception exception) {
                        Ui.error(this, exception);
                    }
                });
        delete.addActionListener(
                e -> {
                    String selected = list.getSelectedValue();
                    if (selected == null) {
                        Ui.info(this, "请先选择分类");
                        return;
                    }
                    if (!Ui.confirm(this, "确认删除分类“" + selected + "”？")) return;
                    try {
                        service.deleteCategory(selected);
                        reloadCategories(model);
                    } catch (Exception exception) {
                        Ui.error(this, exception);
                    }
                });

        JOptionPane.showMessageDialog(this, content, "分类管理", JOptionPane.PLAIN_MESSAGE);
    }

    private void reloadCategories(DefaultListModel<String> model) {
        model.clear();
        service.categories().forEach(model::addElement);
    }

    private void refreshBooks() {
        String selectedIsbn = selectedBookIsbn();
        bookRows = service.books((String) searchField.getSelectedItem(), keyword.getText());
        bookModel.setRowCount(0);
        for (Book b : bookRows)
            bookModel.addRow(
                    new Object[] {
                        b.isbn(),
                        b.title(),
                        b.author(),
                        b.publisher(),
                        b.category(),
                        b.totalCopies(),
                        b.availableCopies(),
                        b.location()
                    });
        restoreBookSelection(selectedIsbn);
    }

    private String selectedBookIsbn() {
        int i = Ui.selected(bookTable);
        if (i < 0 || i >= bookRows.size()) return null;
        return bookRows.get(i).isbn();
    }

    private void restoreBookSelection(String isbn) {
        if (isbn == null || bookTable == null) return;
        for (int modelRow = 0; modelRow < bookRows.size(); modelRow++) {
            if (!isbn.equals(bookRows.get(modelRow).isbn())) continue;
            int viewRow = bookTable.convertRowIndexToView(modelRow);
            if (viewRow >= 0) {
                bookTable.setRowSelectionInterval(viewRow, viewRow);
                bookTable.scrollRectToVisible(bookTable.getCellRect(viewRow, 0, true));
            }
            return;
        }
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
                    service.users().stream()
                            .filter(u -> u.role() == Role.READER && "ACTIVE".equals(u.cardStatus()))
                            .toList();
            if (readers.isEmpty()) {
                Ui.info(this, "暂无可办理借阅的读者");
                return;
            }
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
        JButton renew = Ui.secondary("续借");
        renew.addActionListener(e -> renewLoan());
        JButton giveBack = Ui.primary("归还");
        giveBack.addActionListener(e -> returnLoan());
        JButton refresh = Ui.secondary("刷新");
        refresh.addActionListener(e -> refreshLoans());
        top.add(Ui.toolbar(renew, giveBack, refresh), BorderLayout.EAST);
        p.add(top, BorderLayout.NORTH);
        loanModel = Ui.model("记录号", "读者", "书名", "状态", "借出时间", "应还时间", "归还时间", "续借");
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
                        loanStatus(l),
                        fmt(l.borrowedAt()),
                        fmt(l.dueAt()),
                        fmt(l.returnedAt()),
                        l.renewCount()
                    });
    }

    private void renewLoan() {
        int i = Ui.selected(loanTable);
        if (i < 0) {
            Ui.info(this, "请先选择借阅记录");
            return;
        }
        Loan loan = loanRows.get(i);
        if (!"BORROWED".equals(loan.status())) {
            Ui.info(this, "已归还的图书不能续借");
            return;
        }
        if (loan.renewCount() >= 1) {
            Ui.info(this, "该记录已续借过一次");
            return;
        }
        try {
            service.renew(loan.id());
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
        Loan loan = loanRows.get(i);
        if (!"BORROWED".equals(loan.status())) {
            Ui.info(this, "该记录已归还，无需重复操作");
            return;
        }
        if (!Ui.confirm(this, "确认归还《" + loan.bookTitle() + "》？")) return;
        try {
            service.returnBook(loan.id());
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
        JButton refresh = Ui.secondary("刷新");
        refresh.addActionListener(e -> refreshReservations());
        if (current.role() == Role.READER) {
            JButton cancel = Ui.danger("取消预约");
            cancel.addActionListener(e -> cancelReservation());
            top.add(Ui.toolbar(cancel, refresh), BorderLayout.EAST);
        } else {
            top.add(Ui.toolbar(refresh), BorderLayout.EAST);
        }
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
        Reservation reservation = reservationRows.get(i);
        if (!"WAITING".equals(reservation.status()) && !"READY".equals(reservation.status())) {
            Ui.info(this, "该预约已结束，无需取消");
            return;
        }
        if (!Ui.confirm(this, "确认取消对《" + reservation.bookTitle() + "》的预约？")) return;
        try {
            service.cancelReservation(reservation.id(), current.id());
            Ui.info(this, "预约已取消");
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
        JButton refresh = Ui.secondary("刷新");
        refresh.addActionListener(e -> refreshFines());
        if (current.role() == Role.READER) {
            JButton pay = Ui.primary("确认缴费");
            pay.addActionListener(e -> payFine());
            top.add(Ui.toolbar(pay, refresh), BorderLayout.EAST);
        } else {
            top.add(Ui.toolbar(refresh), BorderLayout.EAST);
        }
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
        if (!"UNPAID".equals(f.status())) {
            Ui.info(this, "该罚款已缴清，无需重复确认");
            return;
        }
        if (Ui.confirm(this, "确认已缴纳 ¥ " + f.amount() + "？"))
            try {
                service.payFine(f.id(), current.id());
                Ui.info(this, "缴费状态已更新");
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
        JButton add = Ui.secondary("新增读者");
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
        String next = "ACTIVE".equals(u.cardStatus()) ? "停用" : "启用";
        if (!Ui.confirm(this, "确认" + next + "读者“" + u.fullName() + "”的借阅证？")) return;
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
        top.add(Ui.title("统计分析"), BorderLayout.WEST);
        JButton export = Ui.primary("导出报表");
        export.addActionListener(e -> exportStatistics());
        JButton refresh = Ui.secondary("刷新统计");
        refresh.addActionListener(e -> refreshStats());
        top.add(Ui.toolbar(export, refresh), BorderLayout.EAST);
        p.add(top, BorderLayout.NORTH);

        rankingModel = Ui.model("排名", "书名", "作者", "累计借阅次数");
        JTable[] h = new JTable[1];
        statisticsChart = new RankingChart();
        PaperPanel chart = Ui.paper(new BorderLayout(0, 8));
        chart.add(Ui.eyebrow("热门借阅排行"), BorderLayout.NORTH);
        chart.add(statisticsChart, BorderLayout.CENTER);

        PaperPanel monthlyPaper = Ui.paper(new BorderLayout(0, 8));
        monthlyPaper.add(Ui.eyebrow("月借阅统计"), BorderLayout.NORTH);
        monthlyPaper.add(monthlyChart, BorderLayout.CENTER);

        categoryModel = Ui.model("分类", "品种", "馆藏总量", "可借数量", "在借数量");
        JTable[] categoryHolder = new JTable[1];
        JScrollPane categoryTable = Ui.table(categoryModel, categoryHolder);

        JPanel content = new JPanel(new GridLayout(2, 2, 14, 14));
        content.setOpaque(false);
        content.add(chart);
        content.add(Ui.table(rankingModel, h));
        content.add(monthlyPaper);
        content.add(categoryTable);
        p.add(content, BorderLayout.CENTER);
        refreshers.add(this::refreshStats);
        return p;
    }

    private void refreshStats() {
        List<Ranking> rows = service.rankings();
        statisticsChart.setRows(rows);
        rankingModel.setRowCount(0);
        for (int i = 0; i < rows.size(); i++) {
            Ranking r = rows.get(i);
            rankingModel.addRow(new Object[] {i + 1, r.title(), r.author(), r.borrowCount()});
        }
        List<MonthlyStat> monthly = service.monthlyStats();
        monthlyChart.setRows(
                monthly.subList(Math.max(0, monthly.size() - 7), monthly.size()).stream()
                        .map(m -> new Ranking(m.month().substring(5) + "月", "", m.borrowCount()))
                        .toList());
        categoryModel.setRowCount(0);
        for (CategoryStock s : service.categoryStocks())
            categoryModel.addRow(
                    new Object[] {
                        s.category(),
                        s.bookKinds(),
                        s.totalCopies(),
                        s.availableCopies(),
                        s.totalCopies() - s.availableCopies()
                    });
    }

    private void exportStatistics() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("导出统计报表");
        chooser.setSelectedFile(
                new java.io.File(
                        "统计报表-"
                                + DateTimeFormatter.ofPattern("yyyyMMdd")
                                        .format(java.time.LocalDate.now())
                                + ".xlsx"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        java.io.File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".xlsx"))
            file = new java.io.File(file.getParentFile(), file.getName() + ".xlsx");
        if (file.exists() && !Ui.confirm(this, "文件“" + file.getName() + "”已存在，确认覆盖？")) return;
        try {
            StatisticsExport.write(
                    file.toPath(),
                    service.dashboard(),
                    service.rankings(),
                    service.monthlyStats(),
                    service.categoryStocks());
            Ui.info(this, "报表已导出到 " + file.getAbsolutePath());
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

    private void changePassword() {
        JPasswordField currentPassword = new JPasswordField();
        JPasswordField newPassword = new JPasswordField();
        JPasswordField confirmation = new JPasswordField();
        JComponent[] fields = {currentPassword, newPassword, confirmation};
        if (JOptionPane.showConfirmDialog(
                        this,
                        Ui.form(new String[] {"当前密码*", "新密码*", "确认新密码*"}, fields),
                        "修改密码",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE)
                != JOptionPane.OK_OPTION) return;
        try {
            service.changePassword(
                    current.id(),
                    new String(currentPassword.getPassword()),
                    new String(newPassword.getPassword()),
                    new String(confirmation.getPassword()));
            Ui.info(this, "密码已修改，下次登录请使用新密码");
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

    private void showReadyReservationNotice() {
        if (current.role() != Role.READER || !isDisplayable()) return;
        try {
            List<Reservation> ready =
                    service.reservations(current.id()).stream()
                            .filter(r -> "READY".equals(r.status()))
                            .toList();
            if (ready.isEmpty()) return;
            StringBuilder message = new StringBuilder("以下预约图书已到馆，请在保留期内取书：\n\n");
            for (Reservation reservation : ready)
                message.append("《")
                        .append(reservation.bookTitle())
                        .append("》  保留至 ")
                        .append(fmt(reservation.expiresAt()))
                        .append('\n');
            Ui.info(this, message.toString());
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

    private void editProfile() {
        JTextField username = new JTextField(current.username());
        username.setEditable(false);
        JTextField roleField = new JTextField(current.role() == Role.ADMIN ? "管理员" : "读者");
        roleField.setEditable(false);
        JTextField cardField =
                new JTextField(current.role() == Role.ADMIN ? "—" : status(current.cardStatus()));
        cardField.setEditable(false);
        JTextField name = new JTextField(current.fullName()),
                phone = new JTextField(current.phone()),
                email = new JTextField(current.email());
        JComponent[] fields = {username, roleField, cardField, name, phone, email};
        if (JOptionPane.showConfirmDialog(
                        this,
                        Ui.form(new String[] {"账号", "角色", "借阅证", "姓名*", "手机", "邮箱"}, fields),
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
                refreshAccountLabels();
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

    private static String loanStatus(Loan loan) {
        if ("BORROWED".equals(loan.status())
                && loan.dueAt() != null
                && loan.dueAt().isBefore(java.time.LocalDateTime.now())) return "已逾期";
        return status(loan.status());
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
