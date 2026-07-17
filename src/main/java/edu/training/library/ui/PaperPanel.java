package edu.training.library.ui;

import java.awt.*;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

final class PaperPanel extends JPanel {
    private final Color accent;

    PaperPanel(LayoutManager layout, Color accent) {
        super(layout);
        this.accent = accent;
        setOpaque(false);
        setBackground(Ui.PAPER);
        setBorder(new EmptyBorder(18, 18, 18, 18));
    }

    void setPadding(int top, int left, int bottom, int right) {
        setBorder(new EmptyBorder(top, left, bottom, right));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        int width = getWidth();
        int height = getHeight();
        if (width <= 1 || height <= 1) return;

        Graphics2D copy = (Graphics2D) graphics.create();
        copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        copy.setColor(new Color(23, 30, 35, 18));
        copy.fillRoundRect(4, 5, width - 5, height - 6, 4, 4);

        Polygon paper =
                new Polygon(
                        new int[] {1, width - 13, width - 1, width - 1, 1},
                        new int[] {1, 1, 13, height - 1, height - 1},
                        5);
        copy.setColor(Ui.PAPER);
        copy.fillPolygon(paper);
        copy.setColor(Ui.OUTLINE_SOFT);
        copy.drawPolygon(paper);
        copy.setColor(accent);
        copy.fillRect(1, 1, Math.max(0, width - 14), 3);
        copy.setColor(Ui.PAPER_DARK);
        copy.fillPolygon(new int[] {width - 13, width - 1, width - 13}, new int[] {1, 13, 13}, 3);
        copy.setColor(Ui.OUTLINE_SOFT);
        copy.drawLine(width - 13, 1, width - 13, 13);
        copy.drawLine(width - 13, 13, width - 1, 13);
        copy.dispose();
        super.paintComponent(graphics);
    }
}
