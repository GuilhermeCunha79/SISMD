package MultithreadedSolutionWithoutThreadPools;

import java.util.*;
import SharedUtilities.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MultithreadedSolutionWithoutThreadPools {
  //Uso de ConcurrentHashMap e AtomicInteger para garantir que os dados sao atualizados forma tomica e sem problemas de concorrencia
  private static final ConcurrentHashMap<String, AtomicInteger> counts = new ConcurrentHashMap<>();

  public static void main(String[] args, int maxPages, String fileName, int threadNumber) throws InterruptedException {
    long start = System.currentTimeMillis();

    //Carrega as páginas
    Iterable<Page> pages = new Pages(maxPages, fileName);
    List<Page> allPages = new ArrayList<>();
    for (Page p : pages) {
      if (p != null) allPages.add(p);
    }

    //Divide páginas em partes para processar em paralelo
    //int numThreads = Math.min(Runtime.getRuntime().availableProcessors(), allPages.size());
    List<List<Page>> pageChunks = splitPages(allPages, threadNumber);

    //Processa páginas em paralelo
    List<Thread> threads = new ArrayList<>();
    for (List<Page> chunk : pageChunks) {
      Thread thread = new Thread(() -> processPages(chunk));
      threads.add(thread);
      thread.start();
    }

    //Espera todas as threads terminarem
    for (Thread thread : threads) {
      thread.join();
    }

    long end = System.currentTimeMillis();
    System.out.println("Processed pages: " + allPages.size());
    System.out.println("Elapsed time: " + (end - start) + "ms");

    counts.entrySet().stream()
            .sorted((entry1, entry2) -> Integer.compare(entry2.getValue().get(), entry1.getValue().get()))
            .limit(3)
            .forEach(entry -> System.out.println("Word: '" + entry.getKey() + "' with total " + entry.getValue() + " occurrences!"));
  }

  //Divide as páginas em partes para processar em paralelo
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

  //Processa as páginas e conta as palavras
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

  //Verifica se a palavra é válida para contar
  private static boolean isValidWord(String word) {
    return word.length() > 1 || word.equals("a") || word.equals("I");
  }

  //Conta a palavra, atualizando de forma atómica a variavel counts
  private static void countWord(String word) {
    counts.computeIfAbsent(word, k -> new AtomicInteger(0)).incrementAndGet();
  }
}
