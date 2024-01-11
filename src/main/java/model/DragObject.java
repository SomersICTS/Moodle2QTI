package model;

import utils.XMLParser;
import utils.XMLWriter;

import javax.xml.stream.XMLStreamException;

public class DragObject {
    private static final String HOTSPOT_IMAGE = "hotspotimgexp01.jpg";

    private int id;
    private String text;
    private int group;
    private boolean infinite;
    private Image image;

    private int responseNr;


    public static DragObject parseMXML(XMLParser xmlParser, Question question) throws XMLStreamException {
        if (xmlParser.nextBeginTag("drag")) {
            DragObject drag = new DragObject();

            xmlParser.nextTag();
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

    public void exportQTI21QuestionChoices(XMLWriter xmlWriter, Question question) throws XMLStreamException {
        xmlWriter.writeStartElement("positionObjectInteraction");
        xmlWriter.writeAttribute("responseIdentifier", String.format("RESPONSE_%02d", this.responseNr));
        xmlWriter.writeAttribute("maxChoices", "1");
        if (this.text != null) {
            xmlWriter.writeAttribute("label", QuestionBank.escapeToHTMLEntities(this.text));
        }
        xmlWriter.writeEmptyElement("object");
        if (this.image == null) {
            xmlWriter.writeAttribute("type", "image/jpeg");
            String iconFile = question.getQuestionBank().rootCategory.getMediaFilesFolder() + "/" + HOTSPOT_IMAGE;
            xmlWriter.writeAttribute("data", iconFile);
            xmlWriter.writeAttribute("width", "32");
            xmlWriter.writeAttribute("height", "20");
            xmlWriter.writeAttribute("class", "tv_default_marker");
        } else {
            xmlWriter.writeAttribute("type", "image/png");
            String iconFile = question.addFileReference(this.image.getVersionedFullURL());
            xmlWriter.writeAttribute("data", iconFile);
            xmlWriter.writeAttribute("width", "32");
            xmlWriter.writeAttribute("height", "32");
            xmlWriter.writeAttribute("class", "tvimg");
        }
        xmlWriter.writeEndElement(); // positionObjectInteraction
    }

    public int getResponseNr() {
        return responseNr;
    }

    public void setResponseNr(int responseNr) {
        this.responseNr = responseNr;
    }

    public int getId() {
        return id;
    }
}
