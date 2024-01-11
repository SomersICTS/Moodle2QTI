package model;

import utils.SLF4J;
import utils.XMLParser;
import utils.XMLWriter;

import javax.xml.stream.XMLStreamException;
import java.util.*;

public class Question {

    public static final String SELECTION_BASETYPE = "identifier";
    //public static final String SELECTION_BASETYPE = "string";
    public static final double PARTIAL_CORRECTNESS = 50;
    public static final int MAX_DEFAULT_GRADE = 10;
    private static final int MAX_DISPLAY_NAME_LENGTH = 80;
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
        String partialName = this.category.getFullName() + "/" + this.getQuestionId() + "_" + this.getFlatName();
        int firstSlashPosition = partialName.indexOf('/');
        while (partialName.length() > MAX_DISPLAY_NAME_LENGTH && firstSlashPosition > 0) {
            partialName = "..." + partialName.substring(firstSlashPosition);
            firstSlashPosition = partialName.indexOf('/', 4);
        }
        /*
        int numFolders = partialName.length() - partialName.replace("/", "").length();
        if (numFolders > 2) {
            for (int c = 0; c < numFolders - 2; c++) {
                partialName = partialName.substring(partialName.indexOf('/')+1);
            }
            partialName = ".../" + partialName;
        }
        */
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
        double categoryMark = this.getCategory().getDefaultMark();
        if (categoryMark > 0) this.defaultGrade = categoryMark;

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
        if (this.correctFeedback.contains("Your answer is correct.") && this.correctFeedback.length() < (23+10))
            this.correctFeedback = null;

        this.partiallyCorrectFeedback = this.getQuestionBank().parseFormattedElementFromMXML(xmlParser, "partiallycorrectfeedback", this.getPartialName());
        if (this.partiallyCorrectFeedback.contains("Your answer is partially correct.") && this.partiallyCorrectFeedback.length() < (33+10) )
            this.partiallyCorrectFeedback = null;

        this.incorrectFeedback = this.getQuestionBank().parseFormattedElementFromMXML(xmlParser, "incorrectfeedback", this.getPartialName());
        if (this.incorrectFeedback.contains("Your answer is incorrect.") && this.incorrectFeedback.length() < (25+10))
            this.incorrectFeedback = null;

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
        //xmlWriter.writeStartDocument("utf-8", "1.0\" standalone=\"yes");
        xmlWriter.writeStartElement("assessmentItem");
        xmlWriter.writeAttribute("identifier", this.getQuestionId());
        xmlWriter.writeAttribute("title", this.name);
        xmlWriter.writeAttribute("timeDependent", "false");
        xmlWriter.writeAttribute("adaptive", "false");
        xmlWriter.writeAttribute("toolName", QuestionBank.QTI_TOOL_NAME);
        xmlWriter.writeAttribute("toolVersion", QuestionBank.QTI_TOOL_VERSION);
        xmlWriter.writeDefaultNamespace(QuestionBank.XML_IMSQTI_NAMESPACE);
        //xmlWriter.writeAttribute("\n\txmlns", "http://www.imsglobal.org/xsd/imsqti_v2p1");

        this.exportQTI21ResponseDeclaration(xmlWriter);
        this.exportQTI21OutcomeDeclaration(xmlWriter);
        this.exportQTI21FeedbackOutcomeDeclaration(xmlWriter);

        for (String ss : QuestionBank.MOODLE_STYLE_SHEETS) {
            xmlWriter.writeEmptyElement("stylesheet");
            xmlWriter.writeAttribute("href", ss);
            xmlWriter.writeAttribute("type", "text/css");
        }

        xmlWriter.writeStartElement("itemBody");

        this.exportQTI21QuestionText(xmlWriter);

        this.exportQTI21QuestionChoices(xmlWriter);

        this.exportQTI21QuestionRubricBlock(xmlWriter);

        xmlWriter.writeEndElement(); // itembody

        this.exportQTI21ResponseProcessing(xmlWriter);

        this.exportQTI21Feedback(xmlWriter);

        xmlWriter.writeEndElement(); // assessmentItem
        xmlWriter.writeEndDocument();

        manifest.writeStartElement("resource");
        manifest.writeAttribute("identifier", this.getQuestionId());
        manifest.writeAttribute("type", "imsqti_item_xmlv2p1");
        manifest.writeAttribute("href", questionPath);

        // optional ???
        /*
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
        */

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

    private static boolean isNullOrEmpty(String t) {
        return t == null || t.isEmpty();
    }
    private boolean hasFeedback() {
        return !isNullOrEmpty(this.generalFeedback) ||
                !isNullOrEmpty(this.correctFeedback) ||
                !isNullOrEmpty(this.partiallyCorrectFeedback) ||
                !isNullOrEmpty(this.incorrectFeedback);
    }
    public void exportQTI21OutcomeDeclaration(XMLWriter xmlWriter) throws XMLStreamException {
        xmlWriter.writeEmptyElement("outcomeDeclaration");
        xmlWriter.writeAttribute("identifier", "SCORE");
        xmlWriter.writeAttribute("cardinality", "single");
        xmlWriter.writeAttribute("baseType", "float");
        xmlWriter.writeAttribute("normalMinimum", "0");
        xmlWriter.writeAttribute("normalMaximum", String.valueOf((int)(this.defaultGrade+0.99)));
    }

