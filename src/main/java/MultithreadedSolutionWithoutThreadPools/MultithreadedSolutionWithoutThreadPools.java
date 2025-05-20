package MultithreadedSolutionWithoutThreadPools;

import java.util.*;
import SharedUtilities.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MultithreadedSolutionWithoutThreadPools {
  //Use ConcurrentHashMap and AtomicInteger to ensure atomic updates and avoid concurrency issues
  private static final ConcurrentHashMap<String, AtomicInteger> counts = new ConcurrentHashMap<>();

  public static void main(String[] args) {
    if (args.length < 3) {
      System.err.println("Usage: java MultithreadedSolutionWithoutThreadPools <maxPages> <fileName> <threadNumber>");
      return;
    }

    try {
      int maxPages = Integer.parseInt(args[0]);
      String fileName = args[1];
      int threadNumber = Integer.parseInt(args[2]);

      run(maxPages, fileName, threadNumber);
    } catch (NumberFormatException e) {
      System.err.println("Invalid number format for maxPages or threadNumber. They must be integers.");
    } catch (InterruptedException e) {
      System.err.println("Execution was interrupted: " + e.getMessage());
      e.printStackTrace();
    }
  }

  public static void run(int maxPages, String fileName, int threadNumber) throws InterruptedException {
    long start = System.currentTimeMillis();

    //Load pages from the file
    Iterable<Page> pages = new Pages(maxPages, fileName);
    List<Page> allPages = new ArrayList<>();
    for (Page p : pages) {
      if (p != null) allPages.add(p);
    }

    //Split pages into chunks for parallel processing
    List<List<Page>> pageChunks = splitPages(allPages, threadNumber);

    //Process pages in parallel using individual threads
    List<Thread> threads = new ArrayList<>();
    for (List<Page> chunk : pageChunks) {
      Thread thread = new Thread(() -> processPages(chunk));
      threads.add(thread);
      thread.start();
    }

    //Wait for all threads to finish
    for (Thread thread : threads) {
      thread.join();
    }

    long end = System.currentTimeMillis();
    System.out.println("Processed pages: " + allPages.size());
    System.out.println("Elapsed time: " + (end - start) + "ms");

    //Display the top 3 most frequent words
    counts.entrySet().stream()
            .sorted((entry1, entry2) -> Integer.compare(entry2.getValue().get(), entry1.getValue().get()))
            .limit(3)
            .forEach(entry -> System.out.println("Word: '" + entry.getKey() + "' with total " + entry.getValue() + " occurrences!"));
  }

  //Split the list of pages into chunks for each thread
  private static List<List<Page>> splitPages(List<Page> allPages, int numChunks) {
    List<List<Page>> chunks = new ArrayList<>();
    int totalPages = allPages.size();
    int baseSize = totalPages / numChunks;
    int remainder = totalPages % numChunks;

    int start = 0;
    for (int i = 0; i < numChunks; i++) {
      int extra = (i < remainder) ? 1 : 0;
      int end = start + baseSize + extra;
      chunks.add(allPages.subList(start, end));
      start = end;
    }
    return chunks;
  }

  //Process the list of pages and count valid words
  private static void processPages(List<Page> pages) {
    for (Page page : pages) {
      if (page != null) {
        for (String word : new Words(page.getText())) {
          if (isValidWord(word)) {
            countWord(word);
          }
        }
      }
    }
  }

  //Check if the word is valid to be counted
  private static boolean isValidWord(String word) {
    return word.length() > 1 || word.equals("a") || word.equals("I");
  }

  //Increment word count atomically in the shared map
  private static void countWord(String word) {
    counts.computeIfAbsent(word, k -> new AtomicInteger(0)).incrementAndGet();
  }
}