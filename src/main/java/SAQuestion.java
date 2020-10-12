import utils.SLF4J;
import utils.XMLParser;

import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.List;

public class SAQuestion extends Question {

    private boolean caseSensitive;
    private String type;
    private List<Answer> answers = null;

    public SAQuestion(Category category, String type) {
        super(category);

        this.type = type;
        this.answers = new ArrayList<>();
    }

    public static Question createFromMXML(XMLParser xmlParser, Category category, String type) throws XMLStreamException {
        SAQuestion saQuestion = new SAQuestion(category, type);

        saQuestion.parseSetupFromMXML(xmlParser);


        SLF4J.LOGGER.debug("Created {}-question: '{}'", type, saQuestion.name);

        return saQuestion;
    }

    public void parseSetupFromMXML(XMLParser xmlParser) throws XMLStreamException {

        super.parseSetupFromMXML(xmlParser);

        this.caseSensitive = (xmlParser.acceptOptionalElementValue("usecase", 0) > 0);

        Answer answer;
        while ((answer = Answer.parseFromMXML(xmlParser, this)) != null) {
            this.answers.add(answer);
        }
    }
}
