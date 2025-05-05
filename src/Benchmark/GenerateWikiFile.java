package Benchmark;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

public class GenerateWikiFile {

    static final String fileName = "C:\\Users\\Guilherme Cunha\\IdeaProjects\\sismd-project1\\WikiDumps\\large_wiki_file_100000.xml";
    static final int numPages = 100000;

    public static void main(String[] args) throws Exception {
        generateXMLFile(numPages, fileName);
        System.out.println("Arquivo gerado com sucesso!");
    }

    public static void generateXMLFile(int numPages, String fileName) throws Exception {
        XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
        XMLStreamWriter writer = outputFactory.createXMLStreamWriter(new FileOutputStream(fileName), "UTF-8");

        writer.writeStartDocument("UTF-8", "1.0");

        writer.writeStartElement("mediawiki");
        writer.writeDefaultNamespace("http://www.mediawiki.org/xml/export-0.10/");
        writer.writeNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        writer.writeAttribute("http://www.w3.org/2001/XMLSchema-instance", "schemaLocation",
                "http://www.mediawiki.org/xml/export-0.10/ http://www.mediawiki.org/xml/export-0.10.xsd");

        for (int i = 1; i <= numPages; i++) {
            writer.writeStartElement("page");

            // <title>
            writer.writeStartElement("title");
            writer.writeCharacters("GeneratedPage" + i);
            writer.writeEndElement();

            // <ns>
            writer.writeStartElement("ns");
            writer.writeCharacters("0");
            writer.writeEndElement();

            // <id>
            writer.writeStartElement("id");
            writer.writeCharacters(String.valueOf(i));
            writer.writeEndElement();

            // <revision>
            writer.writeStartElement("revision");

            writer.writeStartElement("id");
            writer.writeCharacters(String.valueOf(i));
            writer.writeEndElement();

            writer.writeStartElement("timestamp");
            writer.writeCharacters(generateTimestamp(i));
            writer.writeEndElement();

            // <contributor>
            writer.writeStartElement("contributor");

            writer.writeStartElement("username");
            writer.writeCharacters("User" + i);
            writer.writeEndElement();

            writer.writeStartElement("id");
            writer.writeCharacters(String.valueOf(1000 + i));
            writer.writeEndElement();

            writer.writeEndElement(); // </contributor>

            writer.writeStartElement("comment");
            writer.writeCharacters("Generated comment for page " + i);
            writer.writeEndElement();

            writer.writeStartElement("text");
            writer.writeAttribute("xml:space", "preserve");
            writer.writeCharacters(generateRandomText(i));
            writer.writeEndElement(); // </text>

            writer.writeEndElement(); // </revision>

            writer.writeEndElement(); // </page>
        }

        writer.writeEndElement(); // </mediawiki>
        writer.writeEndDocument();
        writer.close();
    }

    public static String generateRandomText(int pageId) {
        StringBuilder text = new StringBuilder();
        text.append("This is a random article content for page ").append(pageId).append(". ");
        for (int i = 0; i < 5; i++) {
            text.append("Lorem ipsum dolor sit amet, consectetur adipiscing elit. ");
        }
        return text.toString();
    }

    public static String generateTimestamp(int i) {
        LocalDateTime time = LocalDateTime.of(2025, 4, 1, 12, 0).plusMinutes(i);
        return time.format(DateTimeFormatter.ISO_DATE_TIME) + "Z";
    }
}
