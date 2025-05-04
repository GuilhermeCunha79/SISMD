import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;

public class SequentialSolution {

  private static final HashMap<String, Integer> counts =
          new HashMap<>();

  public static void main(String[] args, int maxPages, String fileName) throws Exception {

    Iterable<Page> pages = new Pages(maxPages, fileName);
    for(Page page: pages) {
      if(page == null)
        break;
      Iterable<String> words = new Words(page.getText());
      for (String word: words)
        if(word.length()>1 || word.equals("a") || word.equals("I"))
          countWord(word);
    }

    LinkedHashMap<String, Integer> commonWords = new LinkedHashMap<>();
    counts.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())) .forEachOrdered(x -> commonWords.put(x.getKey(), x.getValue()));
    commonWords.entrySet().stream().limit(3).toList().forEach(x -> System.out.println("Word: '" +x.getKey()+ "' with total " +x.getValue()+" occurrences!"));
  }

  private static void countWord(String word) {
    Integer currentCount = counts.get(word);
    if (currentCount == null)
      counts.put(word, 1);
    else
      counts.put(word, currentCount + 1);
  }
}
