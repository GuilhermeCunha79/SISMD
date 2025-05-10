package Benchmark;

import java.io.FileOutputStream;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import CompletableFuturesBasedSolution.CompletableFuturesBasedSolution;
import ForkJoinFrameworkSolution.ForkJoinFrameworkSolution;
import MultithreadedSolutionWithThreadPools.MultithreadedSolutionWithThreadPools;
import MultithreadedSolutionWithoutThreadPools.MultithreadedSolutionWithoutThreadPools;
import SequentialSolution.SequentialSolution;
import com.sun.management.OperatingSystemMXBean;
import org.apache.poi.xddf.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xddf.usermodel.chart.*;

public class BenchmarkRunner {

    public static void main(String[] args) {
        String fileName = "WikiDumps/enwiki-20250420-pages-meta-current1.xml-p1p41242";

        int[] maxPagesArray = {500, 5000, 10000, 25000}; //adicionar consoante necessário. TODO: Verificar que intervalo de amostras colocar
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
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        long gcCountBefore = gcBeans.stream().mapToLong(GarbageCollectorMXBean::getCollectionCount).sum();
        long gcTimeBefore  = gcBeans.stream().mapToLong(GarbageCollectorMXBean::getCollectionTime).sum();
        System.gc();
        long memBefore     = runtime.totalMemory() - runtime.freeMemory();
        long timeBefore    = System.nanoTime();
        double cpuBefore   = osBean.getProcessCpuLoad();

        // Executa a implementação escolhida
        switch (choice) {
            case 1 -> SequentialSolution.run(maxPages, fileName);
            case 2 -> MultithreadedSolutionWithThreadPools.run(maxPages, fileName, threadNumber);
            case 3 -> ForkJoinFrameworkSolution.run(maxPages, fileName, threadNumber);
            case 4 -> MultithreadedSolutionWithoutThreadPools.run(maxPages, fileName, threadNumber);
            case 5 -> CompletableFuturesBasedSolution.run(maxPages, fileName, threadNumber);
            default -> throw new IllegalArgumentException("Opção inválida: " + choice);
        }

        long timeAfter = System.nanoTime();
        System.gc();
        Thread.sleep(100);
        long memAfter = runtime.totalMemory() - runtime.freeMemory();
        long gcCountAfter = gcBeans.stream().mapToLong(GarbageCollectorMXBean::getCollectionCount).sum();
        long gcTimeAfter  = gcBeans.stream().mapToLong(GarbageCollectorMXBean::getCollectionTime).sum();
        double cpuAfter   = osBean.getProcessCpuLoad();

        double elapsedMs = (timeAfter - timeBefore) / 1_000_000.0;
        double memoryMb  = (memAfter - memBefore) / (1024.0 * 1024.0);
        double cpuUsagePct = (cpuAfter - cpuBefore) * 100;

        return new BenchmarkResult(
                getImplName(choice), maxPages, threadNumber,
                Math.round(elapsedMs * 100.0) / 100.0,
                Math.round(memoryMb * 100.0)   / 100.0,
                gcCountAfter - gcCountBefore,
                gcTimeAfter  - gcTimeBefore,
                Math.round(cpuUsagePct * 100.0) / 100.0
        );
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

            // Cabeçalhos
            String[] headers = {"Implementação", "maxPages", "Threads", "Tempo (ms)", "Memória (MB)", "GC Count", "GC Time (ms)", "CPU Usage (%)"};
            CellStyle headerStyle = workbook.createCellStyle();
            XSSFFont boldFont = workbook.createFont(); boldFont.setBold(true);
            headerStyle.setFont(boldFont);

            // Estilos de destaque
            CellStyle lightGreenStyle = workbook.createCellStyle();
            lightGreenStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            lightGreenStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle darkGreenStyle = workbook.createCellStyle();
            darkGreenStyle.setFillForegroundColor(IndexedColors.BRIGHT_GREEN.getIndex());
            darkGreenStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Ordena e agrupa por maxPages
            results.sort(Comparator.comparingInt(BenchmarkResult::maxPages));
            Map<Integer, List<BenchmarkResult>> grouped = results.stream()
                    .collect(Collectors.groupingBy(BenchmarkResult::maxPages, LinkedHashMap::new, Collectors.toList()));

            int rowIdx = 0;
            for (Map.Entry<Integer, List<BenchmarkResult>> entry : grouped.entrySet()) {
                int maxPages = entry.getKey();
                List<BenchmarkResult> group = entry.getValue();

                // Título do grupo
                Row titleRow = sheet.createRow(rowIdx++);
                Cell titleCell = titleRow.createCell(0);
                titleCell.setCellValue("Resultados para maxPages = " + maxPages);
                titleCell.getCellStyle().setFont(boldFont);

                // Cabeçalho da tabela
                Row headerRow = sheet.createRow(rowIdx++);
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                    cell.setCellStyle(headerStyle);
                }

                // Cálculo dos melhores por métrica
                double bestTimeGroup = group.stream().mapToDouble(BenchmarkResult::elapsed).min().orElse(Double.MAX_VALUE);
                double bestMemoryGroup = group.stream().mapToDouble(BenchmarkResult::memory).min().orElse(Double.MAX_VALUE);
                double bestCpuGroup = group.stream().mapToDouble(BenchmarkResult::cpuUsage).min().orElse(Double.MAX_VALUE);
                // Melhores por algoritmo
                Map<String, Double> bestTimePerImpl = group.stream().collect(Collectors.groupingBy(
                        BenchmarkResult::name,
                        Collectors.collectingAndThen(Collectors.minBy(Comparator.comparingDouble(BenchmarkResult::elapsed)), opt -> opt.get().elapsed())
                ));
                Map<String, Double> bestMemoryPerImpl = group.stream().collect(Collectors.groupingBy(
                        BenchmarkResult::name,
                        Collectors.collectingAndThen(Collectors.minBy(Comparator.comparingDouble(BenchmarkResult::memory)), opt -> opt.get().memory())
                ));
                Map<String, Double> bestCpuPerImpl = group.stream().collect(Collectors.groupingBy(
                        BenchmarkResult::name,
                        Collectors.collectingAndThen(Collectors.minBy(Comparator.comparingDouble(BenchmarkResult::cpuUsage)), opt -> opt.get().cpuUsage())
                ));

                // Linhas de dados
                for (BenchmarkResult r : group) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(r.name());
                    row.createCell(1).setCellValue(r.maxPages());
                    row.createCell(2).setCellValue(r.threads());

                    Cell timeCell = row.createCell(3);
                    timeCell.setCellValue(r.elapsed());
                    if (r.elapsed() == bestTimeGroup) timeCell.setCellStyle(darkGreenStyle);
                    else if (r.elapsed() == bestTimePerImpl.get(r.name())) timeCell.setCellStyle(lightGreenStyle);

                    Cell memCell = row.createCell(4);
                    memCell.setCellValue(r.memory());
                    if (r.memory() == bestMemoryGroup) memCell.setCellStyle(darkGreenStyle);
                    else if (r.memory() == bestMemoryPerImpl.get(r.name())) memCell.setCellStyle(lightGreenStyle);

                    row.createCell(5).setCellValue(r.gcCount());
                    row.createCell(6).setCellValue(r.gcTime());

                    Cell cpuCell = row.createCell(7);
                    cpuCell.setCellValue(r.cpuUsage());
                    if (r.cpuUsage() == bestCpuGroup) cpuCell.setCellStyle(darkGreenStyle);
                    else if (r.cpuUsage() == bestCpuPerImpl.get(r.name())) cpuCell.setCellStyle(lightGreenStyle);
                }

