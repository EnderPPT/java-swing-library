package edu.training.library.ui;

import java.awt.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public final class Ui {
    public static final Color BACKGROUND = new Color(255, 248, 240);
    public static final Color PAPER = new Color(253, 243, 223);
    public static final Color PAPER_DARK = new Color(235, 225, 207);
    public static final Color SIDEBAR = new Color(255, 247, 229);
    public static final Color INK = new Color(23, 30, 35);
    public static final Color MUTED = new Color(100, 101, 99);
    public static final Color OUTLINE = new Color(116, 119, 123);
    public static final Color OUTLINE_SOFT = new Color(196, 199, 202);
    public static final Color ACCENT = new Color(153, 69, 52);
    public static final Color ERROR = new Color(186, 26, 26);
    public static final Color SUCCESS = new Color(45, 100, 78);

    static final Color PRIMARY = INK;
    static final Color SURFACE = BACKGROUND;

    private static final Set<String> FONTS = availableFonts();
    private static final String BODY_FAMILY =
            firstAvailable("Microsoft YaHei UI", "Microsoft YaHei", "Noto Sans CJK SC", "Dialog");
    private static final String SERIF_FAMILY =
            firstAvailable("Noto Serif SC", "Source Han Serif SC", "SimSun", "Serif");
    private static final String MONO_FAMILY =
            firstAvailable("Courier Prime", "Cascadia Mono", "Consolas", "Monospaced");

    private Ui() {}

    public static void installDefaults() {
        UIManager.put("defaultFont", bodyFont(Font.PLAIN, 14f));
        UIManager.put("Component.arc", 3);
        UIManager.put("Button.arc", 3);
        UIManager.put("TextComponent.arc", 3);
        UIManager.put("ScrollBar.width", 11);
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("Label.foreground", INK);
        UIManager.put("TextField.background", PAPER);
        UIManager.put("PasswordField.background", PAPER);
        UIManager.put("ComboBox.background", PAPER);
        UIManager.put("Table.background", PAPER);
        UIManager.put("Table.foreground", INK);
        UIManager.put("Table.selectionBackground", new Color(225, 216, 197));
        UIManager.put("Table.selectionForeground", INK);
        UIManager.put("TableHeader.background", PAPER_DARK);
        UIManager.put("TableHeader.foreground", INK);
        UIManager.put("OptionPane.background", BACKGROUND);
        UIManager.put("OptionPane.messageForeground", INK);
    }

    static Font bodyFont(int style, float size) {
        return new Font(BODY_FAMILY, style, Math.round(size));
    }

    static Font serifFont(int style, float size) {
        return new Font(SERIF_FAMILY, style, Math.round(size));
    }

    static Font monoFont(int style, float size) {
        return new Font(MONO_FAMILY, style, Math.round(size));
    }

    static JPanel panel(LayoutManager layout) {
        GridPanel panel = new GridPanel(layout);
        panel.setBorder(new EmptyBorder(22, 24, 24, 24));
        return panel;
    }

    static PaperPanel paper(LayoutManager layout) {
        return new PaperPanel(layout, OUTLINE);
    }

    static PaperPanel paper(LayoutManager layout, Color accent) {
        return new PaperPanel(layout, accent);
    }

    static JLabel title(String text) {
        JLabel label = new JLabel(text);
        label.setFont(serifFont(Font.BOLD, 24f));
        label.setForeground(INK);
        return label;
    }

    static JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(serifFont(Font.BOLD, 18f));
        label.setForeground(INK);
        return label;
    }

    static JLabel eyebrow(String text) {
        JLabel label = new JLabel(text.toUpperCase(Locale.ROOT));
        label.setFont(containsCjk(text) ? bodyFont(Font.BOLD, 12f) : monoFont(Font.BOLD, 12f));
        label.setForeground(MUTED);
        label.setBorder(new EmptyBorder(0, 0, 0, 6));
        return label;
    }

    static JLabel muted(String text) {
        JLabel label = new JLabel(text);
        label.setFont(bodyFont(Font.PLAIN, 13f));
        label.setForeground(MUTED);
        label.setBorder(new EmptyBorder(0, 0, 0, 6));
        return label;
    }

    static JButton primary(String text) {
        JButton button = baseButton(text);
        button.setBackground(INK);
        button.setForeground(Color.WHITE);
        button.setBorder(new EmptyBorder(0, 18, 0, 18));
        return button;
    }

    static JButton secondary(String text) {
        JButton button = baseButton(text);
        button.setBackground(PAPER);
        button.setForeground(INK);
        button.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(OUTLINE_SOFT),
                        new EmptyBorder(0, 15, 0, 15)));
        return button;
    }

    static JButton danger(String text) {
        JButton button = secondary(text);
        button.setForeground(ERROR);
        button.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(222, 159, 149)),
                        new EmptyBorder(0, 15, 0, 15)));
        return button;
    }

    static JToggleButton navigation(String text, String glyph) {
        JToggleButton button = new JToggleButton(glyph + "   " + text);
        button.setFont(bodyFont(Font.PLAIN, 15f));
        button.setForeground(INK);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFocusPainted(false);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(188, 48));
        button.setMinimumSize(new Dimension(120, 48));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        button.addItemListener(event -> applyNavigationState(button));
        applyNavigationState(button);
        return button;
    }

    private static JButton baseButton(String text) {
        JButton button = new JButton(text);
        button.setFont(bodyFont(Font.BOLD, 13f));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty("JButton.buttonType", "square");
        Dimension measured = button.getPreferredSize();
        int width = Math.max(72, measured.width + 24);
        button.setPreferredSize(new Dimension(width, 38));
        button.setMinimumSize(new Dimension(64, 38));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        return button;
    }

    private static void applyNavigationState(JToggleButton button) {
        boolean selected = button.isSelected();
        button.setBackground(selected ? PAPER_DARK : SIDEBAR);
        button.setFont(bodyFont(selected ? Font.BOLD : Font.PLAIN, 15f));
        button.setBorder(
                BorderFactory.createCompoundBorder(
                        new MatteBorder(0, selected ? 4 : 0, 0, 0, INK),
                        new EmptyBorder(0, selected ? 14 : 18, 0, 10)));
    }

    static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    static JScrollPane table(DefaultTableModel model, JTable[] holder) {
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setFillsViewportHeight(true);
        table.setRowHeight(44);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setBackground(PAPER);
        table.setForeground(INK);
        table.setSelectionBackground(new Color(225, 216, 197));
        table.setSelectionForeground(INK);
        table.setFont(bodyFont(Font.PLAIN, 13f));
        table.setDefaultRenderer(Object.class, new CatalogCellRenderer());
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(1, 42));
        table.getTableHeader().setDefaultRenderer(new CatalogHeaderRenderer());
        for (int column = 0; column < model.getColumnCount(); column++) {
            String name = model.getColumnName(column);
            table.getColumnModel().getColumn(column).setPreferredWidth(columnWidth(name));
            if (name.contains("状态") || name.contains("提醒")) {
                table.getColumnModel().getColumn(column).setCellRenderer(new StatusRenderer());
            }
        }
        holder[0] = table;

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(OUTLINE_SOFT));
        scroll.getViewport().setBackground(PAPER);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        return scroll;
    }

    private static int columnWidth(String name) {
        if (name.contains("时间") || name.contains("日期")) return 148;
        if (name.contains("书名") || name.contains("原因")) return 170;
        if (name.contains("出版社")) return 150;
        if (name.contains("邮箱")) return 190;
        if (name.contains("作者") || name.contains("姓名")) return 150;
        if (name.contains("手机")) return 130;
        if (name.contains("ISBN")) return 128;
        if (name.contains("状态") || name.contains("提醒")) return 110;
        return Math.max(90, name.length() * 16 + 28);
    }

    static JPanel toolbar(Component... items) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        panel.setOpaque(false);
        for (Component item : items) panel.add(item);
        return panel;
    }

    static JPanel form(String[] labels, JComponent[] fields) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(7, 6, 7, 6);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        for (int index = 0; index < labels.length; index++) {
            constraints.gridx = 0;
            constraints.gridy = index;
            constraints.weightx = 0;
            JLabel label = new JLabel(labels[index]);
            label.setFont(bodyFont(Font.PLAIN, 13f));
            panel.add(label, constraints);
            constraints.gridx = 1;
            constraints.weightx = 1;
            styleField(fields[index]);
            panel.add(fields[index], constraints);
        }
        return panel;
    }

    static void styleField(JComponent field) {
        field.setFont(bodyFont(Font.PLAIN, 14f));
        field.setBackground(PAPER);
        Dimension preferred = field.getPreferredSize();
        field.setPreferredSize(new Dimension(Math.max(120, preferred.width), 38));
        field.setMinimumSize(new Dimension(80, 38));
    }

    static int selected(JTable table) {
        int row = table.getSelectedRow();
        return row < 0 ? -1 : table.convertRowIndexToModel(row);
    }

    static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "提示", JOptionPane.INFORMATION_MESSAGE);
    }

    static void error(Component parent, Exception exception) {
        String message = exception.getMessage();
        if (message == null && exception.getCause() != null) {
            message = exception.getCause().getMessage();
        }
        JOptionPane.showMessageDialog(
                parent, message == null ? "操作失败" : message, "操作失败", JOptionPane.ERROR_MESSAGE);
    }

    static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(
                        parent,
                        message,
                        "确认",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE)
                == JOptionPane.YES_OPTION;
    }

    static JPanel stack(List<? extends Component> rows) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        for (Component row : rows) {
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
            if (row instanceof JComponent component) {
                component.setAlignmentX(Component.LEFT_ALIGNMENT);
            }
            panel.add(row);
            panel.add(Box.createVerticalStrut(10));
        }
        return panel;
    }

    private static Set<String> availableFonts() {
        Set<String> fonts = new HashSet<>();
        Arrays.stream(
                        GraphicsEnvironment.getLocalGraphicsEnvironment()
                                .getAvailableFontFamilyNames())
                .map(name -> name.toLowerCase(Locale.ROOT))
                .forEach(fonts::add);
        return fonts;
    }

    private static String firstAvailable(String... names) {
        return Arrays.stream(names)
                .filter(name -> FONTS.contains(name.toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElse(names[names.length - 1]);
    }

    private static boolean containsCjk(String text) {
        return text.codePoints()
                .anyMatch(
                        codePoint -> {
                            Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);
                            return script == Character.UnicodeScript.HAN;
                        });
    }

    private static class CatalogCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean selected,
                boolean focused,
                int row,
                int column) {
            JLabel label =
                    (JLabel)
                            super.getTableCellRendererComponent(
                                    table, value, selected, focused, row, column);
            label.setBorder(new EmptyBorder(0, 12, 0, 12));
            if (!selected) {
                label.setBackground(row % 2 == 0 ? PAPER : new Color(246, 237, 219));
                label.setForeground(INK);
            }
            return label;
        }
    }

    private static final class StatusRenderer extends CatalogCellRenderer {
        private boolean danger;

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean selected,
                boolean focused,
                int row,
                int column) {
            JLabel label =
                    (JLabel)
                            super.getTableCellRendererComponent(
                                    table, value, selected, focused, row, column);
            String text = value == null ? "" : value.toString();
            danger =
                    text.contains("逾期")
                            || text.contains("待缴")
                            || text.contains("停用")
                            || text.contains("过期");
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(bodyFont(Font.BOLD, 12f));
            label.setForeground(danger ? Ui.ERROR : INK);
            label.setBorder(new EmptyBorder(0, 4, 0, 4));
            return label;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            String text = getText();
            if (text != null && !text.isBlank() && !"—".equals(text) && !"-".equals(text)) {
                FontMetrics metrics = getFontMetrics(getFont());
                int width = Math.min(getWidth() - 4, metrics.stringWidth(text) + 18);
                int height = metrics.getHeight() + 8;
                int x = (getWidth() - width) / 2;
                int y = (getHeight() - height) / 2;
                graphics.setColor(danger ? Ui.ERROR : OUTLINE);
                graphics.drawRect(x, y, width - 1, height - 1);
            }
        }
    }

    private static final class CatalogHeaderRenderer extends DefaultTableCellRenderer {
        private CatalogHeaderRenderer() {
            setOpaque(true);
            setBackground(PAPER_DARK);
            setForeground(INK);
            setFont(serifFont(Font.BOLD, 13f));
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(new MatteBorder(0, 0, 2, 0, INK));
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean selected,
                boolean focused,
                int row,
                int column) {
            JLabel label =
                    (JLabel)
                            super.getTableCellRendererComponent(
                                    table, value, selected, focused, row, column);
            label.setBorder(
                    BorderFactory.createCompoundBorder(
                            new MatteBorder(0, 0, 2, 0, INK), new EmptyBorder(0, 12, 0, 12)));
            return label;
        }
    }
}
