import utils.SLF4J;
import utils.XMLParser;
import utils.XMLWriter;

import javax.xml.stream.XMLStreamException;
import java.util.*;

public class DDQuestion extends Question {

    protected Image target;
    protected List<DragObject> dragObjects = null;
    protected List<DropZone> dropZones = null;

    public DDQuestion(Category category) {
        super(category);

        this.dragObjects = new ArrayList<>();
        this.dropZones = new ArrayList<>();
    }

    @Override
    public String getInteractionType() {
        return QuestionBank.QTI_HOTSPOT_INTERACTION;
    }

    public static Question createFromMXML(XMLParser xmlParser, Category category) throws XMLStreamException {
        DDQuestion ddQuestion = new DDQuestion(category);

        ddQuestion.parseSetupFromMXML(xmlParser);


        SLF4J.LOGGER.debug("Created DD-question: '{}'", ddQuestion.getFullName());

        return ddQuestion;
    }

    public void parseSetupFromMXML(XMLParser xmlParser) throws XMLStreamException {

        super.parseSetupFromMXML(xmlParser);

        super.parseFeedbackFromMXML(xmlParser);

        this.target = Image.parseMXML(xmlParser, this.getQuestionBank(), null);

        DragObject dragObject;
        while ((dragObject = DragObject.parseMXML(xmlParser, this)) != null) {
            this.dragObjects.add(dragObject);
        }
        DropZone dropZone;
        while ((dropZone = DropZone.parseMXML(xmlParser)) != null) {
            this.dropZones.add(dropZone);
        }

        if (this.dragObjects.size() == 0 || this.dropZones.size() == 0) {
            SLF4J.LOGGER.warn("No dragobjects or dropzones found in '{}'", this.getPartialName());
        }
    }

    private Map<Integer,Integer> dragUsageCount;
    private int uniqueZones = 0;

    @Override
    public void exportQTI21ResponseDeclaration(XMLWriter xmlWriter) throws XMLStreamException {
        this.dragUsageCount = new HashMap<>();
        for (DropZone dz : this.dropZones)
            this.dragUsageCount.merge(dz.getChoice(), 1, Integer::sum);

        this.uniqueZones = 0;

        for (DropZone dz : this.dropZones)
            if (this.dragUsageCount.get(dz.getChoice()) == 1) {
                this.uniqueZones++;
                dz.setResponseNr(this.uniqueZones);
                this.dragObjects.get(dz.getChoice()-1).setResponseNr(this.uniqueZones);
            } else {
                dz.setResponseNr(0);
            }

        for (DropZone dz : this.dropZones)
            if (dz.getResponseNr() > 0) {
                dz.exportQTI21ResponseDeclaration(xmlWriter, 1.0, 1.0/this.uniqueZones);
            }

        if (this.uniqueZones == 0) {
            SLF4J.LOGGER.error("No drag items with unique solution found in '{}'", this.getPartialName());
        } else if (this.uniqueZones < this.dropZones.size()) {
            SLF4J.LOGGER.warn("Removed drag items with multiple solutions from '{}'", this.getPartialName());
        }
    }

    @Override
    public void exportQTI21QuestionChoices(XMLWriter xmlWriter) throws XMLStreamException {
        String targetFile = this.addFileReference(this.target.getVersionedFullURL());
        xmlWriter.writeStartElement("positionObjectStage");
        xmlWriter.writeStartElement("object");
        xmlWriter.writeAttribute("type", "image/png");
        xmlWriter.writeAttribute("data", targetFile);
        xmlWriter.writeAttribute("width", "884");
        xmlWriter.writeAttribute("height", "376");
        xmlWriter.writeAttribute("class", "tvimg");
        xmlWriter.writeEndElement(); // object
        for (DropZone dz : this.dropZones)
            if (dz.getResponseNr() > 0) {
                DragObject dragObject = this.dragObjects.get(dz.getChoice()-1);
                dragObject.exportQTI21QuestionChoices(xmlWriter, this);
            }
        xmlWriter.writeEndElement(); // positionObjectStage
    }

    @Override
    public void exportQTI21ResponseProcessing(XMLWriter xmlWriter) throws XMLStreamException {
        xmlWriter.writeEmptyElement("responseProcessing");
        xmlWriter.writeAttribute("templateLocation",
                String.format("/templates/RPTEMPLATE_POINTS.xml", this.dropZones.size()));
    }
}
