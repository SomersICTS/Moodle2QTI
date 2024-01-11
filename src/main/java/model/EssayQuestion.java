package model;

import utils.SLF4J;
import utils.XMLParser;
import utils.XMLWriter;

import javax.xml.stream.XMLStreamException;

public class EssayQuestion extends Question {

    private String responseFormat;
    private boolean responseRequired;
    private int responseFieldLines;
    private int numAttachments;
    private boolean attachmentsRequired;
    private String graderInfo;
    private String responseTemplate;

    public EssayQuestion(Category category) {
        super(category);
    }

    @Override
    public String getInteractionType() {
        return QuestionBank.QTI_SHORTANSWER_INTERACTION;
    }

    public static Question createFromMXML(XMLParser xmlParser, Category category) throws XMLStreamException {
        EssayQuestion essayQuestion = new EssayQuestion(category);

        essayQuestion.parseSetupFromMXML(xmlParser);

        SLF4J.LOGGER.info("Created Essay-question: '{}'", essayQuestion.name);

        return essayQuestion;
    }

    public void parseSetupFromMXML(XMLParser xmlParser) throws XMLStreamException {

        super.parseSetupFromMXML(xmlParser);

        this.responseFormat = xmlParser.findAndAcceptElementValue("responseformat", null);
        this.responseRequired = (xmlParser.findAndAcceptElementValue("responserequired", 0) > 0);
        this.responseFieldLines = xmlParser.findAndAcceptElementValue("responsefieldlines", 0);
        this.numAttachments = xmlParser.findAndAcceptElementValue("attachments", 0);
        this.attachmentsRequired = (xmlParser.findAndAcceptElementValue("attachmentsrequired", 0) > 0);
        this.graderInfo = this.getQuestionBank().parseFormattedElementFromMXML(xmlParser, "graderinfo", this.getPartialName());
        this.responseTemplate = this.getQuestionBank().parseFormattedElementFromMXML(xmlParser, "responsetemplate", this.getPartialName());
    }

    @Override
    public void exportQTI21ResponseDeclaration(XMLWriter xmlWriter) throws XMLStreamException {
        if (this.numAttachments > 0) {
            xmlWriter.writeEmptyElement("responseDeclaration");
            xmlWriter.writeAttribute("identifier", "RESPONSE_01");
            xmlWriter.writeAttribute("cardinality", "single");
            xmlWriter.writeAttribute("baseType", "file");
        }
        if (!this.responseFormat.equalsIgnoreCase("noinline")) {
            xmlWriter.writeEmptyElement("responseDeclaration");
            xmlWriter.writeAttribute("identifier", "RESPONSE");
            xmlWriter.writeAttribute("cardinality", "single");
            xmlWriter.writeAttribute("baseType", "string");
        }
    }

    @Override
    public void exportQTI21QuestionChoices(XMLWriter xmlWriter) throws XMLStreamException {
        if (this.numAttachments > 0) {
            xmlWriter.writeEmptyElement("uploadInteraction");
            xmlWriter.writeAttribute("responseIdentifier", "RESPONSE_01");
            //xmlWriter.writeAttribute("identifier", "RESPONSE_01");
        }
        if (!this.responseFormat.equalsIgnoreCase("noinline")) {
            xmlWriter.writeEmptyElement("extendedTextInteraction");
            xmlWriter.writeAttribute("responseIdentifier", "RESPONSE");
            xmlWriter.writeAttribute("expectedLength", String.valueOf(40 * this.responseFieldLines));
        }
    }

    @Override
    public void exportQTI21QuestionRubricBlock(XMLWriter xmlWriter) throws XMLStreamException {
        super.exportQTI21QuestionRubricBlock(xmlWriter);

        if (this.defaultGrade <= 0) return;

        xmlWriter.writeStartElement("rubricBlock");
        xmlWriter.writeAttribute("view", "scorer");
        xmlWriter.writeAttribute("class", "tvScoringCriteria");
        xmlWriter.writeStartElement("table");
        xmlWriter.writeStartElement("thead");
        xmlWriter.writeRawCharacters("<tr><th>Name</th><th>Criterium</th><th>Score</th></tr>");
        xmlWriter.writeEndElement(); // thead
        xmlWriter.writeStartElement("tbody");
        double pointsLeft = this.defaultGrade;
        int closeIndex = 0;
        int colonIndex = 0;
        int count = 1;
        String correctionModel = this.graderInfo.trim();
        while (pointsLeft > 0 || correctionModel.length() > 0) {
            double mark = pointsLeft;
            String criterion = "Criterium-" + count++;
            String text = correctionModel;
            if (correctionModel.toLowerCase().startsWith("[mark=") &&
                    (closeIndex = correctionModel.indexOf(']')) > 6) {
                colonIndex = correctionModel.indexOf(':');
                if (colonIndex > 0 && colonIndex < closeIndex) {
                    criterion = correctionModel.substring(colonIndex+1, closeIndex);
                    mark = Double.valueOf(correctionModel.substring(6, colonIndex));
                } else {
                    mark = Double.valueOf(correctionModel.substring(6, closeIndex));
                }
                int nextMarkIndex = correctionModel.indexOf("[mark=", closeIndex);
                if (nextMarkIndex >= 0) {
                    text = correctionModel.substring(closeIndex + 1, nextMarkIndex);
                    correctionModel = correctionModel.substring(nextMarkIndex);
                } else {
                    text = correctionModel.substring(closeIndex + 1);
                    correctionModel = "";
                }
            } else {
                correctionModel = "";
            }
            xmlWriter.writeStartElement("tr");
            xmlWriter.writeStartElement("td");
            xmlWriter.writeCharacters(criterion);
            xmlWriter.writeEndElement(); // td
            xmlWriter.writeStartElement("td");
            this.exportQTI21TextBlock(xmlWriter, text);
            xmlWriter.writeEndElement(); // td
            xmlWriter.writeStartElement("td");
            if ((int)(100*mark) != 100*((int)mark)) {
                SLF4J.LOGGER.warn("Non-integer grade {} points in {}",
                            mark, this.getPartialName());
                if (mark >= pointsLeft) mark = Math.ceil(pointsLeft);
            }
            xmlWriter.writeCharacters(String.valueOf((int)mark));
            xmlWriter.writeEndElement(); // td
            xmlWriter.writeEndElement(); // tr
            pointsLeft -= mark;
        }

        xmlWriter.writeEndElement(); // tbody
        xmlWriter.writeEndElement(); // table
        xmlWriter.writeEndElement(); // rubricBlock

        if (pointsLeft != 0) {
            SLF4J.LOGGER.error("Grading model adds up to {} points in {}",
                    this.defaultGrade - pointsLeft, this.getPartialName());
        }
    }
}
