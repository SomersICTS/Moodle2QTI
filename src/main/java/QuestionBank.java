import utils.SLF4J;
import utils.XMLParser;
import utils.XMLWriter;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class QuestionBank {

    Category rootCategory = new Category("", null, this);

    public Category getCurrentCategory() {
        return currentCategory;
    }

    private Category currentCategory = this.rootCategory;
    private List<Question> questions = new ArrayList<>();

    private Map<String, Image> images = new HashMap<>();

    public Map<String, Image> getImages() {
        return images;
    }

    public static QuestionBank load(InputStream input, String format) {
        QuestionBank questionBank = new QuestionBank();

        switch (format) {
            case "moodleXML":
                XMLParser xmlParser = new XMLParser(input);
                questionBank.parseFromMXML(xmlParser);
            default:
                SLF4J.LOGGER.error("Unknown inport format: {}", format);
        }
        return questionBank;
    }

    public static QuestionBank loadFromMoodleXMLResource(String resourceName) {
        QuestionBank questionBank = new QuestionBank();

        XMLParser xmlParser = new XMLParser(resourceName);
        questionBank.parseFromMXML(xmlParser);

        return questionBank;
    }

    private void parseFromMXML(XMLParser xmlParser) {
        try {
            xmlParser.nextTag();
            xmlParser.require(XMLStreamConstants.START_ELEMENT, null, "quiz");
            xmlParser.nextTag();

            while (xmlParser.nextBeginTag("question")) {
                String type = xmlParser.getAttributeValue(null, "type");
                xmlParser.nextTag();

                switch (type) {
                    case "category":
                        this.currentCategory = parseCategoryFromMXML(xmlParser);
                        break;
                    case "multichoice":
                        this.questions.add(
                                MCQuestion.createFromMXML(xmlParser, this.currentCategory));
                        break;
                    case "essay":
                        this.questions.add(
                                EssayQuestion.createFromMXML(xmlParser, this.currentCategory));
                        break;
                    case "shortanswer":
                    case "truefalse":
                        this.questions.add(
                                SAQuestion.createFromMXML(xmlParser, this.currentCategory, type));
                        break;
                    case "numerical":
                    case "calculated":
                        this.questions.add(
                                NumQuestion.createFromMXML(xmlParser, this.currentCategory, type));
                        break;
                    case "ddimageortext":
                        this.questions.add(
                                DDQuestion.createFromMXML(xmlParser, this.currentCategory));
                        break;
                    case "matching":
                        this.questions.add(
                                MatchQuestion.createFromMXML(xmlParser, this.currentCategory));
                        break;
                    case "cloze":
                        this.questions.add(
                                ClozeQuestion.createFromMXML(xmlParser, this.currentCategory));
                        break;
                    default:
                        SLF4J.LOGGER.error("Skipped question type: '{}'", type);
                }
                xmlParser.findAndAcceptEndTag("question");
            }

        } catch (Exception ex) {
            SLF4J.logException("XML input error:", ex);
        }

        this.rootCategory = this.rootCategory.findNewRoot();
        this.rootCategory.setParent(null);
    }

    private Category parseCategoryFromMXML(XMLParser xmlParser) throws XMLStreamException {
        String fullName = parseFormattedElementFromMXML(xmlParser, "category");
        String info = parseFormattedElementFromMXML(xmlParser, "info");
        int id = xmlParser.acceptOptionalElementValue("idnumber", 0);
        if (fullName != null) {
            String[] path = fullName.split("/");
            Category category = this.rootCategory.findOrCreate(path);
            category.setInfo(info);
            category.setId(id);
            return category;
        }
        return null;
    }

    public int showSummary() {
        System.out.println("\nQuestionbank summary:");
        int totalNum = this.rootCategory.showSummary(0);
        System.out.printf("Total %d questions with %d images.\n",
                totalNum, this.images.size());
        return totalNum;
    }

    public String parseFormattedElementFromMXML(XMLParser xmlParser, String tag) throws XMLStreamException {
        xmlParser.findBeginTag(tag);
        String format = parseFormatAttributeFromMXML(xmlParser);
        xmlParser.nextTag();
        String formattedText = parseFormattedTextFromMXML(xmlParser, format);
        Image image;
        while ((image = Image.parseMXML(xmlParser, this, formattedText)) != null) {
            formattedText = formattedText.replace("@@PLUGINFILE@@" + image.getFullURL(), "@@PLUGINFILE@@" + image.getVersionedFullURL());
        }
        xmlParser.findAndAcceptEndTag(tag);
        return formattedText;
    }

    public static String parseFormatAttributeFromMXML(XMLParser xmlParser) {
        String format = xmlParser.getAttributeValue(null, "format");
        if (format != null &&
                !format.equals("html") &&
                !format.equals("moodle_auto_format") &&
                !format.equals("plain_text")
        ) {
            SLF4J.LOGGER.error("Ignored unknown text format '{}'", format);
            return null;
        }
        return format;
    }

    public static String parseFormattedTextFromMXML(XMLParser xmlParser, String format) throws XMLStreamException {
        String formattedText = xmlParser.findAndAcceptElementValue("text", null);
        if (format == null || format.equals("html")) {
            // if (formattedText.startsWith("<![CDATA[") && formattedText.endsWith("]]>")) formattedText = formattedText.substring(9,formattedText.length()-3);
            formattedText = checkSelfClosingHTMLTag("br", formattedText);
            formattedText = checkSelfClosingHTMLTag("img", formattedText);

            formattedText = removeHTMLTag("img src=\"https://moodle.informatica.hva.nl/brokenfile.php", formattedText);

            formattedText = checkSrcAttribute(formattedText);
        }
        return formattedText;
    }

    private static String checkSelfClosingHTMLTag(String tag, String text) {
        int i2 = 0;
        int i1;
        while (0 <= (i1 = text.toLowerCase().indexOf("<" + tag, i2))) {
            i2 = text.indexOf(">", i1);
            int i3 = text.indexOf("/>", i1);
            int i4 = text.indexOf("<", i1 + 1);
            if (i3 >= 0 && i3 < i2) break;
            if (i2 < 0 || (i4 >= 0 && i4 < i2)) {
                SLF4J.LOGGER.error("HTML syntax error on tag <{}> in '{}'.", tag, text);
                return text;
            }
            text = text.substring(0, i2) + "/" + text.substring(i2);
        }
        return text;
    }

    private static String checkSrcAttribute(String text) {
        int i2 = 0;
        int i1;
        while (0 <= (i1 = text.toLowerCase().indexOf(" src=\"", i2))) {
            i2 = text.indexOf("\"", i1 + 6);
            int i3 = text.indexOf("?time=", i1);
            if (i3 < 0 || i3 > i2) continue;
            String errorReference = text.substring(i3, i2);
            text = text.replace(errorReference, "");
            SLF4J.LOGGER.warn("Removed parameter '{}' in src attribute of question data.", errorReference);
            i2 = i3;
        }
        return text;
    }

    public static String removeHTMLTag(String tag, String text) {
        int i2 = 0;
        int i1;
        while (0 <= (i1 = text.toLowerCase().indexOf("<" + tag, i2))) {
            i2 = text.indexOf(">", i1);
            int i4 = text.indexOf("<", i1 + 1);
            if (i2 < 0 || (i4 >= 0 && i4 < i2)) {
                SLF4J.LOGGER.error("Cannot remove tag <{}> in '{}'.", tag, text);
                return text;
            }
            String errorReference = text.substring(i1, i2 + 1);
            text = text.replace(errorReference, "");
            SLF4J.LOGGER.warn("Removed tag '{}' in question data.", errorReference);
        }
        return text;
    }

    public static String removeHTMLAttribute(String attribute, String text) {
        int i2 = 0;
        int i1;
        while (0 <= (i1 = text.toLowerCase().indexOf(" " + attribute + "=\"", i2))) {
            i2 = text.indexOf("\"", i1 + attribute.length() + 3);
            String errorReference = text.substring(i1, i2 + 1);
            text = text.replace(errorReference, " ");
            SLF4J.LOGGER.warn("Removed attribute '{}' in question data.", errorReference);
        }
        return text;
    }

    public static String fixHTMLforQTI21(String text) {
        text = text.replace("<u>", "<b>").replace("</u>", "</b>");
        text = text.replace("<pre>", "<code>").replace("</pre>","</code>");
        text = text.replace("&nbsp;", " ");
        text = QuestionBank.removeHTMLAttribute("role", text);
        text = QuestionBank.removeHTMLAttribute("style", text);
        text = QuestionBank.removeHTMLAttribute("border", text);
        return text;
    }

    public static String escapeToHTMLEntities(String text) {
        text = text.replace("&", "&amp;");
        text = text.replace("<", "&lt;").replace(">", "&gt;");
        return text;
    }
    public static String deEscapeHTMLEntities(String text) {
        text = text.replace("&nbsp;", " ");
        text = text.replace("&lt;", "<").replace("&gt;",">");
        text = text.replace("&amp;", "&");
        return text;
    }

    private static boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

    public void export(OutputStream output, String format) {
        exportToQTI21Resource("exportQTI21");
    }

    public static final String QTI_MEDIAFILES = "mediafiles";
    public static final String QTI_CSS = "css/from_moodle.css";

    public void exportToQTI21Resource(String resourceName) {
        this.rootCategory = this.rootCategory.findNewRoot();

        try {
            String exportPath = this.resolveResourcePath(resourceName);
            deleteDirectory(new File(exportPath));
            Files.createDirectories(Paths.get(exportPath));
            exportImages(exportPath + "/" + this.rootCategory.getMediaFilesFolder());
            XMLWriter manifest = new XMLWriter(exportPath + "/imsmanifest.xml");
            manifest.writeStartDocument();
            manifest.writeStartElement("manifest");
            manifest.writeAttribute("identifier", "MANIFEST-QTI-1");
            manifest.writeAttribute("\n\txmlns:xml", "http://www.w3.org/XML/1998/namespace");
            manifest.writeAttribute("\n\txmlns", "http://www.imsglobal.org/xsd/imscp_v1p1");
            manifest.writeEmptyElement("organisations");
            manifest.writeStartElement("resources");
            this.rootCategory.exportQTI21(manifest, exportPath);
            manifest.writeEndElement();
            manifest.writeEndElement();
            manifest.writeEndDocument();
            manifest.close();
            zipFolder(exportPath);
        } catch (IOException | XMLStreamException e) {
            e.printStackTrace();
        }
    }

    private List<String> fileList = null;

    public static void zipFolder(String folderPath) {
        String zipFile = folderPath + ".zip";
        String rootPath = folderPath.substring(0, folderPath.lastIndexOf(File.separator));
        FileOutputStream fos = null;
        ZipOutputStream zos = null;
        //ZipArchiveOutputStream zos = null;

        try {
            fos = new FileOutputStream(zipFile);
            zos = new ZipOutputStream(fos);
            //zos = new ZipArchiveOutputStream();
            SLF4J.LOGGER.info("\nOutput to Zip: {} ", zipFile);

            zipNode(new File(folderPath), zos, rootPath);
            zos.close();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static void zipNode(File node,
                                ZipOutputStream zos,
                                //ZipArchiveOutputStream zos,
                                String rootPath) throws IOException {
        if (node.isDirectory()) {
            String folderName = node.getPath().substring(rootPath.length() + 1) + File.separator;
            if (folderName.length() > 0) {
                SLF4J.LOGGER.debug("Adding folder: {}", folderName);
                zos.putNextEntry(new ZipEntry(folderName));
                zos.closeEntry();
            }
            for (String filename : node.list()) {
                zipNode(new File(node, filename), zos, rootPath);
            }
            return;
        } else if (node.getName().endsWith((".DS_Store"))) {
            return;
        }

        byte[] buffer = new byte[1024];
        String fileName = node.getPath().substring(rootPath.length() + 1);
        ZipEntry ze = new ZipEntry(fileName);
        ze.setSize(node.length());
        ze.setCompressedSize(node.length());
        if (true) {
            ze.setMethod(ZipEntry.STORED);
            ze.setCompressedSize(node.length());
            CRC32 crc32 = new CRC32();
            try (FileInputStream in = new FileInputStream(node.getAbsoluteFile())) {
                int len;
                while ((len = in.read(buffer)) > 0) {
                    crc32.update(buffer, 0, len);
                }
            }
            ze.setCrc(crc32.getValue());
        }

        zos.putNextEntry(ze);

        FileInputStream in = new FileInputStream(node);
        SLF4J.LOGGER.debug("Adding file: {} with size {}", fileName, ze.getSize());
        int len;
        while ((len = in.read(buffer)) > 0) {
            zos.write(buffer, 0, len);
        }
        zos.closeEntry();
    }

    private String resolveResourcePath(String resourceName) {
        Path resources = Paths.get(this.getClass().getResource("/").getPath());
        return resources.toAbsolutePath() + "/" + resourceName;
    }

    private void exportImages(String exportPath) {
        for (Image img : this.getImages().values()) {
            img.exportToFile(exportPath);
        }

    }
}
