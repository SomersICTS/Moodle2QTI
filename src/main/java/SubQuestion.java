import utils.XMLParser;

import javax.xml.stream.XMLStreamException;

public class SubQuestion {

    private static int nextId = 30001;

    private int id = nextId++;
    private String text;
    private String answer;

    public static SubQuestion parseFromMXML(XMLParser xmlParser, Question question) throws XMLStreamException {
        if (xmlParser.nextBeginTag("subquestion")) {
            SubQuestion sub = new SubQuestion();

            String format = QuestionBank.parseFormatAttributeFromMXML(xmlParser, question.getPartialName());
            sub.text = QuestionBank.parseFormattedTextFromMXML(xmlParser, format, question.getPartialName());
            sub.answer = question.getQuestionBank().parseFormattedElementFromMXML(xmlParser, "answer", question.getPartialName());

            xmlParser.findAndAcceptEndTag("subquestion");
            return sub;
        }
        return null;
    }

    public String getIdText() {
        return "q_" + this.id;
    }

    public String getText() {
        return this.text;
    }

    public String getAnswer() {
        return answer;
    }
}
