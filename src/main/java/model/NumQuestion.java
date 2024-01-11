package model;

import utils.SLF4J;
import utils.XMLParser;

import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;

public class NumQuestion extends SAQuestion {

    private int unitgradingtype;
    private double unitpenalty;
    private int showunits;
    private int unitsleft;

    public NumQuestion(Category category, String saKind) {
        super(category, saKind);
    }

    public static Question createFromMXML(XMLParser xmlParser, Category category, String saKind) throws XMLStreamException {
        NumQuestion numQuestion = new NumQuestion(category, saKind);

        numQuestion.parseSetupFromMXML(xmlParser);

        SLF4J.LOGGER.info("Created {}-question: '{}'", saKind, numQuestion.name);

        return numQuestion;
    }

    @Override
    public void parseSetupFromMXML(XMLParser xmlParser) throws XMLStreamException {

        super.parseSetupFromMXML(xmlParser);

        this.parseGradingFromMXML(xmlParser);

        if (xmlParser.nextBeginTag("dataset_definitions")) {
            xmlParser.nextTag();
            Parameter param;
            while ((param = Parameter.parseFromMXML(xmlParser, this)) != null) {
                // defer calculated questions for now
                SLF4J.LOGGER.error("Disabled {}-question: '{}'", this.getSaKind(), this.name);
                this.setHidden(1);
            }
            xmlParser.findAndAcceptEndTag("dataset_definitions");
        }
    }

    private void parseGradingFromMXML(XMLParser xmlParser) throws XMLStreamException {
        this.unitgradingtype = xmlParser.acceptOptionalElementValue("unitgradingtype", 0);
        this.unitpenalty = xmlParser.acceptOptionalElementValue("unitpenalty", 0.0);
        this.showunits = xmlParser.acceptOptionalElementValue("showunits", 0);
        this.unitsleft = xmlParser.acceptOptionalElementValue("unitsleft", 0);
    }

    @Override
    public void parseAnswersFromMXML(XMLParser xmlParser) throws XMLStreamException {

        if (this.getSaKind().startsWith("calculated")) {
            super.parseFeedbackFromMXML(xmlParser);
        }
        super.parseAnswersFromMXML(xmlParser);
    }

    @Override
    protected String expandAnswers() {
        super.expandAnswers();
        return "float";
    }
}
