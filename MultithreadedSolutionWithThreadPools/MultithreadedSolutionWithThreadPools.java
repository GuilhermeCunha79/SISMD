package MultithreadedSolutionWithThreadPools;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.*;
import java.util.stream.Collectors;

public class MultithreadedSolutionWithThreadPools {

  private static final ConcurrentHashMap<String, AtomicInteger> counts = new ConcurrentHashMap<>(); //  ThreadPool
  // Using AtomicInteger to make a thread-safe counter.
  private static final int numThreads = Runtime.getRuntime().availableProcessors(); // get number of cores available

  public static void main(String[] args, int maxPages, String fileName) throws Exception {
    long start = System.currentTimeMillis();

    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    Iterable<Page> pages = new Pages(maxPages, fileName);

    for (Page page : pages) {
      if (page == null) break;
      executor.submit(() -> processPage(page));
    }

    executor.shutdown();
    executor.awaitTermination(5, TimeUnit.MINUTES); // 5 minutes until thread pool termination

    long end = System.currentTimeMillis();
    System.out.println("Elapsed time: " + (end - start) + "ms");

    // Print the 3 most common words
    LinkedHashMap<String, Integer> commonWords = counts.entrySet().stream()
        .sorted(Map.Entry.<String, AtomicInteger>comparingByValue(Comparator.comparingInt(AtomicInteger::get)).reversed())
        .limit(3)
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            e -> e.getValue().get(),
            (e1, e2) -> e1,
            LinkedHashMap::new
        ));

    commonWords.forEach((word, count) -> System.out.println("Word: '" + word + "' with total " + count + " occurrences!"));
  }

  private static void processPage(Page page) {
    Iterable<String> words = new Words(page.getText());
    for (String word : words) {
      if (word.length() > 1 || word.equals("a") || word.equals("I")) {
        counts.computeIfAbsent(word, k -> new AtomicInteger(0)).incrementAndGet();
      }
    }
  }
}

