package ForkJoinFrameworkSolution;

import java.util.*;
import java.util.concurrent.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.io.*;

public class WordCount {
    static final String fileName = "C:\\Users\\Guilherme Cunha\\IdeaProjects\\sismd-project1\\WikiDumps\\large_wiki_file.xml";
    private static final HashMap<String, Integer> counts = new HashMap<>();

    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();

        // 1. Criar o ForkJoinPool
        ForkJoinPool pool = new ForkJoinPool();

        // 2. Submeter a tarefa principal para o pool
        WordCountTask task = new WordCountTask(fileName);
        Map<String, Integer> result = pool.invoke(task);

        long end = System.currentTimeMillis();
        System.out.println("Elapsed time: " + (end - start) + "ms");

        // 3. Exibe as palavras mais comuns
        result.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(3)
                .forEach(entry -> System.out.println("Word: '" + entry.getKey() + "' with total " + entry.getValue() + " occurrences!"));
    }

    // Tarefa Fork/Join que faz o processamento das páginas
    public static class WordCountTask extends RecursiveTask<Map<String, Integer>> {
        static final int THRESHOLD = 10; // Tamanho mínimo de páginas para divisão

        private String fileName;

        public WordCountTask(String fileName) {
            this.fileName = fileName;
        }

        @Override
        protected Map<String, Integer> compute() {
            List<Page> pages = readPagesFromFile(fileName);
            if (pages.size() <= THRESHOLD) {
                // Se o tamanho for pequeno o suficiente, processa sequencialmente
                return processPages(pages);
            } else {
                // Divide a lista de páginas em duas tarefas
                int mid = pages.size() / 2;
                WordCountTask leftTask = new WordCountTask(fileName);
                WordCountTask rightTask = new WordCountTask(fileName);

                // Fork (cria as tarefas)
                leftTask.fork();
                rightTask.fork();

                // Junta os resultados (join)
                Map<String, Integer> leftResult = leftTask.join();
                Map<String, Integer> rightResult = rightTask.join();

                // Combina os resultados
                return mergeResults(leftResult, rightResult);
            }
        }

        // Processa uma lista de páginas e conta as palavras
        private Map<String, Integer> processPages(List<Page> pages) {
            Map<String, Integer> wordCounts = new HashMap<>();
            for (Page page : pages) {
                String[] words = page.getContent().split("\\s+");
                for (String word : words) {
                    wordCounts.put(word, wordCounts.getOrDefault(word, 0) + 1);
                }
            }
            return wordCounts;
        }

        // Combina os resultados de duas tarefas
        private Map<String, Integer> mergeResults(Map<String, Integer> left, Map<String, Integer> right) {
            Map<String, Integer> merged = new HashMap<>(left);
            for (Map.Entry<String, Integer> entry : right.entrySet()) {
                merged.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
            return merged;
        }

        // Função para ler as páginas do arquivo XML
        private List<Page> readPagesFromFile(String fileName) {
            List<Page> pages = new ArrayList<>();
            try {
                File inputFile = new File(fileName);
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(inputFile);

                doc.getDocumentElement().normalize();

                NodeList pageList = doc.getElementsByTagName("page");
                for (int i = 0; i < pageList.getLength(); i++) {
                    Node pageNode = pageList.item(i);
                    if (pageNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element pageElement = (Element) pageNode;
                        String title = pageElement.getElementsByTagName("title").item(0).getTextContent();
                        String content = pageElement.getElementsByTagName("revision").item(0)
                                .getChildNodes().item(1).getTextContent();  // Aqui, você extrai o conteúdo da página
                        pages.add(new Page(title, content));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return pages;
        }
    }

    // Classe que representa uma página com seu título e conteúdo
    static class Page {
        private String title;
        private String content;

        public Page(String title, String content) {
            this.title = title;
            this.content = content;
        }

        public String getContent() {
            return content;
        }

        public String getTitle() {
            return title;
        }
    }
}
