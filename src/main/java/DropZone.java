import utils.XMLParser;
import utils.XMLWriter;

import javax.xml.stream.XMLStreamException;
import java.util.Locale;

public class DropZone {
    private int id;
    private String text;
    private int choice;
    private int responseNr = 0;
    private int xLeft;
    private int yTop;
    private int xRight;
    private int yBottom;

    public static DropZone parseMXML(XMLParser xmlParser) throws XMLStreamException {
        if (xmlParser.nextBeginTag("drop")) {
            DropZone drop = new DropZone();

            xmlParser.nextTag();
            drop.text = xmlParser.acceptOptionalElementValue("text", null);
            drop.id = xmlParser.acceptOptionalElementValue("no", 0);

            drop.choice = xmlParser.acceptOptionalElementValue("choice", 0);
            drop.xLeft = xmlParser.acceptOptionalElementValue("xleft", 0);
            drop.yTop = xmlParser.acceptOptionalElementValue("ytop", 0);
            xmlParser.findAndAcceptEndTag("drop");
            drop.xRight = drop.xLeft + 32;
            drop.yBottom = drop.yTop + 32;
            return drop;
        }
        return null;
    }

    public int getChoice() {
        return choice;
    }

    public int getTargetX(double scale ) {
        return (int)(0.5 * scale * (this.xLeft+this.xRight));
    }
    public int getTargetY( double scale ) {
        return (int)(0.5 * scale * (this.yTop+this.yBottom));
    }

    public void exportQTI21ResponseDeclaration(XMLWriter xmlWriter, double scale, double mappedValue) throws XMLStreamException {
        xmlWriter.writeStartElement("responseDeclaration");
        xmlWriter.writeAttribute("identifier", String.format("RESPONSE_%02d", this.responseNr));
        xmlWriter.writeAttribute("cardinality", "single");
        xmlWriter.writeAttribute("baseType", "point");
        xmlWriter.writeStartElement("correctResponse");
        xmlWriter.writeStartElement("value");
        xmlWriter.writeCharacters(String.format("%d %d", this.getTargetX(scale), this.getTargetY(scale)));
        xmlWriter.writeEndElement(); // value
        xmlWriter.writeEndElement(); // correctResponse
        xmlWriter.writeStartElement("areaMapping");
        xmlWriter.writeAttribute("defaultValue", "0");
        xmlWriter.writeEmptyElement("areaMapEntry");
        xmlWriter.writeAttribute("shape", "rect");
        xmlWriter.writeAttribute("coords", String.format("%d,%d,%d,%d",
                (int)(scale * this.xLeft), (int)(scale * this.yTop), (int)(scale * this.xRight), (int)(scale * this.yBottom)));
        xmlWriter.writeAttribute("mappedValue", String.valueOf(mappedValue));
        xmlWriter.writeEndElement(); // areaMapping
        xmlWriter.writeEndElement(); // responseDeclaration
    }

    public int getResponseNr() {
        return responseNr;
    }

    public void setResponseNr(int responseNr) {
        this.responseNr = responseNr;
    }

}
