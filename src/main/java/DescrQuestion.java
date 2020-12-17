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
}
