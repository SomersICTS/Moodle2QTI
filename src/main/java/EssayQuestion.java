import utils.SLF4J;
import utils.XMLParser;

import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.List;

public class EssayQuestion extends Question {

    private String responseFormat;
    private boolean responseRequired;
    private int responseFieldLines;
    private boolean attachmentsAllowed;
    private boolean attachmentsRequired;
    private String graderInfo;
    private String responseTemplate;

    public EssayQuestion(Category category) {
        super(category);
    }

    @Override
    public String getInteractionType() {
        return QuestionBank.QTI_SHORTANSWER_INTERACTION;
    }

    public static Question createFromMXML(XMLParser xmlParser, Category category) throws XMLStreamException {
        EssayQuestion essayQuestion = new EssayQuestion(category);

        essayQuestion.parseSetupFromMXML(xmlParser);


        SLF4J.LOGGER.info("Created Essay-question: '{}'", essayQuestion.name);

        return essayQuestion;
    }

    public void parseSetupFromMXML(XMLParser xmlParser) throws XMLStreamException {

        super.parseSetupFromMXML(xmlParser);

        this.responseFormat = xmlParser.findAndAcceptElementValue("responseformat", null);
        this.responseRequired = (xmlParser.findAndAcceptElementValue("responserequired", 0) > 0);
        this.responseFieldLines = xmlParser.findAndAcceptElementValue("responsefieldlines", 0);
        this.attachmentsAllowed = (xmlParser.findAndAcceptElementValue("attachments", 0) > 0);
        this.attachmentsRequired = (xmlParser.findAndAcceptElementValue("attachmentsrequired", 0) > 0);
        this.graderInfo = this.getQuestionBank().parseFormattedElementFromMXML(xmlParser, "graderinfo", this.getPartialName());
        this.responseTemplate = this.getQuestionBank().parseFormattedElementFromMXML(xmlParser, "responsetemplate", this.getPartialName());
    }
}
