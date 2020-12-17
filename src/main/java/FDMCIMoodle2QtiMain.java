import utils.SLF4J;

public class FDMCIMoodle2QtiMain {

    private static final String[] MOODLE_SOURCES = {
            // "ADS-20201026.xml",
            "WTE-20201014.xml",
            // "WEF-20201014.xml", "DB2-20200730.xml",
            // "DB-20201014-a.xml", "DB-20201014-b.xml",
            // "OOAD-20200815.xml",
            // "DS-20200731.xml"
    };
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

        for (String source : MOODLE_SOURCES) {
            SLF4J.LOGGER.info("Starting {}-import of questionbank '{}'...", inputFormat, source);
            String exportName = source.split("\\.")[0] + "_qti";
            QuestionBank questionBank =
                    // QuestionBank.load(System.in, inputFormat);
                    // QuestionBank.loadFromMoodleXMLResource("DB2-20200730.xml");
                    QuestionBank.loadFromMoodleXMLResource(source);
            // QuestionBank.loadFromMoodleXMLResource("DS-20200731.xml");
            // QuestionBank.loadFromMoodleXMLResource("OOAD-20200815.xml");

            questionBank.showSummary();

            SLF4J.LOGGER.info("Starting {}-export of questionbank '{}'...", outputFormat, exportName);
            questionBank.export(outputFormat, exportName);
        }
    }
}
