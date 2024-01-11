package model;

import utils.SLF4J;
import utils.XMLParser;
import utils.XMLWriter;

import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.List;

public class MCQuestion extends Question {

    private boolean singleAnswer;
    private boolean shuffleAnswers;
    private String answerNumbering;

    public MCQuestion(Category category) {
        super(category);
    }

    @Override
    public String getInteractionType() {
        return QuestionBank.QTI_MC_INTERACTION;
    }

    public static Question createFromMXML(XMLParser xmlParser, Category category) throws XMLStreamException {
        MCQuestion mcQuestion = new MCQuestion(category);

        mcQuestion.parseSetupFromMXML(xmlParser);


        SLF4J.LOGGER.debug("Created MC-question: '{}'", mcQuestion.name);

        return mcQuestion;
    }

    @Override
    public void parseSetupFromMXML(XMLParser xmlParser) throws XMLStreamException {

        super.parseSetupFromMXML(xmlParser);

        this.singleAnswer = xmlParser.acceptOptionalElementValue("single", true);
        this.shuffleAnswers = xmlParser.acceptOptionalElementValue("shuffleanswers", true);
        this.answerNumbering = xmlParser.acceptOptionalElementValue("answernumbering", null);

        super.parseFeedbackFromMXML(xmlParser);

        super.parseAnswersFromMXML(xmlParser);
    }

    @Override
    public void exportQTI21ResponseDeclaration(XMLWriter xmlWriter) throws XMLStreamException {
        boolean firstScoreWarning = true;
        xmlWriter.writeStartElement("responseDeclaration");
        xmlWriter.writeAttribute("identifier", "RESPONSE");
        xmlWriter.writeAttribute("cardinality", this.singleAnswer ? "single" : "multiple");
        xmlWriter.writeAttribute("baseType", "identifier");
        double maxCorrectness =
                this.answers.stream().mapToDouble(Answer::getCorrectness).max().orElse(0.0);
        if (this.singleAnswer & maxCorrectness < 100) {
            SLF4J.LOGGER.warn("Single-answer mc-question '{}' gives a score of '{}' (below 100)",
                    this.getPartialName(), maxCorrectness);
            firstScoreWarning = false;
        }
        xmlWriter.writeStartElement("correctResponse");
        int numCorrectAnswers = 0;
        for (Answer a: this.answers) {
            double correctness = a.getCorrectness();
            if (correctness > 0) {
                if (this.singleAnswer) {
                    if (correctness < maxCorrectness) {
                            SLF4J.LOGGER.warn("Discarded partial score {} of answer {} in single-answer mc-question '{}'",
                                    correctness, a.getId(), this.getPartialName());
                        continue;
                    } else if (numCorrectAnswers > 0) {
                        if (firstScoreWarning) {
                            SLF4J.LOGGER.warn("Discarded duplicate correct answer {} in single-answer mc-question '{}'",
                                    a.getId(), this.getPartialName());
                            firstScoreWarning = false;
                        }
                        continue;
                    }
                }
                numCorrectAnswers++;
                xmlWriter.writeStartElement("value");
                xmlWriter.writeCharacters(a.getId());
                xmlWriter.writeEndElement();
            }
        }
        xmlWriter.writeEndElement(); // correctResponse

        if (!this.singleAnswer) {
            xmlWriter.writeStartElement("mapping");
            xmlWriter.writeAttribute("defaultValue", "0");
            String mappedValue = String.valueOf(1.0 * numCorrectAnswers / this.answers.size());
            for (Answer a: this.answers) {
                if (a.getCorrectness() > 0) {
                    xmlWriter.writeEmptyElement("mapEntry");
                    xmlWriter.writeAttribute("mapKey", a.getId());
                    xmlWriter.writeAttribute("mappedValue", mappedValue);
                }
            }
            xmlWriter.writeEndElement(); // mapping
        }

        xmlWriter.writeEndElement(); // responseDeclaration
    }

    @Override
    public void exportQTI21QuestionText(XMLWriter xmlWriter) throws XMLStreamException {

        super.exportQTI21QuestionText(xmlWriter);

        if (this.singleAnswer) return;

        xmlWriter.writeStartElement("div");
        xmlWriter.writeAttribute("class", QuestionBank.TV_TBZONE_CLASS + " generated");
        if (this.getQuestionBank().getLanguage().startsWith("Dut")) {
            xmlWriter.writeCharacters("(Hieronder mag je een of meerdere antwoorden selecteren.)");
        } else {
            xmlWriter.writeCharacters("(Please select one or multiple answers.)");
        }
        xmlWriter.writeEndElement(); // div
    }

    @Override
    public void exportQTI21QuestionChoices(XMLWriter xmlWriter) throws XMLStreamException {
        xmlWriter.writeStartElement("choiceInteraction");
        xmlWriter.writeAttribute("class", this.answers.size() > 5 ? "TweeKolom" : "EenKolom");
        xmlWriter.writeAttribute("responseIdentifier", "RESPONSE");
        xmlWriter.writeAttribute("shuffle", String.valueOf(this.shuffleAnswers));
        xmlWriter.writeAttribute("maxChoices", String.valueOf(this.singleAnswer ? 1 : 0));

        for (Answer a: this.answers) {
            a.exportQTI21Alternative(xmlWriter);
        }

        xmlWriter.writeEndElement(); // choiceInteraction
    }

    @Override
    public void exportQTI21ResponseProcessing(XMLWriter xmlWriter) throws XMLStreamException {
        xmlWriter.writeEmptyElement("responseProcessing");
        xmlWriter.writeAttribute("templateLocation",
                this.singleAnswer ? "/templates/RPTEMPLATE_GF.xml" : "/templates/RPTEMPLATE_SCORE.xml");
    }
}
