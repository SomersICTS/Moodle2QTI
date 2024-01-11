package ui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.QuestionBank;
import utils.SLF4J;

import java.io.*;
import java.util.Arrays;

import static utils.PathUtils.removeFileExtension;

public class Moodle2QtiController {
    @FXML
    public ChoiceBox<String> logLevel;

    @FXML
    private TextArea messageLog;

    @FXML
    private ChoiceBox<String> sourceType;

    @FXML
    private ChoiceBox<String> outputType;

    @FXML
    private Label sourceFilePath;


    @FXML
    protected void initialize() {
        PrintStream outStream = new PrintStream(new TextAreaOutputStream(this.messageLog, System.out));
        System.setOut(outStream);
        //PrintStream errStream = new PrintStream(new TextAreaOutputStream(this.messageLog, System.err));
        //System.setErr(errStream);
    }

    @FXML
    public void onSelectSourceFile(ActionEvent actionEvent) {
        Node node = (Node) actionEvent.getSource();
        Stage primaryStage = (Stage) node.getScene().getWindow();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Source File");
        File file = fileChooser.showOpenDialog(primaryStage);

        sourceFilePath.setText(file.getPath());
        sourceFilePath.setContentDisplay(ContentDisplay.RIGHT);
    }

    public void onStartButtonClick(ActionEvent actionEvent) {
        String source = sourceFilePath.getText();
        String inputFormat = sourceType.getValue();
        String outputFormat = outputType.getValue();

        SLF4J.LOGGER.info("Starting {}-import of questionbank '{}'...", inputFormat, source);

        //String exportName = source.split("\\.")[0] + "_qti";
        String exportPathBase = removeFileExtension(source);

        new Thread(()->{
            QuestionBank questionBank =
                    QuestionBank.load(source, inputFormat);
            // QuestionBank.loadFromMoodleXMLResource("DB2-20200730.xml");
            // QuestionBank.loadFromMoodleXMLResource(source);
            // QuestionBank.loadFromMoodleXMLResource("DS-20200731.xml");
            // QuestionBank.loadFromMoodleXMLResource("OOAD-20200815.xml");

            questionBank.showSummary();

            SLF4J.LOGGER.info("Starting {}-export of questionbank...", outputFormat);
            questionBank.export(outputFormat, exportPathBase);
        }).start();

    }

    private static class TextAreaOutputStream extends OutputStream {

        private TextArea textArea;
        private PrintStream console;

        public TextAreaOutputStream(TextArea textArea, PrintStream console) {
            this.textArea = textArea;
            this.console = console;
        }

        @Override
        public void write(int b) throws IOException {
            if (this.console != null) this.console.write(b);
            if (this.textArea != null) {
                Platform.runLater(()->this.textArea.appendText(String.valueOf((char) b)));
                //this.textArea.positionCaret(Integer.MAX_VALUE);
            }
        }

        @Override
        public void write(byte[] b) throws IOException {
            //if (this.console != null) this.console.write(b);
            if (this.textArea != null) {
                Platform.runLater(()->this.textArea.appendText(Arrays.toString(b)));
                //this.textArea.positionCaret(Integer.MAX_VALUE);
            }
        }
    }
}