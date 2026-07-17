package edu.training.library.ui;

import edu.training.library.model.Models.Role;
import edu.training.library.model.Models.User;
import edu.training.library.service.LibraryService;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public final class LoginFrame extends JFrame {
    private final LibraryService service;
    private final JTextField username = new JTextField(18);
    private final JPasswordField password = new JPasswordField(18);

    public LoginFrame(LibraryService service) {
        super("馆藏通 - 图书馆借阅管理系统");
        this.service = service;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 560);
        setMinimumSize(new Dimension(840, 560));
        setLocationRelativeTo(null);
        GridPanel root = new GridPanel(new GridBagLayout());
        root.setBorder(new EmptyBorder(38, 46, 38, 46));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weighty = 1;
        constraints.gridx = 0;
        constraints.weightx = 0.44;
        constraints.insets = new Insets(0, 0, 0, 42);
        root.add(brandPanel(), constraints);
        constraints.gridx = 1;
        constraints.weightx = 0.56;
        constraints.insets = new Insets(0, 0, 0, 0);
        root.add(loginPanel(), constraints);
        setContentPane(root);
        getRootPane().setDefaultButton(findLoginButton());
    }

    private JPanel brandPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.anchor = GridBagConstraints.WEST;
        JLabel mark = Ui.eyebrow("LIBRARY DESK / ACCESS 01");
        p.add(mark, g);
        g.gridy = 1;
        g.insets = new Insets(14, 0, 0, 0);
        JLabel title = new JLabel("馆藏通");
        title.setForeground(Ui.INK);
        title.setFont(Ui.serifFont(Font.BOLD, 46f));
        p.add(title, g);
        g.gridy = 2;
        g.insets = new Insets(6, 0, 0, 0);
        JLabel sub = new JLabel("图书馆借阅管理工作台");
        sub.setForeground(Ui.MUTED);
        sub.setFont(Ui.bodyFont(Font.PLAIN, 16f));
        p.add(sub, g);
        g.gridy = 3;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(28, 0, 0, 0);
        JSeparator rule = new JSeparator();
        rule.setForeground(Ui.OUTLINE);
        p.add(rule, g);
        g.gridy = 4;
        g.fill = GridBagConstraints.NONE;
        g.insets = new Insets(18, 0, 0, 0);
        JLabel callNumber = new JLabel("索书号  TP311.13 / GCT");
        callNumber.setFont(Ui.bodyFont(Font.BOLD, 12f));
        callNumber.setForeground(Ui.INK);
        callNumber.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Ui.OUTLINE), new EmptyBorder(7, 10, 7, 10)));
        p.add(callNumber, g);
        g.gridy = 5;
        g.insets = new Insets(14, 0, 0, 0);
        JLabel note = Ui.muted("馆藏检索 · 借还流通 · 预约与统计");
        p.add(note, g);
        return p;
    }

    private JButton loginButton;

    private JPanel loginPanel() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setOpaque(false);
        PaperPanel p = Ui.paper(new BorderLayout(), Ui.INK);
        p.setPadding(22, 34, 18, 34);
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        JLabel code = Ui.eyebrow("MEMBER SIGN-IN");
        code.setAlignmentX(LEFT_ALIGNMENT);
        content.add(code);
        content.add(Box.createVerticalStrut(6));
        JLabel t = Ui.title("登录系统");
        t.setAlignmentX(LEFT_ALIGNMENT);
        content.add(t);
        content.add(Box.createVerticalStrut(4));
        JLabel hint = Ui.muted("使用借阅证账号或管理员账号登录");
        hint.setAlignmentX(LEFT_ALIGNMENT);
        content.add(hint);
        content.add(Box.createVerticalStrut(18));
        JLabel usernameLabel = label("用户名");
        usernameLabel.setLabelFor(username);
        content.add(usernameLabel);
        content.add(Box.createVerticalStrut(6));
        Ui.styleField(username);
        username.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        content.add(username);
        content.add(Box.createVerticalStrut(12));
        JLabel passwordLabel = label("密码");
        passwordLabel.setLabelFor(password);
        content.add(passwordLabel);
        content.add(Box.createVerticalStrut(6));
        Ui.styleField(password);
        password.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        content.add(password);
        content.add(Box.createVerticalStrut(16));
        loginButton = Ui.primary("登录");
        loginButton.setAlignmentX(LEFT_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        loginButton.addActionListener(e -> login());
        content.add(loginButton);
        content.add(Box.createVerticalStrut(10));
        JButton register = Ui.secondary("注册读者账号");
        register.setAlignmentX(LEFT_ALIGNMENT);
        register.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        register.addActionListener(e -> register());
        content.add(register);
        content.add(Box.createVerticalStrut(12));
        JSeparator rule = new JSeparator();
        rule.setForeground(Ui.OUTLINE_SOFT);
        rule.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        content.add(rule);
        content.add(Box.createVerticalStrut(8));
        JLabel demo =
                new JLabel(
                        "<html>管理员账号&nbsp; admin / admin123<br>读者账号&nbsp; reader / reader123</html>");
        demo.setFont(Ui.bodyFont(Font.PLAIN, 11f));
        demo.setForeground(Ui.MUTED);
        demo.setAlignmentX(LEFT_ALIGNMENT);
        content.add(demo);
        p.add(content, BorderLayout.CENTER);
        p.setPreferredSize(new Dimension(390, 430));
        outer.add(p);
        return outer;
    }

    private JButton findLoginButton() {
        return loginButton;
    }

    private JLabel label(String s) {
        JLabel l = new JLabel(s);
        l.setForeground(Ui.INK);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private void login() {
        try {
            User user = service.login(username.getText(), new String(password.getPassword()));
            dispose();
            new LibraryFrame(service, user).setVisible(true);
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

    private void register() {
        JTextField u = new JTextField(),
                n = new JTextField(),
                phone = new JTextField(),
                email = new JTextField();
        JPasswordField p = new JPasswordField();
        JComponent[] f = {u, p, n, phone, email};
        int result =
                JOptionPane.showConfirmDialog(
                        this,
                        Ui.form(new String[] {"用户名*", "密码*", "姓名*", "手机", "邮箱"}, f),
                        "注册读者账号",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION)
            try {
                service.register(
                        u.getText(),
                        new String(p.getPassword()),
                        n.getText(),
                        phone.getText(),
                        email.getText(),
                        Role.READER);
                username.setText(u.getText());
                Ui.info(this, "注册成功，请登录");
            } catch (Exception e) {
                Ui.error(this, e);
            }
    }
}
