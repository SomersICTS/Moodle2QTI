import utils.XMLParser;

import javax.xml.stream.XMLStreamException;
import java.util.Objects;

public class Parameter {

    private Question question;
    private String name;
    private String type;
    private double min;
    private double max;
    private int decimals;

    @Override
    public boolean equals(Object o) {
        return (this.question == ((Parameter)o).question) &&
                this.name.equals(((Parameter)o).name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.question, this.name);
    }

    public Parameter(String name, Question question) {
        this.name = name;
        this.question = question;
        question.add(this);
    }

    public static Parameter parseFromMXML(XMLParser xmlParser, Question question) throws XMLStreamException {
        if (xmlParser.nextBeginTag("dataset_definition")) {
            String name = question.getQuestionBank().parseFormattedElementFromMXML(xmlParser, "name", question.getPartialName());
            Parameter param = new Parameter(name, question);
            param.type = xmlParser.findAndAcceptElementValue("type", null);
            if (xmlParser.findBeginTag("minimum")) {
                param.min = xmlParser.findAndAcceptElementValue("text", 0.0);
                xmlParser.findAndAcceptEndTag("minimum");
            }
            if (xmlParser.findBeginTag("maximum")) {
                param.max = xmlParser.findAndAcceptElementValue("text", 0.0);
                xmlParser.findAndAcceptEndTag("maximum");
            }
            if (xmlParser.findBeginTag("decimals")) {
                param.decimals = xmlParser.findAndAcceptElementValue("text", 0);
                xmlParser.findAndAcceptEndTag("decimals");
            }
            xmlParser.findAndAcceptEndTag("dataset_definition");
            return param;
        }
        return null;
    }
}
