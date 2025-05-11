package Benchmark;

import java.io.FileOutputStream;

import java.util.*;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import java.io.*;
import java.nio.file.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.xddf.usermodel.XDDFShapeProperties;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xddf.usermodel.chart.*;

public class BenchmarkRunnerGC {

    public static void main(String[] args) {
        String fileName = "WikiDumps/enwiki-20250420-pages-meta-current1.xml-p1p41242";

        int[] maxPagesArray = {500, 5000, 10000, 25000};
        int[] threadCounts = {2, 4, 8, 12, 16};
        String[] gcTypes = {"G1GC", "ParallelGC", "SerialGC", "DefaultGC"};

        List<BenchmarkResult> allResults = new ArrayList<>();

        try {
            for (String gcType : gcTypes) {
                System.out.println("Executando benchmarks para GC: " + gcType);

                for (int maxPages : maxPagesArray) {
                    for (int choice = 1; choice <= 5; choice++) {
                        if (choice == 1) {
                            allResults.add(runBenchmarkWithProcess(choice, maxPages, fileName, 1, gcType));
                        } else {
                            for (int threadNum : threadCounts) {
                                allResults.add(runBenchmarkWithProcess(choice, maxPages, fileName, threadNum, gcType));
                            }
                        }
                    }
                }
            }

            saveResultsToExcel(allResults);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static BenchmarkResult runBenchmarkWithProcess(int choice, int maxPages, String fileName, int threadNumber, String gcType) throws Exception {
        String implName = getImplName(choice);
        String className = getClassName(choice);

        // Construir o comando para executar como processo separado
        ProcessBuilder pb = new ProcessBuilder();
        List<String> command = new ArrayList<>();
        command.add("java");

        // Configurações de memória
        command.add("-Xms2g");
        command.add("-Xmx4g");

        // Configuração do GC
        switch (gcType) {
            case "G1GC":
                command.add("-XX:+UseG1GC");
                break;
            case "ParallelGC":
                command.add("-XX:+UseParallelGC");
                break;
            case "SerialGC":
                command.add("-XX:+UseSerialGC");
                break;
            case "DefaultGC":
                break;
        }

        // Log do GC
        String logDir = "Logs/Machine1" + implName;
        Files.createDirectories(Paths.get(logDir));
        String logFilePath = logDir + "/gc-" + gcType.toLowerCase() + "-" + maxPages + "-" + threadNumber + ".log";

        // Verificar se o ficheiro já existe e remover
        Path logFile = Paths.get(logFilePath);
        if (Files.exists(logFile)) {
            Files.delete(logFile);
        }

        // Adicionar o caminho do log ao comando
        command.add("-Xlog:gc*:file=" + logFilePath);

        // Classe a ser executada e argumentos
        command.add("-cp");
        command.add("target");
        command.add(className);
        command.add(String.valueOf(maxPages));
        command.add(fileName);
        if (choice != 1) { // Se não for sequencial, adiciona o número de threads
            command.add(String.valueOf(threadNumber));
        }

        pb.command(command);
        pb.redirectErrorStream(true);

        System.out.println("Executando: " + String.join(" ", command));

        // Registra dados antes da execução
        long timeBefore = System.nanoTime();

        // Executa o processo
        Process process = pb.start();

        // Captura a saída do processo
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                System.out.println(line);
            }
        }

        // Aguarda o término do processo
        int exitCode = process.waitFor();
        long timeAfter = System.nanoTime();

        if (exitCode != 0) {
            System.err.println("Processo terminou com erro, código: " + exitCode);
            System.err.println("Saída: " + output);
        }

        // Ler os dados do arquivo de log do GC para extrair métricas
        GCMetrics gcMetrics = parseGCLog(logFilePath);

        // Calcula o tempo em ms
        double elapsedMs = (timeAfter - timeBefore) / 1_000_000.0;

        return new BenchmarkResult(
                implName,
                maxPages,
                threadNumber,
                Math.round(elapsedMs * 100.0) / 100.0,
                gcMetrics.memoryUsed,
                gcMetrics.cpuUsage,
                gcType
        );
    }

