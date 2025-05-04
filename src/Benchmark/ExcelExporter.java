package Benchmark;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.*;

import java.io.FileOutputStream;
import java.util.List;

public class ExcelExporter {

    public static void export(List<BenchmarkResult> results, String filePath) throws Exception {
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Benchmark");

        String[] headers = {"Implementation", "Elapsed(ms)", "CPU(ms)", "Memory(MB)", "GC Count", "GC Time(ms)"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        for (int i = 0; i < results.size(); i++) {
            BenchmarkResult r = results.get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(r.name);
            row.createCell(1).setCellValue(r.elapsedTime);
            row.createCell(2).setCellValue(r.cpuTime);
            row.createCell(3).setCellValue(r.memoryUsed);
            row.createCell(4).setCellValue(r.gcCount);
            row.createCell(5).setCellValue(r.gcTime);
        }

        createChart(sheet, results.size(), 1, "Elapsed(ms)", "Elapsed Time Chart");
        createChart(sheet, results.size(), 2, "CPU(ms)", "CPU Time Chart");
        createChart(sheet, results.size(), 3, "Memory(MB)", "Memory Usage Chart");

        try (FileOutputStream out = new FileOutputStream(filePath)) {
            workbook.write(out);
        }
        workbook.close();
        System.out.println("✔ Excel file with charts generated at: " + filePath);
    }

    private static void createChart(XSSFSheet sheet, int dataSize, int colIndex, String yAxisLabel, String title) {
        Drawing<?> drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = new XSSFClientAnchor(0, 0, 0, 0, 8, 1 + (colIndex - 1) * 15, 20, 15 + (colIndex - 1) * 15);
        XSSFChart chart = ((XSSFDrawing) drawing).createChart(anchor);
        chart.setTitleText(title);
        chart.setTitleOverlay(false);

        XDDFChartLegend legend = chart.getOrAddLegend();
        legend.setPosition(LegendPosition.BOTTOM);

        XDDFDataSource<String> categories = XDDFDataSourcesFactory.fromStringCellRange(sheet,
                new CellRangeAddress(1, dataSize, 0, 0));
        XDDFNumericalDataSource<Double> values = XDDFDataSourcesFactory.fromNumericCellRange(sheet,
                new CellRangeAddress(1, dataSize, colIndex, colIndex));

        XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        bottomAxis.setTitle("Implementation");
        XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
        leftAxis.setTitle(yAxisLabel);

        XDDFChartData data = chart.createData(ChartTypes.BAR, bottomAxis, leftAxis);
        XDDFChartData.Series series = data.addSeries(categories, values);
        series.setTitle(yAxisLabel, null);
        chart.plot(data);
    }
}
