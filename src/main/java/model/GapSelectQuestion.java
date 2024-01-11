package model;

import utils.SLF4J;
import utils.XMLParser;
import utils.XMLWriter;

import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.List;

public class GapSelectQuestion extends Question {
    private static int nextGapEntryId = 40001;
    private static String GAP_ID_PREFIX = "alt_";

    private boolean shuffleAnswers;

    private static class GapAnswer {
        private String value;
        private int groupNr;
    }

    private class GapEntry {
        private int id;
        private int answerNr = 0;

        private GapEntry() {
            this.id = nextGapEntryId++;
        }

        private String getIdText() {
            return GAP_ID_PREFIX + this.id;
        }
        private String getAnswerIdText(int aIdx) {
            return this.getIdText() + "_" + (aIdx+1);
        }
        private String getBaseType() {
                return Question.SELECTION_BASETYPE;
        }
    }

    List<GapAnswer> gapAnswers = new ArrayList<>();
    List<GapEntry> gapEntries = new ArrayList<>();

    private String interactionBlock = null;

    public GapSelectQuestion(Category category) {
        super(category);
    }

    @Override
    public String getInteractionType() {
        return QuestionBank.QTI_MIXED_INTERACTION;
    }

    public static GapSelectQuestion createFromMXML(XMLParser xmlParser, Category category) throws XMLStreamException {
        GapSelectQuestion gapSelectQuestion = new GapSelectQuestion(category);

        gapSelectQuestion.parseSetupFromMXML(xmlParser);
        gapSelectQuestion.extractGapEntries();
        gapSelectQuestion.splitInteractionBlock();

        SLF4J.LOGGER.debug("Created gapselect-question: '{}'", gapSelectQuestion.name);

        return gapSelectQuestion;
    }

    @Override
    public void parseSetupFromMXML(XMLParser xmlParser) throws XMLStreamException {

        super.parseSetupFromMXML(xmlParser);

        this.shuffleAnswers = xmlParser.acceptOptionalElementValue("shuffleanswers", true);

        super.parseFeedbackFromMXML(xmlParser);

        this.parseGapAnswersFromMXML(xmlParser);
    }

    private void parseGapAnswersFromMXML(XMLParser xmlParser) throws XMLStreamException {

        this.gapAnswers = new ArrayList<>();
        xmlParser.findBeginTag("selectoption");
        GapAnswer gapAnswer;
        while ((gapAnswer = this.parseGapAnswerFromMXML(xmlParser)) != null) {
            this.gapAnswers.add(gapAnswer);
        }
    }

    private GapAnswer parseGapAnswerFromMXML(XMLParser xmlParser) throws XMLStreamException {
        if (xmlParser.nextBeginTag("selectoption")) {
            GapAnswer gapAnswer = new GapAnswer();
            xmlParser.nextTag();
            gapAnswer.value = xmlParser.findAndAcceptElementValue("text", "---");
            gapAnswer.groupNr = xmlParser.acceptOptionalElementValue("group", 0);
            xmlParser.findAndAcceptEndTag("selectoption");
            return gapAnswer;
        }
        return null;
    }

