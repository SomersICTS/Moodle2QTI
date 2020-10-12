package utils;

import javanet.staxutils.IndentingXMLStreamWriter;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.*;

public class XMLWriter extends IndentingXMLStreamWriter {
    private String sourceName = "";
    private OutputStream outputStream = null;

    public XMLWriter(String resourceName) {
        this(FileOutputStreamOrNull(resourceName));
        this.sourceName = resourceName;
    }

    private static OutputStream FileOutputStreamOrNull(String resourceName) {
        try {
            return new FileOutputStream(resourceName);
        } catch (FileNotFoundException ex) {
            SLF4J.logException("Cannot create file " + resourceName, ex);
            return null;
        }
    }

    public XMLWriter(OutputStream outputStream) {
        super(createXMLStreamWriter(outputStream));
        this.outputStream = outputStream;
    }

    public static XMLStreamWriter createXMLStreamWriter(OutputStream output) {
        XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
        XMLStreamWriter xmlStreamWriter = null;
        try {
            xmlStreamWriter = outputFactory.createXMLStreamWriter(output);
        } catch (XMLStreamException e) {
            SLF4J.logException("Cannot attach XMLStreamWriter to file stream handle", e);
        }
        return xmlStreamWriter;
    }

    public void writeRawCharacters(String text) throws XMLStreamException {
        this.writeCharacters("");
        this.flush();
        try {
            this.outputStream.write(text.getBytes());
            this.outputStream.flush();
        } catch (IOException e) {
            SLF4J.logException("Cannot write raw characters at " + this.sourceName, e);
        }
    }
}
