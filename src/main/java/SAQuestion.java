import utils.SLF4J;
import utils.XMLParser;
import utils.XMLWriter;

import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.List;

public class SAQuestion extends Question {

    private boolean caseSensitive;
    private final String saKind;

    public String getSaKind() {
        return saKind;
    }

    public SAQuestion(Category category, String saKind) {
        super(category);

        this.saKind = saKind;
    }

    @Override
    public String getInteractionType() {
        return QuestionBank.QTI_SHORTANSWER_INTERACTION;
    }

    public static Question createFromMXML(XMLParser xmlParser, Category category, String type) throws XMLStreamException {
        SAQuestion saQuestion = new SAQuestion(category, type);

        saQuestion.parseSetupFromMXML(xmlParser);


        SLF4J.LOGGER.debug("Created {}-question: '{}'", type, saQuestion.name);

        return saQuestion;
    }

    @Override
    public void parseSetupFromMXML(XMLParser xmlParser) throws XMLStreamException {

        super.parseSetupFromMXML(xmlParser);

        this.caseSensitive = (xmlParser.acceptOptionalElementValue("usecase", 0) > 0);

        this.parseAnswersFromMXML(xmlParser);
    }

    protected List<String> expandedAnswers;

    protected String expandAnswers() {
        boolean first = true;
        this.expandedAnswers = new ArrayList<>();

        for (Answer a : this.answers) {
            String rawValue = a.getText();
            double correctness = a.getCorrectness();
            if (correctness > 0) {
                if (correctness < Question.PARTIAL_CORRECTNESS) {
                    if (first) {
                        SLF4J.LOGGER.warn("Discarded partial score {} of answer {} in question '{}'",
                                correctness, rawValue, this.getPartialName());
                        first = false;
                    }
                    continue;
                } else if (correctness < 100) {
                    if (first) {
                        SLF4J.LOGGER.warn("Upgraded partial score {} of answer {} in question '{}'",
                                correctness, rawValue, this.getPartialName());
                        first = false;
                    }
                }
            } else {
                continue;
            }

            if (QuestionBank.indexOfNonEscaped('*', rawValue, 0) >= 0) {
                String flatValue = QuestionBank.deEscape(rawValue.replace("\\*", "{$$$}").replace("*", "").replace("{$$$}", "\\*"));
                rawValue = rawValue.replace("\\*", "{$$$}").replace("*", " ").replace("{$$$}", "\\*");
                this.expandedAnswers.add(flatValue);
                SLF4J.LOGGER.debug("Expanded *-wildcard in answer '{}' of question '{}'",
                        flatValue, this.getPartialName());
            }
            this.expandedAnswers.add(QuestionBank.deEscape(rawValue));
        }
        return "string";
    }

    @Override
    public void exportQTI21ResponseDeclaration(XMLWriter xmlWriter) throws XMLStreamException {

        String baseType = this.expandAnswers();

        xmlWriter.writeStartElement("responseDeclaration");
        xmlWriter.writeAttribute("identifier", "RESPONSE");
        xmlWriter.writeAttribute("cardinality", this.expandedAnswers.size() > 1 ? "multiple" : "single");
        xmlWriter.writeAttribute("baseType", baseType);
        xmlWriter.writeStartElement("correctResponse");
        //xmlWriter.writeAttribute("interpretation", "caseSensitive");
        for (String aText : this.expandedAnswers) {
            xmlWriter.writeStartElement("value");
            // QuestionBank.countAndFlagInvalidWords(new String[] {"<",">","&"}, aText, this.getPartialName() );
            xmlWriter.writeCharacters(aText);
            xmlWriter.writeEndElement();
        }

        xmlWriter.writeEndElement(); // correctResponse
        xmlWriter.writeEndElement(); // responseDeclaration
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
