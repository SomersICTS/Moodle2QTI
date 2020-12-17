import utils.SLF4J;
import utils.XMLParser;
import utils.XMLWriter;

import javax.xml.stream.XMLStreamException;

public class Answer {

    private static int nextId = 10001;

    private int id = nextId++;
    private double correctness;
    private String text;
    private double tolerance;
    private int toleranceType;
    private int correctAnswerFormat;
    private int correctAnswerLength;
    private String feedback;
    private Question question;

    public static Answer parseFromMXML(XMLParser xmlParser, Question question) throws XMLStreamException {
        if (xmlParser.nextBeginTag("answer")) {
            Answer answer = new Answer();
            answer.question = question;
            answer.correctness = xmlParser.getAttributeValue(null, "fraction", 0.0);
            String format = QuestionBank.parseFormatAttributeFromMXML(xmlParser, question.getPartialName());
            xmlParser.nextTag();
            answer.text = QuestionBank.parseFormattedTextFromMXML(xmlParser, format, question.getPartialName());
            answer.feedback = question.getQuestionBank().parseFormattedElementFromMXML(xmlParser, "feedback", question.getPartialName());
            answer.tolerance = xmlParser.acceptOptionalElementValue("tolerance", 0.0);
            answer.toleranceType = xmlParser.acceptOptionalElementValue("tolerancetype", 0);
            answer.correctAnswerFormat = xmlParser.acceptOptionalElementValue("correctanswerformat", 0);
            answer.correctAnswerLength = xmlParser.acceptOptionalElementValue("correctanswerlength", 0);
            if (answer.feedback == null) {
                answer.feedback = question.getQuestionBank().parseFormattedElementFromMXML(xmlParser, "feedback", question.getPartialName());
            }
            xmlParser.findAndAcceptEndTag("answer");
            return answer;
        }
        return null;
    }

    public void exportQTI21Alternative(XMLWriter xmlWriter) throws XMLStreamException {
        xmlWriter.writeStartElement("simpleChoice");
        xmlWriter.writeAttribute("identifier", this.getId());

        this.question.exportQTI21TextBlock(xmlWriter, this.text);
        xmlWriter.writeEndElement(); // simpleChoice
    }

    public void exportQTI21TrueFalseAlternative(XMLWriter xmlWriter) throws XMLStreamException {
        xmlWriter.writeStartElement("simpleChoice");
        xmlWriter.writeAttribute("identifier", this.getId());
        String aText = this.text.toLowerCase();
        boolean isTrue = aText.contains("true");
        boolean isFalse = aText.contains("false");
        if (isTrue == isFalse) {
            SLF4J.LOGGER.error("Alternative {} not suitable for True/False-question {}", this.text, this.question.getPartialName());
        }
        if (this.question.getQuestionBank().getLanguage().startsWith("Dut")) {
            this.question.exportQTI21TextBlock(xmlWriter, isTrue ? "Juist" : "Onjuist");
        } else {
            this.question.exportQTI21TextBlock(xmlWriter, isTrue ? "True" : "False");
        }
        xmlWriter.writeEndElement(); // simpleChoice
    }

    public double getCorrectness() {
        return correctness;
    }

    public void setCorrectness(double correctness) {
        this.correctness = correctness;
    }

    public String getId() {
        return "a_" + this.id;
    }

    public String getText() {
        return text;
    }

    public double getTolerance() {
        return tolerance;
    }

    public int getToleranceType() {
        return toleranceType;
    }
}