    private static GCMetrics parseGCLog(String logFile) {
        GCMetrics metrics = new GCMetrics();

        try {
            Path path = Paths.get(logFile);
            if (!Files.exists(path)) {
                System.err.println("Arquivo de log do GC não encontrado: " + logFile);
                return metrics;
            }

            List<String> lines = Files.readAllLines(path);

            // Contadores para análise do log
            int gcCount = 0;
            long totalGcTimeMs = 0;
            double memoryBefore = 0;
            double memoryAfter = 0;
            double cpuUsage = 0;

            // Analisar linhas do log do GC
            Pattern gcPattern = Pattern.compile(".*\\[(\\d+\\.\\d+)s\\][^\\[]*GC\\(\\d+\\).*");
            Pattern memoryPattern = Pattern.compile(".*\\[(\\d+\\.\\d+)s\\][^\\[]*([0-9.]+[MKG]).*=>.*([0-9.]+[MKG]).*");
            Pattern cpuPattern = Pattern.compile(".*\\[(\\d+\\.\\d+)s\\][^\\[]*CPU: (\\d+\\.\\d+)% usr.*");

            for (String line : lines) {
                // Contar eventos de GC
                Matcher gcMatcher = gcPattern.matcher(line);
                if (gcMatcher.matches()) {
                    gcCount++;
                }

                // Extrair uso de memória
                Matcher memMatcher = memoryPattern.matcher(line);
                if (memMatcher.matches()) {
                    // Captura o último uso de memória registrado
                    memoryAfter = parseMemorySize(memMatcher.group(3));
                    if (memoryBefore == 0) {
                        memoryBefore = parseMemorySize(memMatcher.group(2));
                    }
                }

                // Extrair uso de CPU
                Matcher cpuMatcher = cpuPattern.matcher(line);
                if (cpuMatcher.matches()) {
                    cpuUsage = Double.parseDouble(cpuMatcher.group(2));
                }
            }

            // Calcular tempo total do GC a partir do tempo registrado nos logs
            for (String line : lines) {
                if (line.contains("GC(") && line.contains("ms")) {
                    Pattern timePattern = Pattern.compile(".*\\[(\\d+\\.\\d+)s\\].*,\\s*(\\d+\\.\\d+)\\s*ms\\)");
                    Matcher timeMatcher = timePattern.matcher(line);
                    if (timeMatcher.find()) {
                        totalGcTimeMs += Double.parseDouble(timeMatcher.group(2));
                    }
                }
            }

            metrics.memoryUsed = Math.max(0, (memoryAfter - memoryBefore) / (1024 * 1024)); // Convertido para MB
            metrics.cpuUsage = cpuUsage;

        } catch (Exception e) {
            System.err.println("Erro ao analisar o log do GC: " + e.getMessage());
            e.printStackTrace();
        }

        return metrics;
    }

    private static double parseMemorySize(String memorySizeStr) {
        double size = Double.parseDouble(memorySizeStr.replaceAll("[^0-9.]", ""));

        if (memorySizeStr.endsWith("G")) {
            size *= 1024 * 1024 * 1024;
        } else if (memorySizeStr.endsWith("M")) {
            size *= 1024 * 1024;
        } else if (memorySizeStr.endsWith("K")) {
            size *= 1024;
        }

        return size;
    }

    private static String getClassName(int choice) {
        return switch (choice) {
            case 1 -> "SequentialSolution.SequentialSolution";
            case 2 -> "MultithreadedSolutionWithThreadPools.MultithreadedSolutionWithThreadPools";
            case 3 -> "ForkJoinFrameworkSolution.ForkJoinFrameworkSolution";
            case 4 -> "MultithreadedSolutionWithoutThreadPools.MultithreadedSolutionWithoutThreadPools";
            case 5 -> "CompletableFuturesBasedSolution.CompletableFuturesBasedSolution";
            default -> throw new IllegalArgumentException("Opção inválida: " + choice);
        };
    }

    private static String getImplName(int choice) {
        return switch (choice) {
            case 1 -> "Sequencial";
            case 2 -> "ThreadPool";
            case 3 -> "ForkJoin";
            case 4 -> "MultithreadedManual";
            case 5 -> "CompFutures";
            default -> "Desconhecido";
        };
    }

