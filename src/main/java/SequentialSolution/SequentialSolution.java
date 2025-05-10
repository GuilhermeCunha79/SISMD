package SequentialSolution;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

import SharedUtilities.*;

public class SequentialSolution {

  private static final HashMap<String, Integer> counts = new HashMap<>();

  public static void main(String[] args) {
    if (args.length < 2) {
      System.err.println("Usage: java SequentialSolution <maxPages> <fileName>");
      return;
    }

    try {
      // Parse arguments from the command line
      int maxPages = Integer.parseInt(args[0]);
      String fileName = args[1];

      // Execute the run method
      run(maxPages, fileName);
    } catch (NumberFormatException e) {
      System.err.println("Invalid number format for maxPages. It must be an integer.");
    } catch (Exception e) {
      System.err.println("Error during execution: " + e.getMessage());
      e.printStackTrace();
    }
  }

  public static void run(int maxPages, String fileName) {
    long start = System.currentTimeMillis(); // Start timer

    Iterable<Page> pages = new Pages(maxPages, fileName);

    // Process pages sequentially
    for (Page page : pages) {
      if (page == null) break;

      // Extract words from the page
      Iterable<String> words = new Words(page.getText());
      for (String word : words) {
        // Only count valid words (length > 1 or specific single letters)
        if (word.length() > 1 || word.equals("a") || word.equals("I")) {
          countWord(word);
        }
      }
    }

    long end = System.currentTimeMillis(); // End timer
    System.out.println("Elapsed time: " + (end - start) + "ms");

    // Sort and display the 3 most common words
    LinkedHashMap<String, Integer> commonWords = counts.entrySet().stream()
            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
            .limit(3)
            .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (e1, e2) -> e1,
                    LinkedHashMap::new
            ));

    commonWords.forEach((word, count) ->
            System.out.println("Word: '" + word + "' with total " + count + " occurrences!")
    );
  }

  // Count a word using a thread-safe approach (HashMap is fine here because it's single-threaded)
  private static void countWord(String word) {
    counts.merge(word, 1, Integer::sum);
  }
}