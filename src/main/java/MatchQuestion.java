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
    private List<SubQuestion> uniqueAnswers = null;

    public MatchQuestion(Category category) {
        super(category);

        this.subQuestions = new ArrayList<>();
    }

    @Override
    public String getInteractionType() {
        return QuestionBank.QTI_MIXED_INTERACTION;
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
    }

    private void findUniqueAnswers() {
        this.uniqueAnswers = new ArrayList<>();
        for (SubQuestion q : this.subQuestions) {
            if (this.uniqueAnswers.stream().anyMatch(qa -> qa.getAnswer().equals(q.getAnswer()))) {
                continue;
            }
            this.uniqueAnswers.add(q);
        }
    }

    @Override
    public void exportQTI21ResponseDeclaration(XMLWriter xmlWriter) throws XMLStreamException {

        this.findUniqueAnswers();

        xmlWriter.writeStartElement("responseDeclaration");
        xmlWriter.writeAttribute("identifier", "RESPONSE");
        xmlWriter.writeAttribute("cardinality", "multiple");
        xmlWriter.writeStartElement("correctResponse");
        int numQuestions = 0;
        for (SubQuestion q: this.subQuestions) {
            if (q.getText().isEmpty()) continue;
            numQuestions++;
            xmlWriter.writeStartElement("value");
            xmlWriter.writeAttribute("fieldIdentifier", q.getIdText());
            xmlWriter.writeAttribute("baseType", Question.SELECTION_BASETYPE);
            xmlWriter.writeCharacters(
                    this.uniqueAnswers.stream()
                            .filter(qa->qa.getAnswer().equals(q.getAnswer()))
                            .map(SubQuestion::getAnswerIdText)
                            .findFirst().orElse("unspecified")
            );
            xmlWriter.writeEndElement(); // value
        }
        xmlWriter.writeEndElement(); // correctResponse
        xmlWriter.writeStartElement("mapping");
        xmlWriter.writeAttribute("defaultValue", "0");
        for (SubQuestion q: this.subQuestions) {
            if (q.getText().isEmpty()) continue;
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
        xmlWriter.writeAttribute("class", QuestionBank.TV_INTERACTIONBLOCK_CLASS);
        xmlWriter.writeStartElement("div");
        xmlWriter.writeAttribute("id", QuestionBank.TV_TEXTBLOCK_ID + QuestionBank.getNextTextBlockId());
        xmlWriter.writeAttribute("class", QuestionBank.TV_TEXTBLOCK_CLASS + " moodletext");

        xmlWriter.writeStartElement("div");
        xmlWriter.writeAttribute("class", QuestionBank.TV_TBZONE_CLASS);
        xmlWriter.writeStartElement("table");
        xmlWriter.writeStartElement("tbody");

        for (SubQuestion q: this.subQuestions) {
            if (q.getText().isEmpty()) continue;

            //xmlWriter.writeStartElement("div");
            //xmlWriter.writeAttribute("class", QuestionBank.TV_TBZONE_CLASS);
            xmlWriter.writeStartElement("tr");
            xmlWriter.writeStartElement("td");
            xmlWriter.writeRawCharacters(QuestionBank.fixHTMLforQTI21(q.getText(), this.getPartialName()));
            xmlWriter.writeEndElement(); // td
            xmlWriter.writeStartElement("td");
            xmlWriter.writeStartElement("inlineChoiceInteraction");
            xmlWriter.writeAttribute("class", "multipleinput");
            xmlWriter.writeAttribute("id", q.getIdText());
            xmlWriter.writeAttribute("responseIdentifier", "RESPONSE");
            xmlWriter.writeAttribute("shuffle", String.valueOf(this.shuffleAnswers));
            xmlWriter.writeAttribute("required", "true");
            for (SubQuestion qa: this.uniqueAnswers) {
                String aText = qa.getAnswer();
                aText = QuestionBank.deEscapeHTMLEntities(aText);
                aText = QuestionBank.fixPlainTextforQTI21(aText);
                xmlWriter.writeStartElement("inlineChoice");
                xmlWriter.writeAttribute("identifier", qa.getAnswerIdText());
                //QuestionBank.countAndFlagInvalidWords(new String[] {"<",">","&"}, aText, this.getPartialName() );
                xmlWriter.writeCharacters(aText);
                xmlWriter.writeEndElement(); // inlineChoice
            }
            xmlWriter.writeEndElement(); // inlineChoiceInteraction
            xmlWriter.writeEndElement(); // td
            xmlWriter.writeEndElement(); // tr
            //xmlWriter.writeEndElement(); // div zone
        }

        xmlWriter.writeEndElement(); // tbody
        xmlWriter.writeEndElement(); // table
        xmlWriter.writeEndElement(); // div zone
        xmlWriter.writeEndElement(); // div textblock
        xmlWriter.writeEndElement(); // div interactionblock
    }

    public void exportQTI21ResponseProcessing(XMLWriter xmlWriter) throws XMLStreamException {
        xmlWriter.writeEmptyElement("responseProcessing");
        xmlWriter.writeAttribute("templateLocation", "/templates/RPTEMPLATE_SCORE.xml");
    }
}