                // Ajustar colunas
                for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

                // Inserir gráficos abaixo
                int chartStart = rowIdx + 1;
                createChart(sheet, workbook, group, chartStart, maxPages, "Tempo (ms)", BenchmarkResult::elapsed);
                createChart(sheet, workbook, group, chartStart + 16, maxPages, "Memória (MB)", BenchmarkResult::memory);
                createChart(sheet, workbook, group, chartStart + 32, maxPages, "CPU Usage (%)", BenchmarkResult::cpuUsage);

                rowIdx = chartStart + 48;
            }

            try (FileOutputStream out = new FileOutputStream("benchmark_summary.xlsx")) {
                workbook.write(out);
                System.out.println("Resultados e gráficos salvos em benchmark_summary.xlsx");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static <T extends Number> void createChart(XSSFSheet sheet, XSSFWorkbook workbook,
                                                       List<BenchmarkResult> data, int anchorRow,
                                                       int maxPages, String metricName,
                                                       ToDoubleFunction<BenchmarkResult> mapper) {
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 0, anchorRow, 10, anchorRow + 15);
        XSSFChart chart = drawing.createChart(anchor);
        // Separando a execução sequencial dos demais
        List<String> categories = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        Double sequentialValue = null;

        for (BenchmarkResult r : data) {
            if (r.name().toLowerCase().contains("sequencial")) {
                sequentialValue = mapper.applyAsDouble(r);
            } else {
                categories.add(r.name() + "-T" + r.threads());
                values.add(mapper.applyAsDouble(r));
            }
        }

        // Incluir o valor sequencial no título se disponível
        String chartTitle = metricName + " por Implementação - maxPages=" + maxPages;
        if (sequentialValue != null) {
            // Formatar o valor sequencial com 2 casas decimais
            String formattedSeqVal = String.format("%.2f", sequentialValue);
            chartTitle += " (Sequencial: " + formattedSeqVal + ")";
        }
        chart.setTitleText(chartTitle);
        chart.setTitleOverlay(false);

        // Eixo X (categorias)
        XDDFCategoryAxis xAxis = chart.createCategoryAxis(org.apache.poi.xddf.usermodel.chart.AxisPosition.BOTTOM);
        xAxis.setTitle("Impl + Threads");

        // Eixo Y (valores)
        XDDFValueAxis yAxis = chart.createValueAxis(org.apache.poi.xddf.usermodel.chart.AxisPosition.LEFT);
        yAxis.setTitle(metricName);

        // Definindo as fontes de dados para as categorias e valores (sem sequencial)
        XDDFCategoryDataSource cat = XDDFDataSourcesFactory.fromArray(categories.toArray(new String[0]));
        XDDFNumericalDataSource<Double> val = XDDFDataSourcesFactory.fromArray(values.toArray(new Double[0]));

        // Criando o gráfico de colunas (barras verticais)
        XDDFBarChartData columnData = (XDDFBarChartData) chart.createData(ChartTypes.BAR, xAxis, yAxis);
        // Configura a direção como vertical (VERTICAL = barras para cima)
        columnData.setBarDirection(BarDirection.COL);
        XDDFChartData.Series columnSeries = columnData.addSeries(cat, val);
        columnSeries.setTitle(metricName, null);
        chart.plot(columnData);

    }

    public record BenchmarkResult(String name, int maxPages, int threads, double elapsed, double memory, long gcCount, long gcTime, double cpuUsage) {}
}
