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
            String format = QuestionBank.parseFormatAttributeFromMXML(xmlParser);
            xmlParser.nextTag();
            answer.text = QuestionBank.parseFormattedTextFromMXML(xmlParser, format);
            answer.feedback = question.getQuestionBank().parseFormattedElementFromMXML(xmlParser, "feedback");
            answer.tolerance = xmlParser.acceptOptionalElementValue("tolerance", 0.0);
            answer.toleranceType = xmlParser.acceptOptionalElementValue("tolerancetype", 0);
            answer.correctAnswerFormat = xmlParser.acceptOptionalElementValue("correctanswerformat", 0);
            answer.correctAnswerLength = xmlParser.acceptOptionalElementValue("correctanswerlength", 0);
            if (answer.feedback == null) {
                answer.feedback = question.getQuestionBank().parseFormattedElementFromMXML(xmlParser, "feedback");
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

    public double getCorrectness() {
        return correctness;
    }

    public void setCorrectness(double correctness) {
        this.correctness = correctness;
    }

    public String getId() {
        return "a_" + this.id;
    }
}
