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
        setSize(860, 540);
        setMinimumSize(new Dimension(760, 480));
        setLocationRelativeTo(null);
        JPanel root = new JPanel(new GridLayout(1, 2));
        root.add(brandPanel());
        root.add(loginPanel());
        setContentPane(root);
        getRootPane().setDefaultButton(findLoginButton());
    }

    private JPanel brandPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Ui.PRIMARY);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.anchor = GridBagConstraints.WEST;
        JLabel mark = new JLabel("LIBRARY DESK");
        mark.setForeground(new Color(189, 233, 225));
        mark.setFont(mark.getFont().deriveFont(Font.BOLD, 13f));
        p.add(mark, g);
        g.gridy = 1;
        g.insets = new Insets(18, 0, 0, 0);
        JLabel title = new JLabel("馆藏通");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 42f));
        p.add(title, g);
        g.gridy = 2;
        g.insets = new Insets(8, 0, 0, 0);
        JLabel sub = new JLabel("借阅、预约与馆藏管理工作台");
        sub.setForeground(new Color(225, 242, 239));
        sub.setFont(sub.getFont().deriveFont(16f));
        p.add(sub, g);
        return p;
    }

    private JButton loginButton;

    private JPanel loginPanel() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(Color.WHITE);
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(24, 48, 24, 48));
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel t = Ui.title("登录系统");
        t.setAlignmentX(LEFT_ALIGNMENT);
        p.add(t);
        p.add(Box.createVerticalStrut(8));
        JLabel hint = Ui.muted("使用借阅证账号或管理员账号登录");
        hint.setAlignmentX(LEFT_ALIGNMENT);
        p.add(hint);
        p.add(Box.createVerticalStrut(28));
        p.add(label("用户名"));
        p.add(Box.createVerticalStrut(6));
        username.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        p.add(username);
        p.add(Box.createVerticalStrut(16));
        p.add(label("密码"));
        p.add(Box.createVerticalStrut(6));
        password.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        p.add(password);
        p.add(Box.createVerticalStrut(24));
        loginButton = Ui.primary("登录");
        loginButton.setAlignmentX(LEFT_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        loginButton.addActionListener(e -> login());
        p.add(loginButton);
        p.add(Box.createVerticalStrut(12));
        JButton register = new JButton("注册读者账号");
        register.setAlignmentX(LEFT_ALIGNMENT);
        register.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        register.addActionListener(e -> register());
        p.add(register);
        p.add(Box.createVerticalStrut(18));
        JLabel demo = Ui.muted("演示账号：admin / admin123，reader / reader123");
        demo.setFont(demo.getFont().deriveFont(12f));
        demo.setAlignmentX(LEFT_ALIGNMENT);
        p.add(demo);
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
