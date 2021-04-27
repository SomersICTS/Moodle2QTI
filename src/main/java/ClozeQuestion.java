import utils.SLF4J;
import utils.XMLParser;
import utils.XMLWriter;

import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.List;

public class ClozeQuestion extends Question {
    private static int nextClozeEntryId = 30001;

    private class ClozeAnswer {
        private int id;
        private double score;
        private String value;
    }

    private class ClozeEntry {
        private int id;
        private int weight;
        private String type;
        private int maxLength = 0;
        List<ClozeAnswer> answers = new ArrayList<>();

        private ClozeEntry() {
            this.id = nextClozeEntryId++;
        }

        public int getWeight() {
            return weight;
        }

        private String getIdText() {
            return "alt_" + this.id;
        }
        private String getAnswerIdText(int aIdx) {
            return this.getIdText() + "_" + (aIdx+1);
        }
        private String getBaseType() {
            if (type.startsWith("nm")) {
                return "float";
            } else if (type.startsWith("sa")) {
                return "string";
            } else {
                return Question.SELECTION_BASETYPE;
            }
        }
    }

    List<ClozeEntry> clozeEntries = new ArrayList<>();

    private String interactionBlock = null;

    public ClozeQuestion(Category category) {
        super(category);
    }

    @Override
    public String getInteractionType() {
        return QuestionBank.QTI_MIXED_INTERACTION;
    }

    public static ClozeQuestion createFromMXML(XMLParser xmlParser, Category category) throws XMLStreamException {
        ClozeQuestion clozeQuestion = new ClozeQuestion(category);

        clozeQuestion.parseSetupFromMXML(xmlParser);
        clozeQuestion.extractClozeEntries();
        clozeQuestion.splitInteractionBlock();

        SLF4J.LOGGER.debug("Created cloze-question: '{}'", clozeQuestion.name);

        return clozeQuestion;
    }

