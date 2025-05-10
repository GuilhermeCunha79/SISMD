package Benchmark;

public class BenchmarkResult {
    public final String name;
    public final long elapsedTime;
    public final long cpuTime;
    public final long memoryUsed;
    public final long gcCount;
    public final long gcTime;

    public BenchmarkResult(String name, long elapsedTime, long cpuTime, long memoryUsed, long gcCount, long gcTime) {
        this.name = name;
        this.elapsedTime = elapsedTime;
        this.cpuTime = cpuTime;
        this.memoryUsed = memoryUsed;
        this.gcCount = gcCount;
        this.gcTime = gcTime;
    }
}
