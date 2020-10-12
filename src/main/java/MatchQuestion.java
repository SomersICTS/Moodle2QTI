import utils.SLF4J;
import utils.XMLParser;
import utils.XMLWriter;

import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MatchQuestion extends Question {

    private boolean shuffleAnswers;
    private List<SubQuestion> subQuestions = null;
    private List<SubQuestion> sortedByAnswer = null;

    public MatchQuestion(Category category) {
        super(category);

        this.subQuestions = new ArrayList<>();
    }

    public static Question createFromMXML(XMLParser xmlParser, Category category) throws XMLStreamException {
        MatchQuestion matchQuestion = new MatchQuestion(category);

        matchQuestion.parseSetupFromMXML(xmlParser);


        SLF4J.LOGGER.debug("Created Match-question: '{}'", matchQuestion.name);

        return matchQuestion;
    }

    public void parseSetupFromMXML(XMLParser xmlParser) throws XMLStreamException {

        super.parseSetupFromMXML(xmlParser);

        this.shuffleAnswers = xmlParser.acceptOptionalElementValue("shuffleanswers", true);

        super.parseFeedbackFromMXML(xmlParser);

        SubQuestion sub;
        while ((sub = SubQuestion.parseFromMXML(xmlParser, this)) != null) {
            this.subQuestions.add(sub);
        }

        final SubQuestion[] previous = {null};
        this.sortedByAnswer = this.subQuestions.stream()
                .sorted((q1,q2) -> q1.getAnswer().compareTo(q2.getAnswer()))
                .filter(q -> { SubQuestion prev = previous[0];
                               previous[0] = q;
                               return prev == null || !(q.getAnswer().equals(prev.getAnswer()));
                    })
                .collect(Collectors.toList());
    }

    @Override
    public void exportQTI21ResponseDeclaration(XMLWriter xmlWriter) throws XMLStreamException {
        xmlWriter.writeStartElement("responseDeclaration");
        xmlWriter.writeAttribute("identifier", "RESPONSE");
        xmlWriter.writeAttribute("cardinality", "multiple");
        xmlWriter.writeStartElement("correctResponse");
        int numQuestions = 0;
        for (SubQuestion q: this.subQuestions) {
            if (q.getIdText().isEmpty()) continue;
            numQuestions++;
            xmlWriter.writeStartElement("value");
            xmlWriter.writeAttribute("fieldIdentifier", q.getIdText());
            xmlWriter.writeAttribute("baseType", "string");
            xmlWriter.writeCharacters(
                    this.sortedByAnswer.stream()
                            .filter(qa->qa.getAnswer().equals(q.getAnswer()))
                            .map(SubQuestion::getIdText)
                            .findFirst().orElse("unspecified")
            );
            xmlWriter.writeEndElement(); // value
        }
        xmlWriter.writeEndElement(); // correctResponse
        xmlWriter.writeStartElement("mapping");
        xmlWriter.writeAttribute("defaultValue", "0");
        for (SubQuestion q: this.subQuestions) {
            if (q.getIdText().isEmpty()) continue;
            xmlWriter.writeEmptyElement("mapEntry");
            xmlWriter.writeAttribute("mapKey", q.getIdText());
            xmlWriter.writeAttribute("mappedValue", String.valueOf(1.0 / numQuestions));
        }
        xmlWriter.writeEndElement(); // mapping
        xmlWriter.writeEndElement(); // responseDeclaration
    }

    @Override
    public void exportQTI21QuestionChoices(XMLWriter xmlWriter) throws XMLStreamException {
        xmlWriter.writeStartElement("div");
        xmlWriter.writeAttribute("class", "interactieblok");
        xmlWriter.writeStartElement("div");
        xmlWriter.writeAttribute("class", "textblock tvblock moodletext");
        xmlWriter.writeStartElement("div");
        xmlWriter.writeAttribute("class", "rte_zone tveditor1");
        xmlWriter.writeStartElement("table");
        xmlWriter.writeStartElement("tbody");

        for (SubQuestion q: this.subQuestions) {
            if (q.getIdText().isEmpty()) continue;
            xmlWriter.writeStartElement("tr");
            xmlWriter.writeStartElement("td");
            xmlWriter.writeRawCharacters(QuestionBank.fixHTMLforQTI21(q.getText()));
            xmlWriter.writeEndElement(); // td
            xmlWriter.writeStartElement("td");
            xmlWriter.writeStartElement("inlineChoiceInteraction");
            xmlWriter.writeAttribute("class", "multipleinput");
            xmlWriter.writeAttribute("id", q.getIdText());
            xmlWriter.writeAttribute("responseIdentifier", "RESPONSE");
            xmlWriter.writeAttribute("shuffle", "false");
            xmlWriter.writeAttribute("required", "true");
            for (SubQuestion qa: this.sortedByAnswer) {
                xmlWriter.writeStartElement("inlineChoice");
                xmlWriter.writeAttribute("identifier", qa.getIdText());
                xmlWriter.writeCharacters(qa.getAnswer());
                xmlWriter.writeEndElement(); // inlineChoice
            }
            xmlWriter.writeEndElement(); // inlineChoiceInteraction
            xmlWriter.writeEndElement(); // td
            xmlWriter.writeEndElement(); // tr
        }

        xmlWriter.writeEndElement(); // tbody
        xmlWriter.writeEndElement(); // table
        xmlWriter.writeEndElement(); // div
        xmlWriter.writeEndElement(); // div
        xmlWriter.writeEndElement(); // div
    }

    @Override
    public void exportQTI21ResponseProcessing(XMLWriter xmlWriter) throws XMLStreamException {
        xmlWriter.writeEmptyElement("responseProcessing");
        xmlWriter.writeAttribute("templateLocation", "/templates/RPTEMPLATE_SCORE.xml");
    }
}

