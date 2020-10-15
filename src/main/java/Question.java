import utils.SLF4J;
import utils.XMLParser;
import utils.XMLWriter;

import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Question {

    public static final String SELECTION_BASETYPE = "identifier";
    //public static final String SELECTION_BASETYPE = "string";
    public static final double PARTIAL_CORRECTNESS = 50;
    private static final int RESERVED_ID_MIN = 630001;
    private static final int RESERVED_ID_MAX = 639999;
    private static int nextIdNumber = RESERVED_ID_MIN;

    private Category category;
    protected String name;
    protected String questionText;

    protected String generalFeedback;
    protected double defaultGrade;
    protected double penalty;
    protected int hidden = 0;
    protected int idNumber = 0;

    protected List<Answer> answers = null;

    protected String correctFeedback;
    protected String partiallyCorrectFeedback;
    protected String incorrectFeedback;
    protected boolean showNumCorrect;

    protected Set<Parameter> parameters = new HashSet<>();

    private List<String> fileReferences = new ArrayList<>();


    public Question(Category category) {
        this.category = category;
        category.add(this);
    }

    public void add(Parameter param) {
        this.parameters.add(param);
    }

    public String getQuestionType() {
        String type = this.getClass().getSimpleName().replace("Question", "");
        if (type.isEmpty()) type = "Cloze";
        return type;
    }
    public String getInteractionType() {
        return QuestionBank.QTI_MIXED_INTERACTION;
    }

    public String getFullName() {
        return this.category.getFullName() + "/" + this.getFlatName();
    }
    public String getPartialName() {
        String partialName = this.category.getFullName() + "/" + this.getFlatName();
        int numFolders = partialName.length() - partialName.replace("/", "").length();
        if (numFolders > 2) {
            for (int c = 0; c < numFolders - 2; c++) {
                partialName = partialName.substring(partialName.indexOf('/')+1);
            }
            partialName = ".../" + partialName;
        }
        return partialName;
    }
    public String getQuestionId() {
        return this.getQuestionType() + this.idNumber;
    }
    public String getQuestionPath() {
        return this.category.getFullName() + "/" + this.getQuestionId() + "_" + this.getFlatName()+ ".xml";
    }

    public void parseSetupFromMXML(XMLParser xmlParser) throws XMLStreamException {

        this.name = this.getQuestionBank().parseFormattedElementFromMXML(xmlParser, "name", "new question");
        this.questionText = this.getQuestionBank().parseFormattedElementFromMXML(xmlParser, "questiontext", this.getPartialName());
        this.generalFeedback = this.getQuestionBank().parseFormattedElementFromMXML(xmlParser, "generalfeedback", this.getPartialName());
        this.defaultGrade = xmlParser.acceptOptionalElementValue("defaultgrade", 1.0);
        this.penalty = xmlParser.acceptOptionalElementValue("penalty", 0.0);
        this.hidden = xmlParser.acceptOptionalElementValue("hidden", 0);
        this.idNumber = xmlParser.acceptOptionalElementValue("idnumber", 0);
        if (this.idNumber == 0L || (this.idNumber >= RESERVED_ID_MIN && this.idNumber <= RESERVED_ID_MAX)) {
            this.idNumber = nextIdNumber++;
        }
    }

    public void parseAnswersFromMXML(XMLParser xmlParser) throws XMLStreamException {

        this.answers = new ArrayList<>();
        xmlParser.findBeginTag("answer");
        Answer answer;
        while ((answer = Answer.parseFromMXML(xmlParser, this)) != null) {
            this.answers.add(answer);
        }
    }

    public void parseFeedbackFromMXML(XMLParser xmlParser) throws XMLStreamException {

        this.correctFeedback = this.getQuestionBank().parseFormattedElementFromMXML(xmlParser, "correctfeedback", this.getPartialName());
        this.partiallyCorrectFeedback = this.getQuestionBank().parseFormattedElementFromMXML(xmlParser, "partiallycorrectfeedback", this.getPartialName());
        this.incorrectFeedback = this.getQuestionBank().parseFormattedElementFromMXML(xmlParser, "incorrectfeedback", this.getPartialName());
        this.showNumCorrect = xmlParser.acceptOptionalElementValue("shownumcorrect", false);
    }

    public QuestionBank getQuestionBank() {
        return this.category.getQuestionBank();
    }

    public void exportQTI21(XMLWriter manifest, String exportFolder) throws XMLStreamException {
        if (this.hidden > 0) return;

        String questionPath = this.getQuestionPath();
        String questionExportPath = exportFolder + "/" + questionPath;

        this.fileReferences.clear();
        XMLWriter xmlWriter = new XMLWriter(questionExportPath);
        xmlWriter.writeStartDocument("utf-8", "1.0");
     //   xmlWriter.writeAttribute("standalone", "yes");
        xmlWriter.writeStartElement("assessmentItem");
        xmlWriter.writeAttribute("identifier", this.getQuestionId());
        xmlWriter.writeAttribute("title", this.name);
        xmlWriter.writeAttribute("timeDependent", "false");
        xmlWriter.writeAttribute("adaptive", "false");
        xmlWriter.writeAttribute("toolName", QuestionBank.QTI_TOOL_NAME);
        xmlWriter.writeAttribute("toolVersion", QuestionBank.QTI_TOOL_VERSION);
        xmlWriter.writeAttribute("\n\txmlns", "http://www.imsglobal.org/xsd/imsqti_v2p1");

        this.exportQTI21ResponseDeclaration(xmlWriter);
        this.exportQTI21OutcomeDeclaration(xmlWriter);

        for (String ss : QuestionBank.MOODLE_STYLE_SHEETS) {
            xmlWriter.writeEmptyElement("stylesheet");
            xmlWriter.writeAttribute("href", ss);
            xmlWriter.writeAttribute("type", "text/css");
        }

        xmlWriter.writeStartElement("itemBody");

        this.exportQTI21TextBlock(xmlWriter, this.questionText);

        this.exportQTI21QuestionClosing(xmlWriter);

        this.exportQTI21QuestionChoices(xmlWriter);

        xmlWriter.writeEndElement(); // itembody

        this.exportQTI21ResponseProcessing(xmlWriter);

        xmlWriter.writeEndElement(); // assessmentItem
        xmlWriter.writeEndDocument();

        manifest.writeStartElement("resource");
        manifest.writeAttribute("identifier", this.getQuestionId());
        manifest.writeAttribute("type", "imsqti_item_xmlv2p1");
        manifest.writeAttribute("href", questionPath);

        // optional ???
        manifest.writeStartElement("metadata");
        manifest.writeStartElement("schema");
        manifest.writeCharacters("MS QTI Item");
        manifest.writeEndElement(); // schema
        manifest.writeStartElement("schemaversion");
        manifest.writeCharacters("2.0");
        manifest.writeEndElement(); // schemaversion
        manifest.writeStartElement(QuestionBank.XML_QTI_NAMESPACE, "qtiMetadata");
        manifest.writeStartElement(QuestionBank.XML_QTI_NAMESPACE, "timeDependent");
        manifest.writeCharacters("false");
        manifest.writeEndElement();
        manifest.writeStartElement(QuestionBank.XML_QTI_NAMESPACE, "interactionType");
        manifest.writeCharacters(this.getInteractionType());
        manifest.writeEndElement();
        manifest.writeEndElement(); // qtiMetaData
        manifest.writeEndElement(); // metadata

        //manifest.writeEmptyElement("file");
        //manifest.writeAttribute("href", "css/from_moodle.css");
        manifest.writeEmptyElement("file");
        manifest.writeAttribute("href", questionPath);

        for (String ss : QuestionBank.MOODLE_STYLE_SHEETS) {
            manifest.writeEmptyElement("file");
            manifest.writeAttribute("href", ss);
        }

        for (String f : this.fileReferences) {
            manifest.writeEmptyElement("file");
            manifest.writeAttribute("href", f);
        }
        manifest.writeEndElement(); // resource
    }

    public void exportQTI21ResponseDeclaration(XMLWriter xmlWriter) throws XMLStreamException {
    }

    public void exportQTI21OutcomeDeclaration(XMLWriter xmlWriter) throws XMLStreamException {
        xmlWriter.writeEmptyElement("outcomeDeclaration");
        xmlWriter.writeAttribute("identifier", "SCORE");
        xmlWriter.writeAttribute("cardinality", "single");
        xmlWriter.writeAttribute("baseType", "float");
        xmlWriter.writeAttribute("normalMinimum", "0.0");
        xmlWriter.writeAttribute("normalMaximum", String.valueOf(this.defaultGrade));
        //xmlWriter.writeEndElement(); // outcomeDeclaration
    }

    public void exportQTI21ResponseProcessing(XMLWriter xmlWriter) throws XMLStreamException {
        xmlWriter.writeEmptyElement("responseProcessing");
        xmlWriter.writeAttribute("templateLocation", "/templates/RPTEMPLATE_SCORE.xml");
    }

    public void exportQTI21TextBlock(XMLWriter xmlWriter, String textBlock) throws XMLStreamException {
        xmlWriter.writeStartElement("div");
        xmlWriter.writeAttribute("id", QuestionBank.TV_TEXTBLOCK_ID + QuestionBank.getNextTextBlockId());
        xmlWriter.writeAttribute("class", QuestionBank.TV_TEXTBLOCK_CLASS + " moodletext");
        xmlWriter.writeStartElement("div");
        xmlWriter.writeAttribute("class", QuestionBank.TV_TBZONE_CLASS);
        String qtiText = this.resolveFileReferences(textBlock);
        qtiText = QuestionBank.fixHTMLforQTI21(qtiText, this.getPartialName());
        qtiText = qtiText.replace("</p><","</p>\n<");

        xmlWriter.writeRawCharacters(qtiText);
        xmlWriter.writeEndElement(); // div
        xmlWriter.writeEndElement(); // div
    }

    public void exportQTI21QuestionClosing(XMLWriter xmlWriter) throws XMLStreamException {
    }

    private String resolveFileReferences(String text) {
        String mediaPath = this.getQuestionBank().rootCategory.getMediaFilesFolder();
        int i2 = 0;
        int i1;
        while (0 <= (i1 = text.indexOf("@@PLUGINFILE@@", i2))) {
            i2 = text.indexOf("\"", i1+14);
            if (i2 < 0) continue;
            this.addFileReference(text.substring(i1+14, i2));
        }
        return text.replace("@@PLUGINFILE@@", mediaPath);
    }

    public String addFileReference(String fileRef) {
        String mediaPath = this.getQuestionBank().rootCategory.getMediaFilesFolder();
        String mediaRef = mediaPath + fileRef.replace("%20"," ");
        this.fileReferences.add(mediaRef);
        return mediaRef;
    }

    public void exportQTI21QuestionChoices(XMLWriter xmlWriter) throws XMLStreamException {

    }

    public String getFlatName() {
        return this.name.replace(' ','_').replaceAll("[^A-Za-z0-9_]","");
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public int getHidden() {
        return hidden;
    }

    public void setHidden(int hidden) {
        this.hidden = hidden;
    }

    public String getQuestionText() {
        return questionText;
    }
}