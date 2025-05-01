package MultithreadedSolutionWithoutThreadPools;

import java.util.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;

public class WordCount {
  static final int maxPages = 100000;
  static final String fileName = "C:\\Users\\Guilherme Cunha\\IdeaProjects\\sismd-project1\\WikiDumps\\large_wiki_file.xml";
  private static final ConcurrentHashMap<String, Integer> counts = new ConcurrentHashMap<>();

  public static void main(String[] args) throws Exception {
    long start = System.currentTimeMillis();

    Iterable<Page> pageIterable = new Pages(maxPages, fileName);
    List<Page> allPages = new ArrayList<>();
    pageIterable.forEach(allPages::add);

    int numThreads = Math.min(Runtime.getRuntime().availableProcessors(), Math.max(1, maxPages / 10));
    List<List<Page>> pageChunks = splitPages(allPages, numThreads);
    List<Thread> threads = new ArrayList<>();

    for (List<Page> chunk : pageChunks) {
      Thread thread = new Thread(() -> processPages(chunk));
      threads.add(thread);
      thread.start();
    }

    for (Thread thread : threads) {
      thread.join();
    }

    long end = System.currentTimeMillis();
    System.out.println("Elapsed time: " + (end - start) + "ms");

    counts.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
            .limit(3)
            .forEach(entry ->
                    System.out.println("Word: '" + entry.getKey() + "' with total " + entry.getValue() + " occurrences!")
            );
  }

  private static List<List<Page>> splitPages(List<Page> allPages, int numChunks) {
    List<List<Page>> chunks = new ArrayList<>();
    int total = allPages.size();
    int chunkSize = (int) Math.ceil(total / (double) numChunks);

    for (int i = 0; i < total; i += chunkSize) {
      int end = Math.min(i + chunkSize, total);
      chunks.add(allPages.subList(i, end));
    }
    return chunks;
  }

  private static void processPages(List<Page> pages) {
    for (Page page : pages) {
      if (page == null) continue;
      Iterable<String> words = new Words(page.getText());
      for (String word : words) {
        if (word.length() > 1 || word.equals("a") || word.equals("I")) {
          countWord(word);
        }
      }
    }
  }

  private static void countWord(String word) {
    counts.merge(word, 1, Integer::sum);
  }
}