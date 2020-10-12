import utils.XMLParser;

import javax.xml.stream.XMLStreamException;

public class DragObject {

    private int id;
    private String text;
    private int group;
    private boolean infinite;
    private Image image;

    public static DragObject parseMXML(XMLParser xmlParser, Question question) throws XMLStreamException {
        if (xmlParser.nextBeginTag("drag")) {
            DragObject drag = new DragObject();

            drag.id = xmlParser.acceptOptionalElementValue("no", 0);
            drag.text = xmlParser.acceptOptionalElementValue("text", null);
            drag.group = xmlParser.acceptOptionalElementValue("draggroup", 0);
            drag.infinite = xmlParser.acceptOptionalElementValue("infinite", true);
            drag.image = Image.parseMXML(xmlParser, question.getQuestionBank(), null);
            xmlParser.findAndAcceptEndTag("drag");
            return drag;
        }
        return null;
    }
}