    private static void saveResultsToExcel(List<BenchmarkResult> results) {
        String outputDir = "BenchmarkResultsOut/BenchmarkRunnerGC";
        try {
            // Cria o diretório se não existir
            Files.createDirectories(Paths.get(outputDir));

            try (XSSFWorkbook workbook = new XSSFWorkbook()) {
                XSSFSheet sheet = workbook.createSheet("Resultados");

                // Cabeçalhos
                String[] headers = {"Implementação", "maxPages", "Threads", "Tempo (ms)", "Memória (MB)", "CPU Usage (%)"};
                CellStyle headerStyle = workbook.createCellStyle();
                XSSFFont boldFont = workbook.createFont();
                boldFont.setBold(true);
                headerStyle.setFont(boldFont);

                // Estilos de cor
                CellStyle lightGreenStyle = workbook.createCellStyle();
                lightGreenStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
                lightGreenStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                CellStyle darkGreenStyle = workbook.createCellStyle();
                darkGreenStyle.setFillForegroundColor(IndexedColors.BRIGHT_GREEN.getIndex());
                darkGreenStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                // Ordena e agrupa por maxPages e GC Type
                results.sort(Comparator.comparingInt(BenchmarkResult::maxPages).thenComparing(BenchmarkResult::gcType));
                Map<String, List<BenchmarkResult>> grouped = results.stream()
                        .collect(Collectors.groupingBy(r -> r.maxPages() + "_" + r.gcType(), LinkedHashMap::new, Collectors.toList()));

                int rowIdx = 0;

                for (Map.Entry<String, List<BenchmarkResult>> entry : grouped.entrySet()) {
                    String[] parts = entry.getKey().split("_");
                    int maxPages = Integer.parseInt(parts[0]);
                    String gcType = parts[1];

                    // Título do grupo
                    Row titleRow = sheet.createRow(rowIdx++);
                    Cell titleCell = titleRow.createCell(0);
                    titleCell.setCellValue("Resultados para maxPages = " + maxPages + ", GC = " + gcType);
                    titleCell.setCellStyle(headerStyle);

                    // Cabeçalho da tabela
                    Row headerRow = sheet.createRow(rowIdx++);
                    for (int i = 0; i < headers.length; i++) {
                        Cell cell = headerRow.createCell(i);
                        cell.setCellValue(headers[i]);
                        cell.setCellStyle(headerStyle);
                    }

                    // Encontrar o melhor tempo de execução geral do grupo
                    BenchmarkResult bestGroupResult = entry.getValue().stream()
                            .min(Comparator.comparingDouble(BenchmarkResult::elapsed))
                            .orElse(null);

                    // Agrupar por algoritmo e encontrar o melhor tempo para cada algoritmo
                    Map<String, BenchmarkResult> bestByAlgorithm = entry.getValue().stream()
                            .collect(Collectors.groupingBy(BenchmarkResult::name,
                                    Collectors.minBy(Comparator.comparingDouble(BenchmarkResult::elapsed))))
                            .entrySet().stream()
                            .filter(e -> e.getValue().isPresent())
                            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));

                    // Dados
                    for (BenchmarkResult r : entry.getValue()) {
                        Row row = sheet.createRow(rowIdx++);
                        row.createCell(0).setCellValue(r.name());
                        row.createCell(1).setCellValue(r.maxPages());
                        row.createCell(2).setCellValue(r.threads());
                        row.createCell(3).setCellValue(r.elapsed());
                        row.createCell(4).setCellValue(r.memory());
                        row.createCell(5).setCellValue(r.cpuUsage());

                        // Aplicar verde claro ao melhor tempo de cada algoritmo
                        if (bestByAlgorithm.containsKey(r.name()) && bestByAlgorithm.get(r.name()).equals(r)) {
                            row.getCell(3).setCellStyle(lightGreenStyle);
                        }

                        // Aplicar verde escuro ao melhor tempo geral do grupo
                        if (r.equals(bestGroupResult)) {
                            row.getCell(3).setCellStyle(darkGreenStyle);
                        }
                    }

                    // Inserir gráficos abaixo
                    int chartStart = rowIdx + 1;
                    createChart(sheet, workbook, entry.getValue(), chartStart, maxPages, gcType, "Tempo (ms)", BenchmarkResult::elapsed);
                    createChart(sheet, workbook, entry.getValue(), chartStart + 16, maxPages, gcType, "Memória (MB)", BenchmarkResult::memory);
                    createChart(sheet, workbook, entry.getValue(), chartStart + 32, maxPages, gcType, "CPU Usage (%)", BenchmarkResult::cpuUsage);

                    rowIdx = chartStart + 50;
                }

                // Salvar o ficheiro na pasta especificada
                String outputPath = outputDir + "/benchmark_summary_gc.xlsx";
                try (FileOutputStream out = new FileOutputStream(outputPath)) {
                    workbook.write(out);
                    System.out.println("Resultados e gráficos salvos em " + outputPath);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static <T extends Number> void createChart(XSSFSheet sheet, XSSFWorkbook workbook,
                                                       List<BenchmarkResult> data, int anchorRow,
                                                       int maxPages, String gcType, String metricName,
                                                       ToDoubleFunction<BenchmarkResult> mapper) {
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 0, anchorRow, 10, anchorRow + 15);
        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText(metricName + " por Implementação - maxPages=" + maxPages + ", GC=" + gcType);
        chart.setTitleOverlay(false);

        XDDFCategoryAxis xAxis = chart.createCategoryAxis(org.apache.poi.xddf.usermodel.chart.AxisPosition.BOTTOM);
        xAxis.setTitle("Impl + Threads");
        XDDFValueAxis yAxis = chart.createValueAxis(org.apache.poi.xddf.usermodel.chart.AxisPosition.LEFT);
        yAxis.setTitle(metricName);

        List<String> categories = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        for (BenchmarkResult r : data) {
            categories.add(r.name() + "-T" + r.threads());
            values.add(mapper.applyAsDouble(r));
        }

        XDDFCategoryDataSource cat = org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory.fromArray(
                categories.toArray(new String[0]));
        XDDFNumericalDataSource<Double> val = org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory.fromArray(
                values.toArray(new Double[0]));

        XDDFChartData chartData = chart.createData(
                ChartTypes.BAR, xAxis, yAxis);

        if (chartData instanceof XDDFBarChartData) {
            ((XDDFBarChartData) chartData).setBarDirection(org.apache.poi.xddf.usermodel.chart.BarDirection.COL);
        }

        yAxis.setCrossBetween(org.apache.poi.xddf.usermodel.chart.AxisCrossBetween.BETWEEN);

        // Calcula valores máximos e mínimos para ajustar a escala do eixo Y
        double maxValue = values.stream().mapToDouble(Double::doubleValue).max().orElse(1000.0);
        double minValue = values.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);

        yAxis.setMinimum(minValue);  // Ajusta o valor mínimo
        yAxis.setMaximum(maxValue * 1.1);  // Ajusta o valor máximo para dar margem ao gráfico

        // Ajusta do intervalo de unidades principais no eixo Y
        double yAxisRange = maxValue - minValue;
        double majorUnit = yAxisRange / 10;  // Divida o intervalo por 10 para obter um espaçamento mais preciso
        yAxis.setMajorUnit(majorUnit);

        // Ajusta o estilo da grade principal para maior precisão
        XDDFShapeProperties majorGridProperties = yAxis.getOrAddMajorGridProperties();
        if (majorGridProperties != null && majorGridProperties.getLineProperties() != null) {
            majorGridProperties.getLineProperties().setWidth(0.5);
        }

        XDDFChartData.Series series = chartData.addSeries(cat, val);
        series.setTitle(metricName, null);
        chart.plot(chartData);

        // Ajusta a altura da linha onde o gráfico foi ancorado
        Row row = sheet.getRow(anchorRow);
        if (row == null) {
            row = sheet.createRow(anchorRow); // Cria a linha se ela não existir
        }
        row.setHeightInPoints(300); // Define a altura da linha para o gráfico
    }

    // Classe para armazenar métricas extraídas dos logs de GC
    private static class GCMetrics {
        double memoryUsed = 0;
        double cpuUsage = 0;
    }

    public record BenchmarkResult(String name, int maxPages, int threads, double elapsed, double memory,
                                  double cpuUsage, String gcType) {
    }
}