package Benchmark;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.xddf.usermodel.chart.*;
import java.io.FileOutputStream;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.stream.Collectors;

import SequentialSolution.SequentialSolution;
import ForkJoinFrameworkSolution.ForkJoinFrameworkSolution;
import MultithreadedSolutionWithoutThreadPools.MultithreadedSolutionWithoutThreadPools;
import MultithreadedSolutionWithThreadPools.MultithreadedSolutionWithThreadPools;
import CompletableFuturesBasedSolution.CompletableFuturesBasedSolution;

public class BenchmarkRunner {

    public static void main(String[] args) {
        String fileName = "WikiDumps/large_wiki_file.xml";

        int[] maxPagesArray = {10000, 25000, 50000, 75000, 100000}; //adicionar consoante necessário. TODO: Verificar que intervalo de amostras colocar
        int[] threadCounts = {2, 4, 8, 12, 16}; //TODO: Verificar se se deve adicionar mais valores

        List<BenchmarkResult> allResults = new ArrayList<>();

        try {
            for (int maxPages : maxPagesArray) {
                for (int choice = 1; choice <= 5; choice++) {
                    if (choice == 1) {
                        allResults.add(runBenchmark(choice, maxPages, fileName, 1));
                    } else {
                        for (int threadNum : threadCounts) {
                            allResults.add(runBenchmark(choice, maxPages, fileName, threadNum));
                        }
                    }
                }
            }

            saveResultsToExcel(allResults);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static BenchmarkResult runBenchmark(int choice, int maxPages, String fileName, int threadNumber) throws Exception {
        Runtime runtime = Runtime.getRuntime();
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

        long gcCountBefore = getGcCount(gcBeans);
        long gcTimeBefore = getGcTime(gcBeans);
        System.gc();
        long memBefore = runtime.totalMemory() - runtime.freeMemory();
        long wallClockBefore = System.nanoTime();

        switch (choice) {
            case 1 -> SequentialSolution.run(maxPages, fileName);
            case 2 -> MultithreadedSolutionWithThreadPools.run(maxPages, fileName, threadNumber);
            case 3 -> ForkJoinFrameworkSolution.run(maxPages, fileName, threadNumber);
            case 4 -> MultithreadedSolutionWithoutThreadPools.run(maxPages, fileName, threadNumber);
            case 5 -> CompletableFuturesBasedSolution.run(maxPages,fileName, threadNumber);
            default -> throw new IllegalArgumentException("Opção inválida: " + choice);
        }

        long wallClockAfter = System.nanoTime();
        System.gc();
        long memAfter = runtime.totalMemory() - runtime.freeMemory();
        long gcCountAfter = getGcCount(gcBeans);
        long gcTimeAfter = getGcTime(gcBeans);

        return new BenchmarkResult(
                getImplName(choice),
                maxPages,
                threadNumber,
                (wallClockAfter - wallClockBefore) / 1_000_000.0,
                (memAfter - memBefore) / (1024.0 * 1024.0),
                gcCountAfter - gcCountBefore,
                gcTimeAfter - gcTimeBefore
        );
    }

    private static long getGcCount(List<GarbageCollectorMXBean> beans) {
        return beans.stream().mapToLong(GarbageCollectorMXBean::getCollectionCount).sum();
    }

    private static long getGcTime(List<GarbageCollectorMXBean> beans) {
        return beans.stream().mapToLong(GarbageCollectorMXBean::getCollectionTime).sum();
    }

    private static String getImplName(int choice) {
        return switch (choice) {
            case 1 -> "Sequencial";
            case 2 -> "ThreadPool";
            case 3 -> "ForkJoin";
            case 4 -> "MultithreadedManual";
            case 5 -> "CompletableFuturesBasedSolution";
            default -> "Desconhecido";
        };
    }

    private static void saveResultsToExcel(List<BenchmarkResult> results) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Resultados");

            String[] headers = {"Implementação", "maxPages", "Threads", "Tempo (ms)", "Memória (MB)", "GC Count", "GC Time (ms)"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            int rowIdx = 1;
            for (BenchmarkResult result : results) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(result.name());
                row.createCell(1).setCellValue(result.maxPages());
                row.createCell(2).setCellValue(result.threads());
                row.createCell(3).setCellValue(result.elapsed());
                row.createCell(4).setCellValue(result.memory());
                row.createCell(5).setCellValue(result.gcCount());
                row.createCell(6).setCellValue(result.gcTime());
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            // Agrupar resultados por maxPages
            Map<Integer, List<BenchmarkResult>> groupedByMaxPages = results.stream()
                    .collect(Collectors.groupingBy(BenchmarkResult::maxPages));

            int chartOffset = rowIdx + 2;

            // Criar gráfico para cada grupo
            for (Map.Entry<Integer, List<BenchmarkResult>> entry : groupedByMaxPages.entrySet()) {
                createChart(sheet, workbook, entry.getValue(), chartOffset, entry.getKey());
                chartOffset += 20; // espaço entre gráficos
            }

            try (FileOutputStream out = new FileOutputStream("benchmark_summary.xlsx")) {
                workbook.write(out);
                System.out.println("✅ Resultados e gráficos salvos em benchmark_summary.xlsx");
            }
        } catch (Exception e) {
            System.err.println("Erro ao gerar Excel: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private static void createChart(XSSFSheet sheet, XSSFWorkbook workbook, List<BenchmarkResult> filteredResults, int anchorRow, int maxPages) {
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 0, anchorRow, 10, anchorRow + 15);
        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText("Tempo por Implementação - maxPages = " + maxPages);
        chart.setTitleOverlay(false);

        XDDFChartLegend legend = chart.getOrAddLegend();
        legend.setPosition(LegendPosition.RIGHT);

        XDDFCategoryAxis xAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        xAxis.setTitle("Implementação + Threads");

        XDDFValueAxis yAxis = chart.createValueAxis(AxisPosition.LEFT);
        yAxis.setTitle("Tempo (ms)");

        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();

        for (BenchmarkResult r : filteredResults) {
            labels.add(r.name() + " - T" + r.threads());
            values.add(r.elapsed());
        }

        XDDFCategoryDataSource categories = XDDFDataSourcesFactory.fromArray(labels.toArray(new String[0]));
        XDDFNumericalDataSource<Double> data = XDDFDataSourcesFactory.fromArray(values.toArray(new Double[0]));

        XDDFBarChartData chartData = (XDDFBarChartData) chart.createData(ChartTypes.BAR, xAxis, yAxis);
        XDDFBarChartData.Series series = (XDDFBarChartData.Series) chartData.addSeries(categories, data);
        series.setTitle("Tempo de Execução", null);
        chart.plot(chartData);
    }


    public record BenchmarkResult(String name, int maxPages, int threads, double elapsed, double memory, long gcCount, long gcTime) {}
}
