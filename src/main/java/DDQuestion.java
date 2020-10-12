import utils.SLF4J;
import utils.XMLParser;

import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.List;

public class DDQuestion extends Question {

    protected Image target;
    protected List<DragObject> dragObjects = null;
    protected List<DropZone> dropZones = null;

    public DDQuestion(Category category) {
        super(category);

        this.dragObjects = new ArrayList<>();
        this.dropZones = new ArrayList<>();
    }

    public static Question createFromMXML(XMLParser xmlParser, Category category) throws XMLStreamException {
        DDQuestion ddQuestion = new DDQuestion(category);

        ddQuestion.parseSetupFromMXML(xmlParser);


        SLF4J.LOGGER.debug("Created DD-question: '{}'", ddQuestion.name);

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
    }
}