    private static int extractInteger(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return Integer.MIN_VALUE;
        }
    }

    private static double extractDouble(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return Double.NEGATIVE_INFINITY;
        }
    }

    private void extractClozeEntries() {
        int nextPosition = QuestionBank.indexOfNonEscaped("{", this.questionText, 0);
        while (nextPosition >= 0) {
            ClozeEntry ce = new ClozeEntry();
            ce.type = null;
            ce.weight = -1;
            int nextBrace = QuestionBank.indexOfNonEscaped("}", this.questionText, nextPosition + 5);
            int nextColon1 = this.questionText.indexOf(':', nextPosition + 1);
            int nextColon2 = this.questionText.indexOf(':', nextColon1 + 3);
            if (nextColon1 > nextPosition && nextColon2 > nextColon1 && nextBrace > nextColon2) {
                ce.weight = extractInteger(this.questionText.substring(nextPosition + 1, nextColon1));
                ce.type = this.questionText.substring(nextColon1 + 1, nextColon2).toLowerCase();
                ce.type = ce.type.replace("shortanswer", "sa");
                ce.type = ce.type.replace("multichoice", "mc");
                ce.type = ce.type.replace("multiresponse", "mr");
                ce.type = ce.type.replace("numerical", "nm");
                ce.type = ce.type.replace("mw", "sa").replace("_", "");
                if (ce.type.length() > 4 || ce.type.length() < 2) ce.type = null;
            }
            if (ce.type == null || ce.weight < 0) {
                nextPosition = QuestionBank.indexOfNonEscaped("{", this.questionText, nextPosition+1);
                continue;
            }

            int nextAnswer = nextColon2 + 1;

            do {
                if (this.questionText.charAt(nextAnswer) == '~') nextAnswer++;

                ClozeAnswer ca = new ClozeAnswer();
                ca.score = 0;
                if (this.questionText.charAt(nextAnswer) == '=') {
                    ca.score = 100;
                    nextAnswer++;
                } else if (this.questionText.charAt(nextAnswer) == '%') {
                    int pStart = nextAnswer + 1;
                    int pEnd = this.questionText.indexOf('%', pStart);
                    if (pEnd > pStart) {
                        double penalty = extractDouble(this.questionText.substring(pStart, pEnd));
                        if (penalty != Double.NEGATIVE_INFINITY) {
                            ca.score = penalty;
                            nextAnswer = pEnd + 1;
                        }
                    }
                }
                int endAnswer = QuestionBank.indexOfNonEscaped("~", this.questionText, nextAnswer);
                int endEntry = QuestionBank.indexOfNonEscaped("}", this.questionText, nextAnswer);
                if (endEntry > 0 && endEntry < endAnswer || endAnswer < 0) {
                    endAnswer = endEntry;
                }
                String rawValue = this.questionText.substring(nextAnswer, endAnswer);
                if (QuestionBank.indexOfNonEscaped("*", rawValue, 0) >= 0) {
                    String flatValue = QuestionBank.deEscape( rawValue.replace("\\*", "{$$$}").replace("*", "").replace("{$$$}", "\\*") );
                    rawValue = rawValue.replace("\\*", "{$$$}").replace("*", " ").replace("{$$$}", "\\*");
                    ClozeAnswer flatCa = new ClozeAnswer();
                    flatCa.score = ca.score;
                    flatCa.value = flatValue;
                    ce.answers.add(flatCa);
                    SLF4J.LOGGER.debug("Expanded *-wildcard in cloze-entry #{} in question '{}'",
                            this.clozeEntries.size(), this.getPartialName());
                }
                ca.value = QuestionBank.deEscape( rawValue );
                ce.answers.add(ca);
                ce.maxLength = Integer.max(ce.maxLength, ca.value.length());
                nextAnswer = endAnswer;
            } while (nextAnswer < this.questionText.length() - 2 &&
                    (this.questionText.charAt(nextAnswer) == '~'));

            if (this.questionText.charAt(nextAnswer) == '}') {
                nextAnswer++;
            } else {
                SLF4J.LOGGER.error("Syntax error in cloze-entry #{} in question '{}'",
                        this.clozeEntries.size(), this.getPartialName());
            }

            this.questionText = this.questionText.substring(0, nextPosition) +
                    String.format("{#%02d}", this.clozeEntries.size()) +
                    this.questionText.substring(nextAnswer);
            this.clozeEntries.add(ce);
            nextPosition = QuestionBank.indexOfNonEscaped("{", this.questionText, nextPosition+5);
        }

        if (this.clozeEntries.size() == 0) {
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
        if (this.clozeEntries.size() == 0) return;

        // find a suitable place to split
        String prefix = this.questionText;
        int firstTag = this.questionText.indexOf("{#0");
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
        xmlWriter.writeStartElement("responseDeclaration");
        xmlWriter.writeAttribute("identifier", "RESPONSE");
        xmlWriter.writeAttribute("cardinality", "multiple");
        xmlWriter.writeStartElement("correctResponse");
        for (int ceIdx = 0; ceIdx < this.clozeEntries.size(); ceIdx++) {
            ClozeEntry ce = this.clozeEntries.get(ceIdx);
            for (int caIdx = 0; caIdx < ce.answers.size(); caIdx++) {
                ClozeAnswer ca = ce.answers.get(caIdx);
                if (ca.score > 0) {
                    if (ca.score < 50) {
                        SLF4J.LOGGER.warn("Ignoring partial score {} of answer {} in close-entry #{} in question '{}'",
                                ca.score, ca.value, ceIdx, this.getPartialName());
                    } else if (ca.score < 100) {
                        SLF4J.LOGGER.warn("Upgrading partial score {} of answer {} in close-entry #{} in question '{}'",
                                ca.score, ca.value, ceIdx, this.getPartialName());
                    }
                }
                if (ca.score >= 50) {
                    xmlWriter.writeStartElement("value");
                    xmlWriter.writeAttribute("fieldIdentifier", ce.getIdText());
                    xmlWriter.writeAttribute("baseType", ce.getBaseType());
                    if (ce.type.startsWith("m")) {
                        xmlWriter.writeCharacters(ce.getAnswerIdText(caIdx));
                    } else {
                        //QuestionBank.countAndFlagInvalidWords(new String[] {"<",">","&"}, ca.value, this.getPartialName() );
                        xmlWriter.writeCharacters(ca.value);
                    }
                    xmlWriter.writeEndElement(); // value
                }
            }
        }
        xmlWriter.writeEndElement(); // correctResponse
        xmlWriter.writeStartElement("mapping");
        xmlWriter.writeAttribute("defaultValue", "0");
        int weightSum = this.clozeEntries.stream().mapToInt(ClozeEntry::getWeight).sum();
        for (ClozeEntry ce : this.clozeEntries) {
            xmlWriter.writeEmptyElement("mapEntry");
            xmlWriter.writeAttribute("mapKey", ce.getIdText());
            xmlWriter.writeAttribute("mappedValue", String.valueOf(1.0 * ce.getWeight() / weightSum));
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

        for (int ceIdx = 0; ceIdx < this.clozeEntries.size(); ceIdx++) {
            ClozeEntry ce = this.clozeEntries.get(ceIdx);
            String iaEntry;
            if (ce.type.startsWith("m")) {
                iaEntry = String.format("<inlineChoiceInteraction class='multipleinput' id='%s' responseIdentifier='RESPONSE' shuffle='%b' required='true'>",
                // iaEntry = String.format("<inlineChoiceInteraction class=\"multipleinput\" id=\"%s\" responseIdentifier=\"RESPONSE\" shuffle=\"%b\" required=\"true\">",
                        ce.getIdText(), ce.type.contains("s"));
                for (int caIdx = 0; caIdx < ce.answers.size(); caIdx++) {
                    String aText = QuestionBank.deEscapeHTMLEntities(ce.answers.get(caIdx).value);
                    aText = QuestionBank.fixPlainTextforQTI21(aText);
                    // aText = QuestionBank.countAndFlagInvalidWords(new String[] {"<",">","&"}, aText, this.getPartialName() );
                    iaEntry += String.format("<inlineChoice identifier='%s'>%s</inlineChoice>",
                    // iaEntry += String.format("<inlineChoice identifier=\"%s\">%s</inlineChoice>",
                            ce.getAnswerIdText(caIdx), aText);
                }
                iaEntry += "</inlineChoiceInteraction>";
            } else {
                iaEntry = String.format("<textEntryInteraction class='multipleinput' id='%s' responseIdentifier='RESPONSE' expectedLength='%d' />",
                // iaEntry = String.format("<textEntryInteraction class=\"multipleinput\" id=\"%s\" responseIdentifier=\"RESPONSE\" expectedLength=\"%d\" />",
                        ce.getIdText(), ce.maxLength + 5);
            }
            interactionText = interactionText.replace(String.format("{#%02d}", ceIdx), iaEntry);
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
