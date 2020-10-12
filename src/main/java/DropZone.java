import utils.XMLParser;

import javax.xml.stream.XMLStreamException;

public class DropZone {
    private int id;
    private String text;
    private int choice;
    private int xLeft;
    private int yTop;

    public static DropZone parseMXML(XMLParser xmlParser) throws XMLStreamException {
        if (xmlParser.nextBeginTag("drop")) {
            DropZone drop = new DropZone();

            drop.text = xmlParser.acceptOptionalElementValue("text", null);
            drop.id = xmlParser.acceptOptionalElementValue("no", 0);

            drop.choice = xmlParser.acceptOptionalElementValue("choice", 0);
            drop.xLeft = xmlParser.acceptOptionalElementValue("xleft", 0);
            drop.yTop = xmlParser.acceptOptionalElementValue("ytop", 0);
            xmlParser.findAndAcceptEndTag("drop");
            return drop;
        }
        return null;
    }
}
