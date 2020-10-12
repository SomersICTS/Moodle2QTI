import utils.SLF4J;
import utils.XMLParser;

import javax.xml.stream.XMLStreamException;

public class NumQuestion extends SAQuestion {

    private int unitgradingtype;
    private double unitpenalty;
    private int showunits;
    private int unitsleft;

    public NumQuestion(Category category, String type) {
        super(category, type);
    }

    public static Question createFromMXML(XMLParser xmlParser, Category category, String type) throws XMLStreamException {
        NumQuestion numQuestion = new NumQuestion(category, type);

        numQuestion.parseSetupFromMXML(xmlParser);

        SLF4J.LOGGER.info("Created {}-question: '{}'", type, numQuestion.name);

        return numQuestion;
    }

    public void parseSetupFromMXML(XMLParser xmlParser) throws XMLStreamException {

        super.parseSetupFromMXML(xmlParser);

        this.unitgradingtype = xmlParser.acceptOptionalElementValue("unitgradingtype", 0);
        this.unitpenalty = xmlParser.acceptOptionalElementValue("unitpenalty", 0.0);
        this.showunits = xmlParser.acceptOptionalElementValue("showunits", 0);
        this.unitsleft = xmlParser.acceptOptionalElementValue("unitsleft", 0);

        if (xmlParser.nextBeginTag("dataset_definitions")) {
            Parameter param;
            while ((param = Parameter.parseFromMXML(xmlParser, this)) != null) { ; }
        }
    }
}
