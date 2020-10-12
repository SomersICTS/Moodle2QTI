import utils.SLF4J;
import utils.XMLParser;
import utils.XMLWriter;

import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.List;

public class ClozeQuestion extends Question {
    private static int nextClozeEntryId = 80001;

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
            return "ce_" + this.id;
        }
        private String getAnswerIdText(int aIdx) {
            return this.getIdText() + "_a" + aIdx;
        }
        private String getBaseType() {
            if (type.startsWith("nm")) {
                return "number";
            } else {
                return "string";
            }
        }
    }

    List<ClozeEntry> clozeEntries = new ArrayList<>();

    private String interactionBlock = null;

    public ClozeQuestion(Category category) {
        super(category);
    }

    public static ClozeQuestion createFromMXML(XMLParser xmlParser, Category category) throws XMLStreamException {
        ClozeQuestion clozeQuestion = new ClozeQuestion(category);

        clozeQuestion.parseSetupFromMXML(xmlParser);
        clozeQuestion.extractClozeEntries();
        clozeQuestion.splitInteractionBlock();

        SLF4J.LOGGER.debug("Created cloze-question: '{}'", clozeQuestion.name);

        return clozeQuestion;
    }

    private static String deEscape(String s) {
        int backSlash = s.indexOf('\\');
        while (backSlash >= 0 && backSlash < s.length() - 1) {
            s = s.substring(0, backSlash) + s.substring(backSlash + 1);
            backSlash = s.indexOf('\\', backSlash + 1);
        }
        return s;
    }

    private static int indexOfNonEscaped(char ch, int from, String s) {
        int idx = s.indexOf(ch, from);
        if (idx >= from && idx > 0 && s.charAt(idx - 1) == '\\') {
            return indexOfNonEscaped(ch, idx + 1, s);
        }
        return idx;
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
        int nextPosition = indexOfNonEscaped('{', 0, this.questionText);
        while (nextPosition >= 0) {
            ClozeEntry ce = new ClozeEntry();
            ce.type = null;
            ce.weight = -1;
            int nextBrace = indexOfNonEscaped('}', nextPosition + 5, this.questionText);
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
                nextPosition = indexOfNonEscaped('{', nextPosition+1, this.questionText);
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
                int endAnswer = indexOfNonEscaped('~', nextAnswer, this.questionText);
                int endEntry = indexOfNonEscaped('}', nextAnswer, this.questionText);
                if (endEntry > 0 && endEntry < endAnswer || endAnswer < 0) {
                    endAnswer = endEntry;
                }
                ca.value = deEscape(this.questionText.substring(nextAnswer, endAnswer));
                ce.answers.add(ca);
                ce.maxLength = Integer.max(ce.maxLength, ca.value.length());
                nextAnswer = endAnswer;
            } while (nextAnswer < this.questionText.length() - 2 &&
                    (this.questionText.charAt(nextAnswer) == '~'));

            if (this.questionText.charAt(nextAnswer) == '}') {
                nextAnswer++;
            } else {
                SLF4J.LOGGER.error("Syntax error in cloze-entry #{} in question '{}'",
                        this.clozeEntries.size(), this.getFullName());
            }

            this.questionText = this.questionText.substring(0, nextPosition) +
                    String.format("{#%02d}", this.clozeEntries.size()) +
                    this.questionText.substring(nextAnswer);
            this.clozeEntries.add(ce);
            nextPosition = indexOfNonEscaped('{', nextPosition+5, this.questionText);
        }

        if (this.clozeEntries.size() == 0) {
            SLF4J.LOGGER.error("No interaction entries found in cloze-question '{}'", this.getFullName());
        }
    }

    private void splitInteractionBlock() {
        if (this.clozeEntries.size() == 0) return;
        int firstCE = this.questionText.indexOf("{#0");
        int lastPgf = -1;
        int nextPgf = this.questionText.indexOf("</p>");
        while (nextPgf >= 0 && nextPgf < firstCE) {
            lastPgf = nextPgf;
            nextPgf = this.questionText.indexOf("</p>", lastPgf + 4);
        }
        if (lastPgf >= 0) {
            this.interactionBlock = this.questionText.substring(lastPgf+4);
            this.questionText = this.questionText.substring(0, lastPgf+4);
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
                                ca.score, ca.value, ceIdx, this.name);
                    } else if (ca.score < 100) {
                        SLF4J.LOGGER.warn("Upgrading partial score {} of answer {} in close-entry #{} in question '{}'",
                                ca.score, ca.value, ceIdx, this.name);
                    }
                }
                if (ca.score >= 50) {
                    xmlWriter.writeStartElement("value");
                    xmlWriter.writeAttribute("fieldIdentifier", ce.getIdText());
                    xmlWriter.writeAttribute("baseType", ce.getBaseType());
                    if (ce.type.startsWith("m")) {
                        xmlWriter.writeCharacters(ce.getAnswerIdText(caIdx));
                    } else {
                        xmlWriter.writeRawCharacters(QuestionBank.escapeToHTMLEntities(ca.value));
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
        xmlWriter.writeAttribute("class", "interactieblok");
        xmlWriter.writeStartElement("div");
        xmlWriter.writeAttribute("class", "textblock tvblock moodletext");
        xmlWriter.writeStartElement("div");
        xmlWriter.writeAttribute("class", "rte_zone tveditor1");

        String interactionText = this.interactionBlock;

        for (int ceIdx = 0; ceIdx < this.clozeEntries.size(); ceIdx++) {
            ClozeEntry ce = this.clozeEntries.get(ceIdx);
            String iaEntry;
            if (ce.type.startsWith("m")) {
                iaEntry = String.format("<inlineChoiceInteraction class=\"multipleinput\" id=\"%s\" responseIdentifier=\"RESPONSE\" shuffle=\"%b\" required=\"true\">",
                        ce.getIdText(), ce.type.contains("s"));
                for (int caIdx = 0; caIdx < ce.answers.size(); caIdx++) {
                    iaEntry += String.format("<inlineChoice identifier=\"%s\">%s</inlineChoice>",
                            ce.getAnswerIdText(caIdx), ce.answers.get(caIdx).value);
                }
                iaEntry += "</inlineChoiceInteraction>";
            } else {
                iaEntry = String.format("<textEntryInteraction class=\"multipleinput\" id=\"%s\" responseIdentifier=\"RESPONSE\" expectedLength=\"%d\" />",
                        ce.getIdText(), ce.maxLength + 5);
            }
            interactionText = interactionText.replace(String.format("{#%02d}", ceIdx), iaEntry);
        }

        xmlWriter.writeRawCharacters(QuestionBank.fixHTMLforQTI21(interactionText));

        xmlWriter.writeEndElement(); // div
        xmlWriter.writeEndElement(); // div
        xmlWriter.writeEndElement(); // div
    }

    @Override
    public void exportQTI21ResponseProcessing(XMLWriter xmlWriter) throws XMLStreamException {
        xmlWriter.writeEmptyElement("responseProcessing");
        xmlWriter.writeAttribute("templateLocation", "/templates/RPTEMPLATE_SCORE.xml");
    }
}
