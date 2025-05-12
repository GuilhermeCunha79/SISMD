# Benchmark Runner for Data Processing Solutions

This project runs benchmarks comparing different data processing solutions based on various criteria such as memory usage, execution time, GC (Garbage Collection) count and time, and CPU usage. The project tests the following implementations of data processing: Sequential Solution, Thread Pool Solution, ForkJoin Framework Solution, Manual Multithreaded Solution and Completable Futures Solution.

## Table of Contents
Benchmark Configuration
Execution Instructions
Saving Results
License

---

## Benchmark Configuration

The variable fileName specifies the path to the data file, for example, WikiDumps/enwiki-20250420-pages-meta-current1.xml-p1p41242.  
The array maxPagesArray defines the maximum number of pages to be processed, for example {500, 5000, 10000, 25000}.  
The array threadCounts defines the number of threads for the multithreaded solutions, for example {2, 4, 8, 12, 16}.  
The choice variable in the runBenchmark method selects which implementation to use: 1 for Sequential Solution, 2 for Thread Pool Solution, 3 for ForkJoin Framework Solution, 4 for Manual Multithreaded Solution and 5 for Completable Futures Solution.

---

## Execution Instructions

Run the benchmark by executing the BenchmarkRunner class after compiling the project:  
java -cp target/benchmark-runner-1.0-SNAPSHOT.jar BenchmarkRunner

During execution, the program collects information about execution time in milliseconds, memory usage in megabytes, GC count, GC time in milliseconds, and CPU usage percentage.

---

## Saving Results

Benchmark results will be saved in an Excel file in the BenchmarkResultsOut/BenchmarkRunner folder. The file will contain a table with the following columns: Implementation name, maxPages, number of Threads, Time in ms, Memory in MB, GC Count, GC Time in ms and CPU Usage percentage. Charts comparing the performance of different implementations will also be included.

---

## License

This project is licensed under the MIT License. See the LICENSE file for details.
