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

            // Estilos
            CellStyle headerStyle = workbook.createCellStyle();
            XSSFFont boldFont = workbook.createFont();
            boldFont.setBold(true);
            headerStyle.setFont(boldFont);

            // Verde claro (melhor por algoritmo)
            CellStyle lightGreenStyle = workbook.createCellStyle();
            lightGreenStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            lightGreenStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Verde escuro (melhor por grupo de maxPages)
            CellStyle darkGreenStyle = workbook.createCellStyle();
            darkGreenStyle.setFillForegroundColor(IndexedColors.BRIGHT_GREEN.getIndex());
            darkGreenStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Ordenar os resultados por maxPages (crescente)
            results.sort(Comparator.comparingInt(BenchmarkResult::maxPages));

            // Agrupar por maxPages
            Map<Integer, List<BenchmarkResult>> groupedByMaxPages = results.stream()
                    .collect(Collectors.groupingBy(BenchmarkResult::maxPages, TreeMap::new, Collectors.toList()));

            // Melhor tempo por algoritmo e maxPages (considerando threads)
            Map<String, Map<Integer, Double>> bestByAlgorithmAndMaxPages = results.stream()
                    .collect(Collectors.groupingBy(
                            BenchmarkResult::name,
                            Collectors.groupingBy(
                                    BenchmarkResult::maxPages,
                                    Collectors.collectingAndThen(
                                            Collectors.minBy(Comparator.comparingDouble(BenchmarkResult::elapsed)),
                                            opt -> Math.round(opt.get().elapsed() * 100.0) / 100.0
                                    )
                            )
                    ));

            int rowIdx = 0;
            int chartOffset = 0;

            // Processar os resultados agrupados por maxPages em ordem crescente
            for (Map.Entry<Integer, List<BenchmarkResult>> entry : groupedByMaxPages.entrySet()) {
                int currentMaxPages = entry.getKey();
                List<BenchmarkResult> group = entry.getValue();

                // Melhor tempo dentro deste grupo (maxPages)
                double bestInGroup = group.stream()
                        .mapToDouble(BenchmarkResult::elapsed)
                        .min()
                        .orElse(Double.MAX_VALUE);
                bestInGroup = Math.round(bestInGroup * 100.0) / 100.0;

                Row titleRow = sheet.createRow(rowIdx++);
                titleRow.createCell(0).setCellValue("Resultados para maxPages = " + currentMaxPages);

                Row headerRow = sheet.createRow(rowIdx++);
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                    cell.setCellStyle(headerStyle);
                }

                for (BenchmarkResult result : group) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(result.name());
                    row.createCell(1).setCellValue(result.maxPages());
                    row.createCell(2).setCellValue(result.threads());

                    double roundedElapsed = Math.round(result.elapsed() * 100.0) / 100.0;
                    Cell timeCell = row.createCell(3);
                    timeCell.setCellValue(roundedElapsed);

                    boolean isBestInGroup = (roundedElapsed == bestInGroup);
                    boolean isBestInAlgorithm = (roundedElapsed == bestByAlgorithmAndMaxPages.get(result.name()).get(result.maxPages()));

                    if (isBestInGroup) {
                        timeCell.setCellStyle(darkGreenStyle); // Melhor tempo deste grupo
                    } else if (isBestInAlgorithm) {
                        timeCell.setCellStyle(lightGreenStyle); // Melhor tempo por algoritmo
                    }

                    row.createCell(4).setCellValue(result.memory());
                    row.createCell(5).setCellValue(result.gcCount());
                    row.createCell(6).setCellValue(result.gcTime());
                }

                for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

                chartOffset = rowIdx + 1;
                createChart(sheet, workbook, group, chartOffset, currentMaxPages);
                rowIdx = chartOffset + 18;
            }

            try (FileOutputStream out = new FileOutputStream("benchmark_summary.xlsx")) {
                workbook.write(out);
                System.out.println("Resultados e gráficos salvos em benchmark_summary.xlsx");
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
