package MultithreadedSolutionWithoutThreadPools;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkerThread extends Thread {
    private final List<Page> pages;
    private final Map<String, Integer> localCounts;

    public WorkerThread(List<Page> pages) {
        this.pages = pages;
        this.localCounts = new HashMap<>();
    }

    @Override
    public void run() {
        for (Page page : pages) {
            Iterable<String> words = new Words(page.getText());
            for (String word : words) {
                if (word.length() > 1 || word.equals("a") || word.equals("I")) {
                    localCounts.merge(word, 1, Integer::sum);
                }
            }
        }
    }

    public Map<String, Integer> getLocalCounts() {
        return localCounts;
    }
}
