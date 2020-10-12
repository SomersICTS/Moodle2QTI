import utils.SLF4J;

public class QuizConverterMain {

    public static void main(String[] args) {

        // load the simulation configuration with open and closing times
        // and products and customers

        String inputFormat = "moodleXml";
        String outputFormat = "qti2.1";

        for (int aIdx = 0; aIdx < args.length; aIdx++) {
            switch (args[aIdx]) {
                case "-i":
                    aIdx++;
                    inputFormat = args[aIdx];
                    break;
                case "-o":
                    aIdx++;
                    outputFormat = args[aIdx];
                    break;
                default:
                    SLF4J.LOGGER.error("Unknown commandline option: {}", args[aIdx]);
            }
        }

        SLF4J.LOGGER.info("Starting {}-import of questionbank...", inputFormat);
        QuestionBank questionBank =
                // QuestionBank.load(System.in, inputFormat);
                QuestionBank.loadFromMoodleXMLResource("DB2-20200730.xml");
                // QuestionBank.loadFromMoodleXMLResource("ADS-20200730.xml");
                // QuestionBank.loadFromMoodleXMLResource("DS-20200731.xml");
                // QuestionBank.loadFromMoodleXMLResource("OOAD-20200815.xml");

        questionBank.showSummary();

        SLF4J.LOGGER.info("Starting {}-export of questionbank...", outputFormat);
        questionBank.export(System.out, outputFormat);

    }
}
