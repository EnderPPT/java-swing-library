package edu.training.library.ui;

import java.awt.*;
import javax.swing.JPanel;

final class GridPanel extends JPanel {
    private static final int GRID_SIZE = 40;

    GridPanel() {
        this(new FlowLayout());
    }

    GridPanel(LayoutManager layout) {
        super(layout);
        setOpaque(true);
        setBackground(Ui.BACKGROUND);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D copy = (Graphics2D) graphics.create();
        copy.setColor(new Color(23, 30, 35, 12));
        copy.setStroke(new BasicStroke(1f));
        for (int x = 0; x < getWidth(); x += GRID_SIZE) copy.drawLine(x, 0, x, getHeight());
        for (int y = 0; y < getHeight(); y += GRID_SIZE) copy.drawLine(0, y, getWidth(), y);
        copy.dispose();
    }
}