    private static int extractInteger(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return Integer.MIN_VALUE;
        }
    }

    private void extractGapEntries() {
        int nextPosition = QuestionBank.indexOfNonEscaped("[[", this.questionText, 0);
        while (nextPosition >= 0) {
            GapEntry ge = new GapEntry();
            int nextBrackets = QuestionBank.indexOfNonEscaped("]]", this.questionText, nextPosition+2);
            if (nextBrackets > nextPosition+2) {
                ge.answerNr = extractInteger(this.questionText.substring(nextPosition+2, nextBrackets));
            }
            if (ge.answerNr > 0) {
                this.gapEntries.add(ge);
            }

            nextPosition = QuestionBank.indexOfNonEscaped("[[", this.questionText, nextPosition+1);
        }

        if (this.gapEntries.size() == 0) {
            SLF4J.LOGGER.error("No interaction entries found in cloze-question '{}'", this.getPartialName());
        }
    }

    private static int findLastClosedTag(String tag, String text) {
        int lastClosed = 0;
        int nextOpen = QuestionBank.findNextOpenTag(tag, text, 0);
        while (nextOpen >= 0) {
            int nextClosed = QuestionBank.findMatchingClosingTagIndex(tag, text, nextOpen);
            if (nextClosed < 0) {
                return nextOpen;
            }
            lastClosed = nextClosed;
            nextOpen = QuestionBank.findNextOpenTag(tag, text, lastClosed);
        }
        return lastClosed;
    }

    private void splitInteractionBlock() {
        if (this.gapEntries.size() == 0) return;

        // find a suitable place to split
        String prefix = this.questionText;
        int firstTag = QuestionBank.indexOfNonEscaped("[[", this.questionText, 0);
        prefix = prefix.substring(0, firstTag);
        firstTag = findLastClosedTag("p", prefix);
        if (firstTag > 0) {
            this.interactionBlock = this.questionText.substring(firstTag);
            this.questionText = this.questionText.substring(0, firstTag);
        } else {
            this.interactionBlock = this.questionText;
            this.questionText = "";
        }
    }

    @Override
    public void exportQTI21ResponseDeclaration(XMLWriter xmlWriter) throws XMLStreamException {
        if (this.gapEntries.size() == 0) return;

        xmlWriter.writeStartElement("responseDeclaration");
        xmlWriter.writeAttribute("identifier", "RESPONSE");
        xmlWriter.writeAttribute("cardinality", "multiple");
        xmlWriter.writeStartElement("correctResponse");
        for (int geIdx = 0; geIdx < this.gapEntries.size(); geIdx++) {
            GapEntry ge = this.gapEntries.get(geIdx);
            xmlWriter.writeStartElement("value");
            xmlWriter.writeAttribute("fieldIdentifier", ge.getIdText());
            xmlWriter.writeAttribute("baseType", ge.getBaseType());
            xmlWriter.writeCharacters(ge.getAnswerIdText(ge.answerNr-1));
            xmlWriter.writeEndElement(); // value
        }
        xmlWriter.writeEndElement(); // correctResponse
        xmlWriter.writeStartElement("mapping");
        xmlWriter.writeAttribute("defaultValue", "0");
        for (GapEntry ge : this.gapEntries) {
            xmlWriter.writeEmptyElement("mapEntry");
            xmlWriter.writeAttribute("mapKey", ge.getIdText());
            xmlWriter.writeAttribute("mappedValue", String.valueOf(1.0 / this.gapEntries.size()));
        }
        xmlWriter.writeEndElement(); // mapping
        xmlWriter.writeEndElement(); // responseDeclaration
    }

    @Override
    public void exportQTI21QuestionChoices(XMLWriter xmlWriter) throws XMLStreamException {
        if (this.interactionBlock == null) return;

        xmlWriter.writeStartElement("div");
        xmlWriter.writeAttribute("class", QuestionBank.TV_INTERACTIONBLOCK_CLASS);
        xmlWriter.writeStartElement("div");
        xmlWriter.writeAttribute("id", QuestionBank.TV_TEXTBLOCK_ID + QuestionBank.getNextTextBlockId());
        xmlWriter.writeAttribute("class", QuestionBank.TV_TEXTBLOCK_CLASS + " moodletext");
        xmlWriter.writeStartElement("div");
        xmlWriter.writeAttribute("class", QuestionBank.TV_TBZONE_CLASS);

        String interactionText = this.interactionBlock;

        int nextPosition = 0;
        int nextBrackets = 0;
        for (int geIdx = 0; geIdx < this.gapEntries.size(); geIdx++) {
            GapEntry ge = this.gapEntries.get(geIdx);
            int correctAnswerNr = 0;
            while (nextPosition >= 0 && correctAnswerNr != ge.answerNr) {
                nextPosition = QuestionBank.indexOfNonEscaped("[[", interactionText, nextPosition);
                if (nextPosition >= 0) {
                    nextBrackets = QuestionBank.indexOfNonEscaped("]]", interactionText, nextPosition+2);
                    if (nextBrackets > nextPosition+2) {
                        correctAnswerNr = extractInteger(interactionText.substring(nextPosition+2, nextBrackets));
                    }
                    nextPosition++;
                }
            }

            if (nextPosition < 0) {
                SLF4J.LOGGER.error("Syntax error in gapselect-entry #{} in question '{}'",
                        geIdx+1, this.getPartialName());
            }

            String iaEntry = String.format("<inlineChoiceInteraction class='multipleinput' id='%s' responseIdentifier='RESPONSE' shuffle='%b' required='true'>",
                    // iaEntry = String.format("<inlineChoiceInteraction class=\"multipleinput\" id=\"%s\" responseIdentifier=\"RESPONSE\" shuffle=\"%b\" required=\"true\">",
                    ge.getIdText(), this.shuffleAnswers);
            GapAnswer correctAnswer = this.gapAnswers.get(correctAnswerNr-1);
            for (int caIdx = 0; caIdx < this.gapAnswers.size(); caIdx++) {
                GapAnswer ga = this.gapAnswers.get(caIdx);
                if (ga.groupNr != correctAnswer.groupNr) continue;
                String aText = QuestionBank.deEscapeHTMLEntities(ga.value);
                aText = QuestionBank.fixPlainTextforQTI21(aText);
                // aText = QuestionBank.countAndFlagInvalidWords(new String[] {"<",">","&"}, aText, this.getPartialName() );
                iaEntry += String.format("<inlineChoice identifier='%s'>%s</inlineChoice>",
                        // iaEntry += String.format("<inlineChoice identifier=\"%s\">%s</inlineChoice>",
                        ge.getAnswerIdText(caIdx), aText);
            }
            iaEntry += "</inlineChoiceInteraction>";
            interactionText = interactionText.substring(0, nextPosition-1) + iaEntry + interactionText.substring(nextBrackets+2);
            nextPosition += iaEntry.length()-1;
        }

        xmlWriter.writeRawCharacters(QuestionBank.fixHTMLforQTI21(interactionText, this.getPartialName()));

        xmlWriter.writeEndElement(); // div
        xmlWriter.writeEndElement(); // div
        xmlWriter.writeEndElement(); // div
    }

    public void exportQTI21ResponseProcessing(XMLWriter xmlWriter) throws XMLStreamException {
        xmlWriter.writeEmptyElement("responseProcessing");
        xmlWriter.writeAttribute("templateLocation", "/templates/RPTEMPLATE_SCORE.xml");
    }
}
