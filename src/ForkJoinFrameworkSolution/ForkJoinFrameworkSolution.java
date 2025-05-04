package ForkJoinFrameworkSolution;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import SharedUtilities.*;

public class ForkJoinFrameworkSolution {
    private static final ConcurrentHashMap<String, AtomicInteger> counts = new ConcurrentHashMap<>();

    public static void run(int maxPages, String fileName, int threadNumber) {
        long start = System.currentTimeMillis();

        Iterable<Page> pages = new Pages(maxPages, fileName);
        List<Page> allPages = new ArrayList<>();
        for (Page p : pages) {
            if (p != null) allPages.add(p);
        }

        int threshold = Math.max(10, allPages.size() / (threadNumber * 4));
        System.out.println("Usando " + threadNumber + " threads");
        System.out.println("Threshold: " + threshold);

        ForkJoinPool pool = new ForkJoinPool(threadNumber);
        try {
            pool.invoke(new WordCountTask(allPages, 0, allPages.size(), threshold));
        } finally {
            pool.shutdown();
        }


        long end = System.currentTimeMillis();
        System.out.println("Processed pages: " + allPages.size());
        System.out.println("Elapsed time: " + (end - start) + "ms");

        counts.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue().get(), e1.getValue().get()))
                .limit(3)
                .forEach(e -> System.out.println("Word: '" + e.getKey() + "' with total " + e.getValue() + " occurrences!"));
    }

    static class WordCountTask extends RecursiveAction {
        private final List<Page> pages;
        private final int start, end;
        private final int threshold;

        WordCountTask(List<Page> pages, int start, int end, int threshold) {
            this.pages = pages;
            this.start = start;
            this.end = end;
            this.threshold = threshold;
        }

        @Override
        protected void compute() {
            int length = end - start;
            if (length <= threshold) {
                for (int i = start; i < end; i++) {
                    Page page = pages.get(i);
                    if (page != null) {
                        for (String word : new Words(page.getText())) {
                            if (isValidWord(word)) {
                                countWord(word);
                            }
                        }
                    }
                }
            } else {
                int mid = start + length / 2;
                invokeAll(
                        new WordCountTask(pages, start, mid, threshold),
                        new WordCountTask(pages, mid, end, threshold)
                );
            }
        }
    }

    private static boolean isValidWord(String word) {
        return word.length() > 1 || word.equals("a") || word.equals("I");
    }

    private static void countWord(String word) {
        counts.computeIfAbsent(word, k -> new AtomicInteger(0)).incrementAndGet();
    }
}