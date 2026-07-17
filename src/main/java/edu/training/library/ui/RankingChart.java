package edu.training.library.ui;

import edu.training.library.model.Models.Ranking;
import java.awt.*;
import java.util.List;
import javax.accessibility.AccessibleContext;
import javax.swing.JComponent;

final class RankingChart extends JComponent {
    private List<Ranking> rows = List.of();

    RankingChart() {
        setOpaque(false);
        setPreferredSize(new Dimension(480, 280));
        getAccessibleContext().setAccessibleName("热门图书借阅排行图");
        updateAccessibleDescription();
    }

    void setRows(List<Ranking> rankings) {
        rows = List.copyOf(rankings.stream().limit(7).toList());
        updateAccessibleDescription();
        repaint();
    }

    private void updateAccessibleDescription() {
        String description =
                rows.stream().anyMatch(row -> row.borrowCount() > 0)
                        ? "热门图书排行，共 " + rows.size() + " 项"
                        : "热门图书排行，暂无借阅数据";
        getAccessibleContext().setAccessibleDescription(description);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D copy = (Graphics2D) graphics.create();
        copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int left = 40;
        int right = 18;
        int top = 24;
        int bottom = 50;
        int width = Math.max(1, getWidth() - left - right);
        int height = Math.max(1, getHeight() - top - bottom);

        copy.setColor(new Color(116, 119, 123, 55));
        for (int line = 0; line <= 4; line++) {
            int y = top + height * line / 4;
            copy.drawLine(left, y, left + width, y);
        }

        if (rows.isEmpty()) {
            copy.setColor(Ui.MUTED);
            copy.setFont(Ui.bodyFont(Font.PLAIN, 13f));
            copy.drawString("暂无借阅排行", left + 12, top + height / 2);
            copy.dispose();
            return;
        }

        long maximum = rows.stream().mapToLong(Ranking::borrowCount).max().orElse(1);
        if (maximum <= 0) {
            copy.setColor(Ui.MUTED);
            copy.setFont(Ui.bodyFont(Font.PLAIN, 13f));
            copy.drawString("暂无借阅数据，完成借阅后生成排行", left + 12, top + height / 2);
            copy.dispose();
            return;
        }
        int slot = Math.max(1, width / rows.size());
        int barWidth = Math.max(18, Math.min(52, slot - 16));
        // 柱高按 height-18 缩放，给最高柱的数值标签留出空间
        int barSpace = Math.max(1, height - 18);
        for (int index = 0; index < rows.size(); index++) {
            Ranking ranking = rows.get(index);
            int barHeight = (int) Math.round((double) ranking.borrowCount() / maximum * barSpace);
            int x = left + index * slot + (slot - barWidth) / 2;
            int y = top + height - barHeight;
            copy.setColor(index == 0 ? Ui.INK : Ui.PAPER_DARK);
            copy.fillRect(x, y, barWidth, barHeight);
            if (barHeight > 0) {
                copy.setColor(Ui.OUTLINE);
                copy.drawRect(x, y, barWidth, barHeight);
            }
            copy.setColor(Ui.INK);
            copy.setFont(Ui.monoFont(Font.BOLD, 11f));
            String value = String.valueOf(ranking.borrowCount());
            int valueWidth = copy.getFontMetrics().stringWidth(value);
            copy.drawString(value, x + (barWidth - valueWidth) / 2, y - 6);
            copy.setFont(Ui.bodyFont(Font.PLAIN, 11f));
            String label = fit(copy.getFontMetrics(), ranking.title(), slot - 6);
            int labelWidth = copy.getFontMetrics().stringWidth(label);
            copy.drawString(label, x + (barWidth - labelWidth) / 2, top + height + 22);
        }
        copy.dispose();
    }

    private static String fit(FontMetrics metrics, String text, int available) {
        if (metrics.stringWidth(text) <= available) return text;
        String shortened = text;
        while (shortened.length() > 1 && metrics.stringWidth(shortened + "…") > available) {
            shortened = shortened.substring(0, shortened.length() - 1);
        }
        return shortened + "…";
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) accessibleContext = new AccessibleRankingChart();
        return accessibleContext;
    }

    protected final class AccessibleRankingChart extends AccessibleJComponent {}
}
