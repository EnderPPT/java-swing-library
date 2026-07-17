package edu.training.library.service;

import edu.training.library.model.Models.CategoryStock;
import edu.training.library.model.Models.Dashboard;
import edu.training.library.model.Models.MonthlyStat;
import edu.training.library.model.Models.Ranking;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public final class StatisticsExport {
    private StatisticsExport() {}

    public static void write(
            Path file,
            Dashboard dashboard,
            List<Ranking> rankings,
            List<MonthlyStat> monthly,
            List<CategoryStock> stocks)
            throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle header = headerStyle(workbook);

            Sheet overview = workbook.createSheet("统计总览");
            fill(
                    overview,
                    header,
                    new String[] {"指标", "数值"},
                    List.of(
                            new Object[] {"图书品种", dashboard.bookKinds()},
                            new Object[] {"馆藏总量", dashboard.totalCopies()},
                            new Object[] {"当前可借", dashboard.availableCopies()},
                            new Object[] {"在借记录", dashboard.activeLoans()},
                            new Object[] {"有效预约", dashboard.waitingReservations()},
                            new Object[] {"未缴罚款(元)", dashboard.unpaidFines().toPlainString()}));

            fill(
                    workbook.createSheet("热门排行"),
                    header,
                    new String[] {"排名", "书名", "作者", "累计借阅次数"},
                    indexed(
                            rankings,
                            (i, r) ->
                                    new Object[] {i + 1, r.title(), r.author(), r.borrowCount()}));

            fill(
                    workbook.createSheet("月借阅统计"),
                    header,
                    new String[] {"月份", "借阅量"},
                    indexed(monthly, (i, m) -> new Object[] {m.month(), m.borrowCount()}));

            fill(
                    workbook.createSheet("分类库存"),
                    header,
                    new String[] {"分类", "品种", "馆藏总量", "可借数量", "在借数量"},
                    indexed(
                            stocks,
                            (i, s) ->
                                    new Object[] {
                                        s.category(),
                                        s.bookKinds(),
                                        s.totalCopies(),
                                        s.availableCopies(),
                                        s.totalCopies() - s.availableCopies()
                                    }));

            try (OutputStream out = Files.newOutputStream(file)) {
                workbook.write(out);
            }
        }
    }

    private interface RowMapper<T> {
        Object[] map(int index, T value);
    }

    private static <T> List<Object[]> indexed(List<T> values, RowMapper<T> mapper) {
        return java.util.stream.IntStream.range(0, values.size())
                .mapToObj(i -> mapper.map(i, values.get(i)))
                .toList();
    }

    private static CellStyle headerStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        XSSFFont font = (XSSFFont) workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private static void fill(
            Sheet sheet, CellStyle headerStyle, String[] columns, List<Object[]> rows) {
        Row head = sheet.createRow(0);
        for (int i = 0; i < columns.length; i++) {
            Cell cell = head.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }
        for (int r = 0; r < rows.size(); r++) {
            Row row = sheet.createRow(r + 1);
            Object[] values = rows.get(r);
            for (int i = 0; i < values.length; i++) {
                Cell cell = row.createCell(i);
                if (values[i] instanceof Number number) cell.setCellValue(number.doubleValue());
                else cell.setCellValue(String.valueOf(values[i]));
            }
        }
        for (int i = 0; i < columns.length; i++) sheet.setColumnWidth(i, 22 * 256);
    }
}
