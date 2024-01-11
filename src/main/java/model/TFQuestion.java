package model;

import utils.SLF4J;
import utils.XMLParser;
import utils.XMLWriter;

import javax.xml.stream.XMLStreamException;

public class TFQuestion extends SAQuestion {

    public TFQuestion(Category category) {
        super(category, "truefalse");
    }

    public static Question createFromMXML(XMLParser xmlParser, Category category) throws XMLStreamException {
        TFQuestion tfQuestion = new TFQuestion(category);

        tfQuestion.parseSetupFromMXML(xmlParser);

        if (tfQuestion.answers.size() != 2) {
            SLF4J.LOGGER.error("Found {} answers in TF-question {}", tfQuestion.answers.size(), tfQuestion.name);
        }
        SLF4J.LOGGER.info("Created TF-question: '{}'", tfQuestion.name);

        return tfQuestion;
    }

    @Override
    public void exportQTI21ResponseDeclaration(XMLWriter xmlWriter) throws XMLStreamException {
        xmlWriter.writeStartElement("responseDeclaration");
        xmlWriter.writeAttribute("identifier", "RESPONSE");
        xmlWriter.writeAttribute("cardinality", "single");
        xmlWriter.writeAttribute("baseType", "identifier");
        xmlWriter.writeStartElement("correctResponse");
        for (Answer a: this.answers) {
            if (a.getCorrectness() >= PARTIAL_CORRECTNESS) {
                xmlWriter.writeStartElement("value");
                xmlWriter.writeCharacters(a.getId());
                xmlWriter.writeEndElement();
            }
        }

        xmlWriter.writeEndElement(); // correctResponse
        xmlWriter.writeEndElement(); // responseDeclaration
    }

    @Override
    public void exportQTI21QuestionChoices(XMLWriter xmlWriter) throws XMLStreamException {
        xmlWriter.writeStartElement("choiceInteraction");
        xmlWriter.writeAttribute("class", "DrieKolom");
        xmlWriter.writeAttribute("responseIdentifier", "RESPONSE");
        xmlWriter.writeAttribute("shuffle", "false");
        xmlWriter.writeAttribute("maxChoices", "1");

        for (Answer a: this.answers) {
            a.exportQTI21TrueFalseAlternative(xmlWriter);
        }

        xmlWriter.writeEndElement(); // choiceInteraction
    }

    @Override
    public void exportQTI21ResponseProcessing(XMLWriter xmlWriter) throws XMLStreamException {
        xmlWriter.writeEmptyElement("responseProcessing");
        xmlWriter.writeAttribute("templateLocation", "/templates/RPTEMPLATE_GF.xml");
    }
}
