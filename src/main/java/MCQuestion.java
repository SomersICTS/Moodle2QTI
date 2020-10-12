import utils.SLF4J;
import utils.XMLParser;
import utils.XMLWriter;

import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.List;

public class MCQuestion extends Question {

    private boolean singleAnswer;
    private boolean shuffleAnswers;
    private String answerNumbering;

    private List<Answer> answers = null;

    public MCQuestion(Category category) {
        super(category);

        this.answers = new ArrayList<>();
    }

    public static Question createFromMXML(XMLParser xmlParser, Category category) throws XMLStreamException {
        MCQuestion mcQuestion = new MCQuestion(category);

        mcQuestion.parseSetupFromMXML(xmlParser);


        SLF4J.LOGGER.debug("Created MC-question: '{}'", mcQuestion.name);

        return mcQuestion;
    }

    public void parseSetupFromMXML(XMLParser xmlParser) throws XMLStreamException {

        super.parseSetupFromMXML(xmlParser);

        this.singleAnswer = xmlParser.acceptOptionalElementValue("single", true);
        this.shuffleAnswers = xmlParser.acceptOptionalElementValue("shuffleanswers", true);
        this.answerNumbering = xmlParser.acceptOptionalElementValue("answernumbering", null);

        super.parseFeedbackFromMXML(xmlParser);

        Answer answer;
        while ((answer = Answer.parseFromMXML(xmlParser, this)) != null) {
            this.answers.add(answer);
        }
    }

    @Override
    public void exportQTI21ResponseDeclaration(XMLWriter xmlWriter) throws XMLStreamException {
        xmlWriter.writeStartElement("responseDeclaration");
        xmlWriter.writeAttribute("identifier", "RESPONSE");
        xmlWriter.writeAttribute("cardinality", this.singleAnswer ? "single" : "multiple");
        xmlWriter.writeAttribute("baseType", "identifier");
        xmlWriter.writeStartElement("correctResponse");
        for (Answer a: this.answers) {
            if (a.getCorrectness() > 0.99) {
                xmlWriter.writeStartElement("value");
                xmlWriter.writeCharacters(a.getId());
                xmlWriter.writeEndElement();
            } else if (a.getCorrectness() > 0.01) {
                SLF4J.LOGGER.error("Partial correctness {} for answer {} on question {} not handled",
                        a.getCorrectness(), a.getId(), this.getFlatName());
            }
        }

        xmlWriter.writeEndElement(); // correctResponse
        xmlWriter.writeEndElement(); // responseDeclaration
    }

    @Override
    public void exportQTI21QuestionChoices(XMLWriter xmlWriter) throws XMLStreamException {
        xmlWriter.writeStartElement("choiceInteraction");
        xmlWriter.writeAttribute("class", this.answers.size() > 5 ? "TweeKolom" : "EenKolom");
        xmlWriter.writeAttribute("responseIdentifier", "RESPONSE");
        xmlWriter.writeAttribute("shuffle", String.valueOf(this.shuffleAnswers));
        xmlWriter.writeAttribute("maxChoices", String.valueOf(this.singleAnswer ? 1 : 0));

        for (Answer a: this.answers) {
            a.exportQTI21Alternative(xmlWriter);
        }

        xmlWriter.writeEndElement(); // choiceInteraction
    }

    @Override
    public void exportQTI21ResponseProcessing(XMLWriter xmlWriter) throws XMLStreamException {
        xmlWriter.writeEmptyElement("responseProcessing");
        xmlWriter.writeAttribute("templateLocation", "/templates/RPTEMPLATE_GF.xml");
    }
}
