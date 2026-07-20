package edu.training.library.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.training.library.model.Models.CategoryStock;
import edu.training.library.model.Models.Dashboard;
import edu.training.library.model.Models.MonthlyStat;
import edu.training.library.model.Models.Ranking;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StatisticsExportTest {
    @TempDir Path directory;

    @Test
    void exportsFourReadableWorksheets() throws Exception {
        Path file = directory.resolve("统计报表.xlsx");
        StatisticsExport.write(
                file,
                new Dashboard(5, 20, 12, 8, 2, new BigDecimal("3.50")),
                List.of(new Ranking("三体", "刘慈欣", 9)),
                List.of(new MonthlyStat("2026-07", 6)),
                List.of(new CategoryStock("科幻", 3, 10, 7)));

        assertTrue(Files.size(file) > 0);
        try (var input = Files.newInputStream(file);
                var workbook = new XSSFWorkbook(input)) {
            assertEquals(4, workbook.getNumberOfSheets());
            assertEquals("统计总览", workbook.getSheetName(0));
            assertEquals("热门排行", workbook.getSheetName(1));
            assertEquals("月借阅统计", workbook.getSheetName(2));
            assertEquals("分类库存", workbook.getSheetName(3));
            assertEquals("三体", workbook.getSheet("热门排行").getRow(1).getCell(1).getStringCellValue());
            assertEquals(6, workbook.getSheet("月借阅统计").getRow(1).getCell(1).getNumericCellValue());
        }
    }
}
