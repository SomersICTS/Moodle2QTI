import utils.SLF4J;
import utils.XMLParser;
import utils.XMLWriter;

import javax.xml.stream.XMLStreamException;

public class DescrQuestion extends Question {

    public DescrQuestion(Category category) {
        super(category);
    }

    @Override
    public String getInteractionType() {
        return QuestionBank.QTI_SHORTANSWER_INTERACTION;
    }

    public static Question createFromMXML(XMLParser xmlParser, Category category) throws XMLStreamException {
        DescrQuestion descrQuestion = new DescrQuestion(category);

        descrQuestion.parseSetupFromMXML(xmlParser);

        SLF4J.LOGGER.info("Created Description-question: '{}'", descrQuestion.name);

        return descrQuestion;
    }

    @Override
    public void exportQTI21ResponseDeclaration(XMLWriter xmlWriter) throws XMLStreamException {

        String baseType = "string";

        xmlWriter.writeStartElement("responseDeclaration");
        xmlWriter.writeAttribute("identifier", "RESPONSE");
        xmlWriter.writeAttribute("cardinality", "single");
        xmlWriter.writeAttribute("baseType", baseType);
        xmlWriter.writeStartElement("correctResponse");
            xmlWriter.writeStartElement("value");
            xmlWriter.writeCharacters("ok");
            xmlWriter.writeEndElement();
        xmlWriter.writeEndElement(); // correctResponse
        xmlWriter.writeEndElement(); // responseDeclaration
    }

    @Override
    public void exportQTI21QuestionText(XMLWriter xmlWriter) throws XMLStreamException {

        xmlWriter.writeStartElement("div");
        xmlWriter.writeAttribute("class", QuestionBank.TV_TBZONE_CLASS + " generated");
        if (this.getQuestionBank().getLanguage().startsWith("Dut")) {
            xmlWriter.writeCharacters("(Dit is een casusomschrijving, geen vraag.)");
        } else {
            xmlWriter.writeCharacters("(This is a casus description, not a question.)");
        }
        xmlWriter.writeEndElement(); // div

        super.exportQTI21QuestionText(xmlWriter);
    }

    @Override
    public void exportQTI21QuestionChoices(XMLWriter xmlWriter) throws XMLStreamException {
        xmlWriter.writeEmptyElement("extendedTextInteraction");
        xmlWriter.writeAttribute("responseIdentifier", "RESPONSE");
    }

    @Override
    public void exportQTI21ResponseProcessing(XMLWriter xmlWriter) throws XMLStreamException {
        xmlWriter.writeEmptyElement("responseProcessing");
        xmlWriter.writeAttribute("templateLocation", "/templates/RPTEMPLATE_GF.xml");
    }
}