    public void exportQTI21FeedbackOutcomeDeclaration(XMLWriter xmlWriter) throws XMLStreamException {
        if (!this.hasFeedback()) return;
        xmlWriter.writeEmptyElement("outcomeDeclaration");
        xmlWriter.writeAttribute("identifier", "FEEDBACK");
        xmlWriter.writeAttribute("cardinality", "single");
        xmlWriter.writeAttribute("baseType", "identifier");
    }

    public void exportQTI21ResponseProcessing(XMLWriter xmlWriter) throws XMLStreamException {
    }

    public void exportQTI21Feedback(XMLWriter xmlWriter) throws XMLStreamException {
        this.exportQTI21Feedback(xmlWriter, "ANSWER_CORRECT", this.correctFeedback);
        this.exportQTI21Feedback(xmlWriter, "FAILURE", this.incorrectFeedback);
        this.exportQTI21Feedback(xmlWriter, "QUESTION_FEEDBACK", this.generalFeedback);
        this.exportQTI21Feedback(xmlWriter, "PARTIAL_CORRECT", this.partiallyCorrectFeedback);
    }

    private void exportQTI21Feedback(XMLWriter xmlWriter, String fbKind, String fbText) throws XMLStreamException {
        if (isNullOrEmpty(fbText)) return;
        xmlWriter.writeStartElement("modalFeedback");
        xmlWriter.writeAttribute("outcomeIdentifier", "FEEDBACK");
        xmlWriter.writeAttribute("showHide", "show");
        xmlWriter.writeAttribute("identifier", fbKind);
        xmlWriter.writeStartElement("div");
        xmlWriter.writeAttribute("id", QuestionBank.TV_TEXTBLOCK_ID + QuestionBank.getNextTextBlockId());
        xmlWriter.writeAttribute("class", QuestionBank.TV_TEXTBLOCK_CLASS + " moodletext");
        xmlWriter.writeStartElement("div");
        xmlWriter.writeAttribute("class", QuestionBank.TV_TBZONE_CLASS);
        String qtiText = this.resolveFileReferences(fbText);
        qtiText = QuestionBank.fixHTMLforQTI21(qtiText, this.getPartialName());
        xmlWriter.writeRawCharacters(qtiText);
        xmlWriter.writeEndElement(); // div
        xmlWriter.writeEndElement(); // div
        xmlWriter.writeEndElement(); // modalFeedback
    }

    public void exportQTI21QuestionText(XMLWriter xmlWriter) throws XMLStreamException {
        this.exportQTI21TextBlock(xmlWriter, this.questionText);
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

    public void exportQTI21QuestionRubricBlock(XMLWriter xmlWriter) throws XMLStreamException {
        if (this.name.toLowerCase().contains("[proef]")) {
            xmlWriter.writeStartElement("rubricBlock");
            xmlWriter.writeAttribute("view", "testConstructor");
            xmlWriter.writeStartElement("dl");
            xmlWriter.writeStartElement("dt");
            xmlWriter.writeCharacters("Sprints");
            xmlWriter.writeEndElement(); // dt
            xmlWriter.writeStartElement("dd");
            xmlWriter.writeCharacters("Oefentoets");
            xmlWriter.writeEndElement(); // dd
            xmlWriter.writeEndElement(); // dl
            xmlWriter.writeEndElement(); // rubricBlock
        }
    }

    private String resolveFileReferences(String text) {
        int i2 = 0;
        int i1;
        while (0 <= (i1 = text.indexOf("@@PLUGINFILE@@", i2))) {
            i2 = text.indexOf("\"", i1+14);
            if (i2 < 0) continue;
            String replacement = this.addFileReference(text.substring(i1+14, i2));
            text = text.substring(0,i1) + replacement + text.substring(i2);
            i2 = i1 + replacement.length();
        }
        return text;
    }

    public String addFileReference(String fileRef) {
        String mediaPath = this.getQuestionBank().rootCategory.getMediaFilesFolder();
        String mediaRef = mediaPath + fileRef;
                // fileRef.replace(" ","_").replace("%20","_").replace("%28","(").replace("%29",")");
        this.fileReferences.add(mediaRef);
        return mediaRef;
    }

    public void exportQTI21QuestionChoices(XMLWriter xmlWriter) throws XMLStreamException {
    }

    public void registerScore(Map<String,int[]> scoresCounts) {
        String type = this.getQuestionType();
        int score = (int)this.defaultGrade;
        if (Math.abs(this.defaultGrade - score) > 0.1)
            SLF4J.LOGGER.warn("Non-integer grade {} in '{}'", this.defaultGrade, this.getPartialName());

        if (score >= 0 && score <= MAX_DEFAULT_GRADE) {
            int[] scores = scoresCounts.get(type);
            if (scores == null) {
                scores = new int[MAX_DEFAULT_GRADE+1];
                scoresCounts.put(type, scores);
            }
            scores[score]++;
        }
    }

    public void registerDeviatingScore(Map<String,Integer> defaultScores, List<String> deviations) {
        String type = this.getQuestionType();
        int score = (int)this.defaultGrade;

        if (score >= 0 && score <= MAX_DEFAULT_GRADE && score != defaultScores.get(type)) {
            deviations.add(this.getPartialName());
        }
    }

    public String getFlatName() {
        String flatName = this.name.replace(' ','_').replaceAll("[^A-Za-z0-9_]","");
        if (flatName.length() > 60) {
            flatName = flatName.substring(0,15) + "(" + flatName.hashCode() + ")" + flatName.substring(flatName.length()-30);
        }
        return flatName;
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