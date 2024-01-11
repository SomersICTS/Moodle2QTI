package cli;

import model.*;
import utils.SLF4J;

import java.io.File;

public class Moodle2QtiMain {

    private static final String[] MOODLE_SOURCES = {
            "data/DB2-20220119-1718.xml",
            // "data/WEF-20211222-1442.xml",
            // "data/ADS-20211225-1353.xml",
            // "data/BPM-20211015-1854.xml",
            // "data/BUS-20210429-1347.xml",
            // "data/ITSM-20210429-1322.xml",
            // "data/se1_q.xml",
            // "data/ESKE-20210525-1421.xml",
            // "data/ESKN-DT-20210628-1729.xml",
            // "data/PRO-20210823-1833.xml",
            // "data/INFRA-20210122-1114.xml",
            // "data/DB-top-20201217.xml",
            // "data/DB-20201014-a.xml", "data/DB-20201014-b.xml",
            // "data/OOAD-20200815.xml",
            // "data/DS-20200731.xml",
            // "data/WTE-20220322-1520.xml"
    };
    public static void main(String[] args) {

        // load the simulation configuration with open and closing times
        // and products and customers

        String inputFormat = "moodleXML";
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
            //String exportName = source.split("\\.")[0] + "_qti";
            String exportName = new File(source).getName().split("\\.")[0] + "_qti";
            QuestionBank questionBank =
                    // QuestionBank.load(System.in, inputFormat);
                    // QuestionBank.loadFromMoodleXMLResource("DB2-20200730.xml");
                    QuestionBank.loadFromMoodleXMLResource(source);
            // QuestionBank.loadFromMoodleXMLResource("DS-20200731.xml");
            // QuestionBank.loadFromMoodleXMLResource("OOAD-20200815.xml");

            questionBank.showSummary();

            SLF4J.LOGGER.info("Starting {}-export of questionbank '{}'...", outputFormat, exportName);
            questionBank.exportToResource(outputFormat, exportName);
        }
    }
}
