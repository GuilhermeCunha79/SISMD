# Benchmark Runner for Data Processing Solutions

This project runs benchmarks comparing different data processing solutions based on performance metrics such as memory usage, execution time, garbage collection (GC) count and time, and CPU usage.

## Implemented Solutions

- **Sequential Solution**
- **Thread Pool Solution**
- **ForkJoin Framework Solution**
- **Manual Multithreaded Solution**
- **CompletableFuture Solution**

## Table of Contents

- [Benchmark Configuration](#benchmark-configuration)
- [How to Run](#how-to-run)
- [Saving Results](#saving-results)

## Benchmark Configuration

The following variables define benchmark behavior:

### 1. Benchmark Configuration without Garbage Collectors
This configuration is used for the benchmark without specifying any particular garbage collector.

- **fileName**: Path to the XML data file.
  - Example: `WikiDumps/enwiki-20250420-pages-meta-current1.xml-p1p41242`

- **maxPagesArray**: Maximum number of pages to process per run.
  - Example: `{1000, 10000, 25000, 50000}`

- **threadCounts**: Number of threads used in multithreaded solutions.
  - Example: `{2, 4, 8, 12, 16}`

- **choice**: Selects the processing implementation to benchmark:
  - `1` - Sequential
  - `2` - Thread Pool
  - `3` - ForkJoin Framework
  - `4` - Manual Multithreading
  - `5` - CompletableFuture

### 2. Benchmark Configuration with Garbage Collectors
This configuration is used for the benchmark with different Garbage Collectors.

- **fileName**: Path to the XML data file.
  - Example: `WikiDumps/enwiki-20250420-pages-meta-current1.xml-p1p41242`

- **maxPagesArray**: Maximum number of pages to process per run.
  - Example: `{1000, 10000, 25000, 50000}`

- **threadCounts**: Number of threads used in multithreaded solutions.
  - Example: `{2, 4, 8, 12, 16}`

- **gcTypes**: The types of Garbage Collectors to benchmark:
  - Example: `{"G1GC", "ParallelGC", "SerialGC", "DefaultGC"}`

- **choice**: Selects the processing implementation to benchmark:
  - `1` - Sequential
  - `2` - Thread Pool
  - `3` - ForkJoin Framework
  - `4` - Manual Multithreading
  - `5` - CompletableFuture

## How to Run

After compiling the project, you can run two types of benchmarks:

### Benchmark without Garbage Collectors
This runs the benchmark without specifying any particular garbage collector. It is suitable for a general performance comparison without considering GC impacts.

Run it using:
```bash
java -cp target/sismd-project1-1.0-SNAPSHOT-shaded.jar Benchmark.BenchmarkRunner
```

### Benchmark with Garbage Collectors
This version runs the benchmark with different Garbage Collectors (G1GC, ParallelGC, SerialGC, DefaultGC) to evaluate how each GC affects the performance of the data processing solutions.

Run it using:
```bash
java -cp target/sismd-project1-1.0-SNAPSHOT-shaded.jar Benchmark.BenchmarkRunnerGC
```

Both versions collect the following metrics during execution:
- Execution time (ms)
- Memory usage (MB)
- GC count
- GC time (ms)
- CPU usage (%)

## Saving Results

Results are saved as Excel files in the following locations:

### Without Garbage Collectors
Results from the benchmark without specifying any GC are saved in:
```
BenchmarkResultsOut/BenchmarkRunner
```

### With Garbage Collectors
Results from the benchmark with different Garbage Collectors are saved in a separate file located at:
```
BenchmarkResultsOut/BenchmarkRunnerGC
```

Both files include a table with these columns:
- Implementation
- Max Pages
- Thread Count
- Time (ms)
- Memory (MB)
- GC Count
- GC Time (ms)
- CPU Usage (%)

Performance comparison charts between implementations are also included.

**Note**: A detailed analysis report can be found in the `SISMD_REPORT.pdf`.