package MultithreadedSolutionWithThreadPools;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.*;
import java.util.stream.Collectors;
import SharedUtilities.*;

public class MultithreadedSolutionWithThreadPools {

  private static final ConcurrentHashMap<String, AtomicInteger> counts = new ConcurrentHashMap<>(); // ThreadPool
  // Using AtomicInteger to make a thread-safe counter.

  public static void main(String[] args) {
    if (args.length < 3) {
      System.err.println("Usage: java MultithreadedSolutionWithThreadPools <maxPages> <fileName> <threadNumber>");
      return;
    }

    try {
      // Parse arguments from the command line
      int maxPages = Integer.parseInt(args[0]);
      String fileName = args[1];
      int threadNumber = Integer.parseInt(args[2]);

      // Execute the run method
      run(maxPages, fileName, threadNumber);
    } catch (NumberFormatException e) {
      System.err.println("Invalid number format for maxPages or threadNumber. They must be integers.");
    } catch (Exception e) {
      System.err.println("Error during execution: " + e.getMessage());
      e.printStackTrace();
    }
  }

  public static void run(int maxPages, String fileName, int threadNumber) throws Exception {
    long start = System.currentTimeMillis();

    // Here we could use all the available cores, but for performance studies it is better to receive as argument.
    ExecutorService executor = Executors.newFixedThreadPool(threadNumber);
    Iterable<Page> pages = new Pages(maxPages, fileName);

    // Submitting pages to be processed in parallel
    for (Page page : pages) {
      if (page == null) break;
      executor.submit(() -> processPage(page)); // Send each page to a thread.
    }

    // By sending each page to a thread, we have the following scenarios:
    // - The page is small and therefore will free up the thread for the next page.
    // - The page is big and might take a while to free up the thread.
    // We could potentially partition in page chunks.

    // Gracefully shutdown the executor, waiting for all tasks to complete
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

  // Process a page by counting the words, using ConcurrentHashMap<String, AtomicInteger>.
  private static void processPage(Page page) {
    Iterable<String> words = new Words(page.getText());
    for (String word : words) {
      // Only count valid words (length > 1 or specific single letters)
      if (word.length() > 1 || word.equals("a") || word.equals("I")) {
        // Count the word using atomic operation to ensure thread safety
        counts.computeIfAbsent(word, k -> new AtomicInteger(0)).incrementAndGet();
      }
    }
  }
}