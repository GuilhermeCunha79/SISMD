package WikiDumps;

import java.io.*;
import java.util.*;
import javax.xml.stream.*;
import javax.xml.stream.events.*;

public class GenerateWikiFile {

    static final String fileName = "C:\\Users\\Guilherme Cunha\\IdeaProjects\\sismd-project1\\WikiDumps\\large_wiki_file.xml";
    static final int numPages = 10000000; // Número de páginas a gerar no arquivo

    public static void main(String[] args) throws Exception {
        generateXMLFile(numPages, fileName);
        System.out.println("Arquivo gerado com sucesso!");
    }

    public static void generateXMLFile(int numPages, String fileName) throws Exception {
        XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
        XMLStreamWriter writer = outputFactory.createXMLStreamWriter(new FileOutputStream(fileName), "UTF-8");

        writer.writeStartDocument("UTF-8", "1.0");
        writer.writeStartElement("mediawiki");

        for (int i = 1; i <= numPages; i++) {
            writer.writeStartElement("page");

            writer.writeStartElement("title");
            writer.writeCharacters("Page " + i);
            writer.writeEndElement(); // title

            writer.writeStartElement("text");
            writer.writeCharacters(generateRandomText(i));
            writer.writeEndElement(); // text

            writer.writeEndElement(); // page
        }

        writer.writeEndElement(); // mediawiki
        writer.writeEndDocument();
        writer.close();
    }

    // Método para gerar texto fictício para cada página
    public static String generateRandomText(int pageId) {
        StringBuilder text = new StringBuilder();
        text.append("This is a random text for page ").append(pageId).append(". ");
        for (int i = 0; i < 10; i++) {
            text.append("Lorem ipsum dolor sit amet, consectetur adipiscing elit. ");
        }
        return text.toString();
    }
}
