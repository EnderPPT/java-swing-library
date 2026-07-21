package edu.training.library.ui;

import static org.junit.jupiter.api.Assertions.*;

import com.formdev.flatlaf.FlatLightLaf;
import edu.training.library.model.Models.Ranking;
import edu.training.library.model.Models.Role;
import java.awt.Dimension;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class UiDesignTest {
    @BeforeAll
    static void installTheme() {
        FlatLightLaf.setup();
        Ui.installDefaults();
    }

    @Test
    void primaryButtonUsesStableCatalogDimensions() {
        JButton button = Ui.primary("借阅");

        assertEquals(38, button.getPreferredSize().height);
        assertEquals(Ui.INK, button.getBackground());
        assertFalse(button.isFocusPainted());
    }

    @Test
    void tablesUseDensePaperCatalogStyle() {
        JTable[] holder = new JTable[1];

        Ui.table(Ui.model("书名", "状态"), holder);

        assertEquals(44, holder[0].getRowHeight());
        assertFalse(holder[0].getShowHorizontalLines());
        assertEquals(Ui.PAPER, holder[0].getBackground());
        assertEquals(JTable.AUTO_RESIZE_OFF, holder[0].getAutoResizeMode());
        assertTrue(holder[0].getColumnModel().getColumn(0).getPreferredWidth() >= 90);
        assertTrue(holder[0].getColumnModel().getColumn(0).getMinWidth() >= 1);
    }

    @Test
    void navigationButtonReflectsSelectedState() {
        JToggleButton button = Ui.navigation("总览", "▦");

        button.setSelected(true);
        assertEquals(Ui.PAPER_DARK, button.getBackground());
        button.setSelected(false);
        assertEquals(Ui.SIDEBAR, button.getBackground());
    }

    @Test
    void chineseCatalogLabelsUseAFontThatCanDisplayThem() {
        var label = Ui.eyebrow("馆藏总量");

        assertEquals(-1, label.getFont().canDisplayUpTo("馆藏总量"));
        assertTrue(label.getInsets().right >= 4);
    }

    @Test
    void rankingChartExposesItsDataToAssistiveTechnology() {
        RankingChart chart = new RankingChart();
        chart.setRows(List.of(new Ranking("Java 编程思想", "Bruce Eckel", 12)));

        assertEquals(new Dimension(480, 280), chart.getPreferredSize());
        assertEquals("热门图书排行，共 1 项", chart.getAccessibleContext().getAccessibleDescription());
    }

    @Test
    void rankingChartDescribesZeroBorrowingAsAnEmptyState() {
        RankingChart chart = new RankingChart();
        chart.setRows(List.of(new Ranking("Java 编程思想", "Bruce Eckel", 0)));

        assertEquals("热门图书排行，暂无数据", chart.getAccessibleContext().getAccessibleDescription());
    }

    @Test
    void monthlyChartUsesItsOwnAccessibleDescription() {
        RankingChart chart = new RankingChart("月借阅统计图", "月借阅统计", "暂无月借阅数据");
        chart.setRows(List.of(new Ranking("07月", "", 6)));

        assertEquals("月借阅统计图", chart.getAccessibleContext().getAccessibleName());
        assertEquals("月借阅统计，共 1 项", chart.getAccessibleContext().getAccessibleDescription());
    }

    @Test
    void navigationKeepsRoleSpecificDestinations() {
        List<String> administratorLabels =
                LibraryNavigation.forRole(Role.ADMIN).stream()
                        .map(LibraryNavigation.Item::label)
                        .toList();
        List<String> readerLabels =
                LibraryNavigation.forRole(Role.READER).stream()
                        .map(LibraryNavigation.Item::label)
                        .toList();

        assertTrue(administratorLabels.contains("读者管理"));
        assertTrue(administratorLabels.contains("借还管理"));
        assertFalse(readerLabels.contains("读者管理"));
        assertTrue(readerLabels.contains("我的借阅"));
        assertTrue(
                LibraryNavigation.forRole(Role.ADMIN).stream()
                        .allMatch(item -> Ui.bodyFont(0, 14f).canDisplayUpTo(item.glyph()) == -1));
    }
}
