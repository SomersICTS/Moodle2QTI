import utils.SLF4J;
import utils.XMLParser;
import utils.XMLWriter;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class QuestionBank {

    public static final String QTI_DEFAULTS = "qti21_defaults";
    public static final String QTI_MEDIAFILES = "mediafiles";

    public static final String QTI_TOOL_NAME = "Testvision Online";
    public static final String QTI_TOOL_VERSION = "41.0.9240";

    public static final String QTI_MC_INTERACTION = "choiceInteraction";
    public static final String QTI_SHORTANSWER_INTERACTION = "extendedTextInteraction";
    public static final String QTI_HOTSPOT_INTERACTION = "positionObjectStage";
    public static final String QTI_MIXED_INTERACTION = "div";

    public static final String TV_TEXTBLOCK_CLASS = "textblock tvblock tvcss_1";
    public static final String TV_TEXTBLOCK_ID = "textBlockId_";
    public static final int TV_FIRST_TEXTBLOCK_ID = 101;
    public static final String TV_TBZONE_CLASS = "rte_zone tveditor1";
    public static final String TV_INTERACTIONBLOCK_CLASS = "interactieblok";

    public static final String XML_DEFAULT_NAMESPACE = "http://www.imsglobal.org/xsd/imscp_v1p1";
    public static final String XML_QTI_NAMESPACE = "http://www.imsglobal.org/xsd/imsqti_v2p0";
    public static final String[] MOODLE_STYLE_SHEETS = {"css/from_moodle.css", "css/TvEditor.css"};

    Category rootCategory = new Category("", null, this);
    String language = "unknown";

    public String getLanguage() {
        return language;
    }

    public Category getCurrentCategory() {
        return currentCategory;
    }

    private static int nextTextBlockId = TV_FIRST_TEXTBLOCK_ID;

    public static int getNextTextBlockId() {
        return nextTextBlockId++;
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
        System.out.printf("\nStart loading of %s...\n", resourceName);

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
                    case "regexp":
                        this.questions.add(
                                SAQuestion.createFromMXML(xmlParser, this.currentCategory, type));
                        break;
                    case "truefalse":
                        this.questions.add(
                                TFQuestion.createFromMXML(xmlParser, this.currentCategory));
                        break;
                    case "numerical":
                        //case "calculated":
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
                    case "gapselect":
                        this.questions.add(
                                GapSelectQuestion.createFromMXML(xmlParser, this.currentCategory));
                        break;
                    case "description":
                        this.questions.add(
                                DescrQuestion.createFromMXML(xmlParser, this.currentCategory));
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
        this.rootCategory.sortContent();
    }

    private Category parseCategoryFromMXML(XMLParser xmlParser) throws XMLStreamException {
        String fullName = parseFormattedElementFromMXML(xmlParser, "category", "new category");
        String info = parseFormattedElementFromMXML(xmlParser, "info", fullName);
        int id = xmlParser.acceptOptionalElementValue("idnumber", 0);
        if (fullName != null) {
            String[] path = fullName.replace("//", "+").split("/");
            Category category = this.rootCategory.findOrCreate(path);
            category.setInfo(info);
            category.setId(id);
            return category;
        }
        return null;
    }

    public static String countsToString(Map<String, int[]> scoresCounts) {
        String result = "{";
        String separator = "";
        for (Map.Entry<String, int[]> e : scoresCounts.entrySet()) {
            result += separator + e.getKey() + "=" + Arrays.toString(e.getValue());
            separator = ", ";
        }
        result += "}";
        return result;
    }

    public int showSummary() {
        System.out.println("\nQuestionbank summary:");
        int totalNum = this.rootCategory.showSummary(0);
        System.out.printf("Total %d questions with %d images.\n",
                totalNum, this.images.size());

        Map<String, int[]> scoresCounts = this.rootCategory.mapQuestionScores(null);
        System.out.println("Default grade map by question type:");
        System.out.println(countsToString(scoresCounts));
        System.out.println("Default grade exceptions:");
        System.out.println(this.rootCategory.findDeviatingScores(Category.findDefaultScores(scoresCounts), null));
        return totalNum;
    }

    public static String escapeToHTMLEntities(String text) {
        text = text.replace("&", "&amp;");
        text = text.replace("<", "&lt;").replace(">", "&gt;");
        return text;
    }

    public static String deEscapeHTMLEntities(String text) {
        text = text.replace("&nbsp;", " ");
        text = text.replace("&lt;", "<").replace("&gt;", ">");
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

    private static void copyDirectoryRecursively(String sourcePath, String copyPath) throws IOException {
        Path sourceDir = Paths.get(sourcePath);
        Path copyDir = Paths.get(copyPath);

        // Traverse the file tree and copy each file/directory.
        for (Path path : Files.walk(sourceDir).collect(Collectors.toList())) {
            Path targetPath = copyDir.resolve(sourceDir.relativize(path));
            Files.copy(path, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void setLanguage() {
        int numThe = this.rootCategory.countWords(" the ");
        int numDe = this.rootCategory.countWords(" de ");
        int numEen = this.rootCategory.countWords(" een ");
        if (numDe + numEen > numThe) {
            this.language = "Dutch";
        } else {
            this.language = "English";
        }
    }

    public void export(String format, String exportName) {
        this.setLanguage();
        exportToQTI21Resource(exportName);
    }

    public void exportToQTI21Resource(String resourceName) {

        System.out.printf("Exporting into '%s' (%s) ...\n", resourceName, this.language);
        nextTextBlockId = TV_FIRST_TEXTBLOCK_ID;

        try {
            String defaultsPath = this.resolveResourcePath(QTI_DEFAULTS);
            String exportPath = this.resolveResourcePath(resourceName);
            Files.createDirectories(Paths.get(exportPath));
            deleteDirectory(new File(exportPath));
            copyDirectoryRecursively(defaultsPath, exportPath);

            exportImages(exportPath + "/" + this.rootCategory.getMediaFilesFolder());
            XMLWriter manifest = new XMLWriter(exportPath + "/imsmanifest.xml");
            manifest.writeStartDocument();
            manifest.writeStartElement("manifest");
            manifest.writeAttribute("identifier", "MANIFEST-QTI-1");
            manifest.writeAttribute("\n\txmlns:xml", "http://www.w3.org/XML/1998/namespace");
            //manifest.writeAttribute("\n\txmlns", "http://www.imsglobal.org/xsd/imscp_v1p1");
            //manifest.writeAttribute("\n\txmlns:qti", "http://www.imsglobal.org/xsd/imsqti_v2p0");
            manifest.writeDefaultNamespace(XML_DEFAULT_NAMESPACE);
            manifest.writeNamespace("qti", XML_QTI_NAMESPACE);
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

    public static void zipFolder(String folderPath) {
        System.out.println("Packaging the .zip file...");

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

    private static void zipNode(File node, ZipOutputStream zos, String rootPath) throws IOException {
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

    public String parseFormattedElementFromMXML(XMLParser xmlParser, String tag, String context) throws XMLStreamException {
        xmlParser.findBeginTag(tag);
        String format = parseFormatAttributeFromMXML(xmlParser, context);
        xmlParser.nextTag();
        String formattedText = parseFormattedTextFromMXML(xmlParser, format, context);
        formattedText = processHTMLAttributeInTag("src", "img", this::imageSrcProcessor, formattedText, context);
        formattedText = Image.parseMXMLForText(xmlParser, this, formattedText);
        xmlParser.findAndAcceptEndTag(tag);

        return formattedText;
    }

    public String imageSrcProcessor(String url, String context) {
        int nextParam = url.toLowerCase().indexOf("?time=");
        if (nextParam >= 0) {
            url = url.substring(0, nextParam);
        }

        if (url.contains(".informatica.hva.nl/")) {
            Image image = null; // Image.retrieveFromHTTP(url, this);
            if (image != null && image.getSize() > 0) {
                return "@@PLUGINFILE@@" + image.getVersionedFullURL();
            } else {
                SLF4J.LOGGER.error("Cannot download image from url '{}' in '{}'", url, context);
            }
        }
        return url;
    }

    public static String parseFormatAttributeFromMXML(XMLParser xmlParser, String context) {
        String format = xmlParser.getAttributeValue(null, "format");
        if (format != null &&
                !format.equals("html") &&
                !format.equals("moodle_auto_format") &&
                !format.equals("plain_text")
        ) {
            SLF4J.LOGGER.error("Ignored unknown text format '{}' in '{}'", format, context);
            return null;
        }
        return format;
    }

    public static String parseFormattedTextFromMXML(XMLParser xmlParser, String format, String context) throws XMLStreamException {
        String formattedText = xmlParser.findAndAcceptElementValue("text", null);
        //if (format != null && format.equals("moodle_auto_format")) {
        //    SLF4J.LOGGER.debug("MOODLE_AUTO_FORMAT: '{}' in '{}'", formattedText, context);
        //}
        if (format == null ||
                format.equals("html") //|| format.equals("moodle_auto_format")
        ) {
            // if (formattedText.startsWith("<![CDATA[") && formattedText.endsWith("]]>")) formattedText = formattedText.substring(9,formattedText.length()-3);
            formattedText = formattedText.replace("<ol><ol>", "<ol>").replace("</ol></ol>", "</ol>");
            formattedText = checkSelfClosingHTMLTag("img", formattedText, context);
            formattedText = checkHTMLAttributeInTag("alt", "img", "none", formattedText, context);
            formattedText = checkSelfClosingHTMLTag("br", formattedText, context);
            formattedText = replaceHTMLTag("br", "br", "", formattedText, context);

            //countAndFlagInvalidWords(new String[]{".informatica.hva.nl/"}, formattedText, context);
            //formattedText = processHTMLAttributeInTag("src", "img", QuestionBank::imageSrcProcessor, formattedText, context);
            //formattedText = checkSrcAttribute(formattedText, context);

            // ADS
            formattedText = removeHTMLTag("style", formattedText, context);
            formattedText = removeHTMLTag("link", formattedText, context);
            formattedText = removeHTMLTag("o:p", formattedText, context);
            formattedText = removeHTMLTag("colgroup", formattedText, context);
            formattedText = removeHTMLTag("col", formattedText, context);

            formattedText = replaceHTMLTag("font", "span", "", formattedText, context);
            formattedText = replaceHTMLTag("span style=\"font-weight: bold", "strong", "", formattedText, context);
            formattedText = replaceHTMLTag("span style=\"font-family: courier", "code", "", formattedText, context);

            formattedText = replaceHTMLTagBR("div style=\"margin-left: 20px", "div", "  ", formattedText, context);
            formattedText = replaceHTMLTagBR("div style=\"margin-left: 30px", "div", "   ", formattedText, context);
            formattedText = replaceHTMLTagBR("div style=\"margin-left: 40px", "div", "    ", formattedText, context);
            formattedText = replaceHTMLTagBR("div style=\"margin-left: 80px", "div", "        ", formattedText, context);
            formattedText = replaceHTMLTagBR("div style=\"margin-left: 120px", "div", "            ", formattedText, context);
            formattedText = replaceHTMLTagBR("div style=\"margin-left: 160px", "div", "                ", formattedText, context);

            formattedText = removeInnerTagNesting("table", "p", formattedText, context);
            //formattedText = removeInnerTagNesting("pre", "p", formattedText, context);
            formattedText = removeInnerTagNesting("pre", "pre", formattedText, context);

            formattedText = removeOuterTagNesting("div", "div", formattedText, context);
            //formattedText = removeOuterTagNesting("ol", "p", formattedText, context);

            // WEF
            formattedText = removeHTMLAttribute("data-sheets-value", formattedText, context);
            formattedText = removeHTMLAttribute("data-sheets-userformat", formattedText, context);
            formattedText = removeHTMLTag("script", formattedText, context);
            formattedText = removeHTMLAttributeFromTag("name", "p", formattedText, context);
            formattedText = removeOuterTagNesting("p", "pre", formattedText, context);

            // DB1
            formattedText = removeHTMLTag("span data-fontsize=", formattedText, context);
            formattedText = removeHTMLTag("span data-ccp-parastyle=", formattedText, context);
            formattedText = removeOuterTagNesting("p", "table", formattedText, context);
            formattedText = removeInnerTagNesting("table", "div", formattedText, context);

            // SE
            formattedText = removeOuterTagNesting("span", "p", formattedText, context);
            formattedText = removeInnerTagNesting("p", "h1", formattedText, context);
            formattedText = fixInnerOuterTagNesting("div", "pre", formattedText, context);
        }
        return formattedText;
    }

    public static String fixPlainTextforQTI21(String text) {
        text = text.replace("<", "≺");
        text = text.replace(">", "≻");
        text = text.replace("&", "\uFF06");
        return text;
    }

    public static String fixHTMLTagCharactersForQTI21(String text, String context) {
        int nextPos = 0;
        while (0 <= (nextPos = text.indexOf('<', nextPos))) {
            char nextCh = (nextPos < text.length()-1 ? text.charAt(nextPos+1) : '\0');
            if (nextCh != '/' && nextCh != '!' && nextCh != '?' && ! Character.isAlphabetic(nextCh)) {
                text = text.substring(0, nextPos) + "≺" + text.substring(nextPos+1);
            }
            nextPos++;
        }
        /*
        nextPos = 0;
        while (0 <= (nextPos = text.indexOf('>', nextPos))) {
            char prevCh = (nextPos > 0 ? text.charAt(nextPos-1) : '\0');
            if ( prevCh != '"' && prevCh != '/' && prevCh != '-' && prevCh != '?' && ! Character.isAlphabetic(prevCh) && ! Character.isDigit(prevCh)) {
                text = text.substring(0, nextPos) + "≻" + text.substring(nextPos+1);
            }
            nextPos++;
        }
         */
        nextPos = 0;
        while (0 <= (nextPos = text.indexOf('&', nextPos))) {
            int semiPos = nextPos+1;
            while (semiPos < text.length() && Character.isAlphabetic(text.charAt(semiPos))) semiPos++;
            if (semiPos >= text.length() || text.charAt(semiPos) != ';') {
                text = text.substring(0, nextPos) + "\uFF06" + text.substring(nextPos+1);
            }
            nextPos++;
        }
        return text;
    }


    public static String fixHTMLforQTI21(String text, String context) {
        text = text.replace("<u>", "<b>").replace("</u>", "</b>");
        text = removeOuterTagNesting("p", "p", text, context);
        text = removeOuterTagNesting("p", "ol", text, context);
        text = removeOuterTagNesting("p", "ul", text, context);
        text = removeOuterTagNesting("p", "div", text, context);
        text = removeInnerTagNesting("i", "p", text, context);
        text = removeInnerTagNesting("b", "p", text, context);
        //text = text.replace("<pre>", "<code>").replace("</pre>", "</code>");
        text = text.replace("&nbsp;", " ");
        text = text.replace("&radic;", "√");
        text = fixHTMLTagCharactersForQTI21(text, context);

        text = removeHTMLAttribute("role", text, context);
        text = removeHTMLAttribute("style", text, context);
        text = removeHTMLAttribute("border", text, context);
        text = removeHTMLAttribute("lang", text, context);
        // from ADS:
        text = removeHTMLAttribute("valign", text, context);
        text = removeHTMLAttribute("cellspacing", text, context);
        text = removeHTMLAttribute("cellpadding", text, context);
        text = removeHTMLAttribute("bordercolor", text, context);
        text = removeHTMLAttribute("title", text, context);
        text = removeHTMLAttribute("frame", text, context);
        text = removeHTMLAttributeFromTag("width", "td", text, context);
        text = removeHTMLAttributeFromTag("width", "table", text, context);

        // from WEF
        text = removeHTMLNonAlfaNumIdAttribute(text, context);

        // from DS:
        text = removeHTMLAttribute("hspace", text, context);
        text = removeHTMLAttribute("vspace", text, context);

        // from db1
        text = removeHTMLAttribute("paraid", text, context);
        text = removeHTMLAttribute("paraeid", text, context);
        text = removeHTMLAttribute("contenteditable", text, context);
        text = removeHTMLAttribute("data-contrast", text, context);
        text = removeHTMLAttribute("data-ccp-props", text, context);
        text = removeHTMLAttribute("xml:lang", text, context);
        text = removeHTMLAttribute("aria-hidden", text, context);
        text = removeHTMLAttribute("data-table-id", text, context);
        text = removeHTMLAttribute("data-tablestyle", text, context);
        text = removeHTMLAttribute("data-tablelook", text, context);
        text = removeHTMLAttribute("data-celllook", text, context);
        text = removeHTMLAttribute("data-size", text, context);
        text = removeHTMLAttribute("nowrap", text, context);
        text = removeHTMLTag("intgrtsrgls", text, context);

        // from infra
        text = removeHTMLAttribute("dir", text, context);
        text = removeHTMLAttribute("original-url", text, context);
        text = removeHTMLAttribute("target", text, context);

        text = text.replace("<p></p>", "<br/><br/>");
        text = removeInnerTagNesting("pre", "br", text.replace("<br/>", "<br/>\n"), context);
        text = text.replace("</p>", "</p>\n").replace("</tr>", "</tr>\n");
        text = text.replace("<span></span>", "");
        return text;
    }

    private static String replaceHTMLTag(String sTag, String rTag, String extraText, String text, String context) {
        String plainTag = sTag.split(" ")[0];

        int nextOpen = 0;
        String lcText = text.toLowerCase();
        while (0 <= (nextOpen = findNextOpenTag(sTag, lcText, nextOpen))) {
            int openEnd = text.indexOf('>', nextOpen) + 1;
            int endClose = findSelfClosingEnd(lcText, nextOpen);
            if (endClose < 0) {
                endClose = findMatchingClosingTagIndex(plainTag, lcText, nextOpen);
                if (endClose < 0 || openEnd <= 0) {
                    SLF4J.LOGGER.error("replaceTag: <{}> and </{}> not properly nested in '{}' of '{}'", sTag, plainTag, text, context);
                    return text;
                } else {
                    text = text.substring(0, endClose - 3 - plainTag.length()) + "</" + rTag + ">" + text.substring(endClose);
                    text = text.substring(0, nextOpen) + "<" + rTag + ">" + extraText + text.substring(openEnd);
                }
            } else {
                text = text.substring(0, nextOpen) + "<" + rTag + "/>" + text.substring(endClose);
            }
            lcText = text.toLowerCase();
            nextOpen += rTag.length() + 2;
        }

        return text;
    }

    private static String replaceHTMLTagBR(String sTag, String rTag, String extraText, String text, String context) {
        String plainTag = sTag.split(" ")[0];

        int nextOpen = 0;
        String lcText = text.toLowerCase();
        while (0 <= (nextOpen = findNextOpenTag(sTag, lcText, nextOpen))) {
            int openEnd = text.indexOf('>', nextOpen) + 1;
            int endClose = findSelfClosingEnd(lcText, nextOpen);
            if (endClose < 0) {
                endClose = findMatchingClosingTagIndex(plainTag, lcText, nextOpen);
                if (endClose < 0 || openEnd <= 0) {
                    SLF4J.LOGGER.error("replaceTag: <{}> and </{}> not properly nested in '{}' of '{}'", sTag, plainTag, text, context);
                    return text;
                } else {
                    String embedded = text.substring(openEnd, endClose - 3 - plainTag.length());
                    embedded = embedded.replace("<br/>", "<br/>" + extraText);
                    text = text.substring(0, nextOpen) + "<" + rTag + ">" +
                            extraText + embedded +
                            "</" + rTag + ">" + text.substring(endClose);
                }
            } else {
                text = text.substring(0, nextOpen) + "<" + rTag + "/>" + text.substring(endClose);
            }
            lcText = text.toLowerCase();
            nextOpen += rTag.length() + 2;
        }

        return text;
    }

    private static String checkSelfClosingHTMLTag(String tag, String text, String context) {
        int nextOpen = 0;
        String lcText = text.toLowerCase();
        while (0 <= (nextOpen = findNextOpenTag(tag, lcText, nextOpen))) {
            int closeEnd = findSelfClosingEnd(lcText, nextOpen);
            int openEnd = lcText.indexOf(">", nextOpen);
            if (closeEnd < 0) {
                text = text.substring(0, openEnd) + "/" + text.substring(openEnd);
                lcText = text.toLowerCase();
            }
            nextOpen += tag.length();
        }
        return text;
    }

    private static String checkImgSrcAttribute(String text, String context) {
        int i2 = 0;
        int i1;
        while (0 <= (i1 = text.toLowerCase().indexOf(" src=\"", i2))) {
            i2 = text.indexOf("\"", i1 + 6);
            int i3 = text.indexOf("?time=", i1);
            if (i3 < 0 || i3 > i2) continue;
            String errorReference = text.substring(i3, i2);
            text = text.replace(errorReference, "");
            SLF4J.LOGGER.warn("Removed parameter '{}' from src attribute in '{}'", errorReference, context);
            i2 = i3;
        }
        return text;
    }

    public static int countAndFlagInvalidWords(String[] words, String text, String context) {
        int total = 0;
        for (String w : words) {
            int count = countWords(w, text);
            if (count > 0 && total == 0) {
                SLF4J.LOGGER.error("Found invalid character(s) '{}' in '{}' of '{}'", w, text, context);
            }
            total += count;
        }
        return total;
    }

    public static String removeHTMLTag(String tag, String text, String context) {
        String plainTag = tag.split(" ")[0];
        boolean first = true;
        int nextOpen = 0;
        String lcText = text.toLowerCase();
        while (0 <= (nextOpen = findNextOpenTag(tag, lcText, nextOpen))) {
            int endClose = findSelfClosingEnd(lcText, nextOpen);
            if (endClose < 0) {
                endClose = findMatchingClosingTagIndex(plainTag, lcText, nextOpen);
                if (endClose < 0) {
                    SLF4J.LOGGER.error("Cannot remove tag <{}> from '{}' in '{}'", tag, text, context);
                    return text;
                }
            }
            if (first) {
                SLF4J.LOGGER.warn("Removed tag '{}' from '{}'", text.substring(nextOpen, endClose), context);
                first = false;
            }
            text = text.substring(0, nextOpen) + text.substring(endClose);
            lcText = text.toLowerCase();
        }
        return text;
    }

    public static String removeHTMLAttribute(String attribute, String text, String context) {
        int nextAttr = 0;
        boolean first = true;
        while (0 <= (nextAttr = text.toLowerCase().indexOf(" " + attribute + "=\"", nextAttr))) {
            int endAttr = indexOfNonEscaped("\"", text, nextAttr + attribute.length() + 3);
            if (endAttr > nextAttr) {
                if (first) {
                    SLF4J.LOGGER.debug("Removed attribute '{}' from '{}'", text.substring(nextAttr, endAttr + 1), context);
                    first = false;
                }
                text = text.substring(0, nextAttr) + text.substring(endAttr + 1);
            } else {
                SLF4J.LOGGER.error("Could not isolate attribute '{}' from '{}' in '{}'", attribute, text, context);
                return text;
            }
        }
        return text;
    }

    public static String removeHTMLNonAlfaNumIdAttribute(String text, String context) {
        int nextAttr = 0;
        while (0 <= (nextAttr = text.toLowerCase().indexOf(" id=\"", nextAttr))) {
            int endAttr = indexOfNonEscaped("\"", text, nextAttr + 5);
            if (endAttr > nextAttr) {
                if (!Character.isLetter(text.charAt(nextAttr + 5))) {
                    SLF4J.LOGGER.debug("Removed attribute '{}' from '{}'", text.substring(nextAttr, endAttr + 1), context);
                    text = text.substring(0, nextAttr) + text.substring(endAttr + 1);
                } else {
                    nextAttr += 5;
                }
            } else {
                SLF4J.LOGGER.error("Could not isolate attribute 'id' from '{}' in '{}'", text, context);
                nextAttr += 5;
            }
        }
        return text;
    }

    public static String removeHTMLAttributeFromTag(String attribute, String tag, String text, String context) {
        String openTag = "<" + tag + " ";
        boolean first = true;
        int nextAttr = 0;
        while (0 <= (nextAttr = text.toLowerCase().indexOf(" " + attribute + "=\"", nextAttr))) {
            String prefix = text.substring(0, nextAttr + 1).toLowerCase();
            int prevTag = prefix.lastIndexOf('<');
            if (prevTag >= 0 && prefix.substring(prevTag).startsWith(openTag)) {
                int endAttr = indexOfNonEscaped("\"", text, nextAttr + attribute.length() + 3);
                if (endAttr > nextAttr) {
                    if (first) {
                        SLF4J.LOGGER.debug("Removed {}-attribute '{}' from '{}'", tag, text.substring(nextAttr, endAttr + 1), context);
                        first = false;
                    }
                    text = text.substring(0, nextAttr) + text.substring(endAttr + 1);
                } else {
                    SLF4J.LOGGER.error("Could not isolate {}-attribute '{}' from '{}' in '{}'", tag, attribute, text, context);
                    return text;
                }
            } else {
                nextAttr += attribute.length();
            }
        }
        return text;
    }

    public static String checkHTMLAttributeInTag(String attribute, String tag, String defaultValue, String text, String context) {
        int nextOpen = 0;
        String lcText = text.toLowerCase();
        while (0 <= (nextOpen = findNextOpenTag(tag, lcText, nextOpen))) {
            int endOpen = indexOfNonEscaped(">", text, nextOpen + 2);
            if (endOpen < 0) {
                SLF4J.LOGGER.error("Cannot check tag <{}> in '{}' of '{}'", tag, text, context);
                return text;
            }
            int nextAttr = lcText.indexOf(" " + attribute + "=\"", nextOpen + 2);
            if (nextAttr < 0 || nextAttr > endOpen) {
                if (text.charAt(endOpen - 1) == '/') endOpen--;
                text = text.substring(0, endOpen) + " " + attribute + "=\"" + defaultValue + "\"" + text.substring(endOpen);
                lcText = text.toLowerCase();
            }
            nextOpen = endOpen;
        }
        return text;
    }

    public static String processHTMLAttributeInTag(String attribute, String tag, BinaryOperator<String> processor, String text, String context) {
        int nextOpen = 0;
        String lcText = text.toLowerCase();
        while (0 <= (nextOpen = findNextOpenTag(tag, lcText, nextOpen))) {
            int endOpen = indexOfNonEscaped(">", text, nextOpen + 2);
            if (endOpen < 0) {
                SLF4J.LOGGER.error("Cannot process tag <{}> in '{}' of '{}'", tag, text, context);
                return text;
            }
            int nextAttr = lcText.indexOf(" " + attribute + "=\"", nextOpen + 2);
            if (nextAttr > 0 && nextAttr < endOpen) {
                nextAttr += attribute.length() + 3;
                int endAttr = indexOfNonEscaped("\"", lcText, nextAttr);
                if (endAttr > nextAttr && endAttr < endOpen) {
                    String original = text.substring(nextAttr, endAttr);
                    String replacement = processor.apply(original, context);
                    if (!replacement.equals(original)) {
                        SLF4J.LOGGER.info("Replaced {}:{}-attribute '{}' by '{} in '{}'", tag, attribute,
                                original, replacement, context);
                        text = text.substring(0, nextAttr) + replacement + text.substring(endAttr);
                        endOpen += replacement.length() - original.length();
                        lcText = text.toLowerCase();
                    }
                } else {
                    SLF4J.LOGGER.error("Could not isolate {}-attribute '{}' from '{}' in '{}'", tag, attribute, text, context);
                }
            }
            nextOpen = endOpen;
        }
        return text;
    }

    public static int findSelfClosingEnd(String text, int from) {
        if (text.charAt(from) == '<' && text.charAt(from + 1) != '/') {
            int openEnd = text.indexOf('>', from);
            int selfClosingEnd = text.indexOf("/>", from);
            if (selfClosingEnd > from && selfClosingEnd < openEnd) {
                return selfClosingEnd + 2;
            }
        }
        return -1;
    }

    public static int minimumPositive(int a, int b) {
        if (a < 0) return b;
        else if (b < 0) return a;
        else if (a < b) return a;
        else return b;
    }

    public static int findNextOpenTag(String tag, String text, int from) {
        //text = text.replace('&','$');
        int open1 = text.indexOf("<" + tag + ">", from);
        int open2 = text.indexOf("<" + tag + "/>", from);
        int open3 = text.indexOf("<" + tag + " ", from);
        if (open3 < 0) open3 = text.indexOf("<" + tag + "\"", from);
        if (open3 < 0) open3 = text.indexOf("<" + tag + ";", from);
        if (open3 < 0) open3 = text.indexOf("<" + tag + ",", from);
        if (open3 < 0) return minimumPositive(open1, open2);
        else if (open2 < 0) return minimumPositive(open1, open3);
        else if (open1 < 0) return minimumPositive(open2, open3);
        else if (open2 < open3 || open1 < open3) return minimumPositive(open1, open2);
        else return open3;
    }

    public static String removeTagAt(String text, int from) {
        if (text.charAt(from) == '<') {
            int tagEnd = text.indexOf('>', from);
            if (tagEnd > from) {
                return text.substring(0, from) + text.substring(tagEnd + 1);
            }
        }
        return text;
    }

    public static int findMatchingClosingTagIndex(String tag, String text, int from) {
        String openTag = "<" + tag + ">";
        String openTag1 = "<" + tag + " ";
        String closeTag = "</" + tag + ">";
        int selfClosingEnd;
        if (text.substring(from, from + openTag1.length()).equalsIgnoreCase(openTag1) &&
                0 <= (selfClosingEnd = findSelfClosingEnd(text, from))) {
            return selfClosingEnd;
        }

        int nesting = 1;
        from += 3;
        while (nesting > 0) {
            int nextOpen = findNextOpenTag(tag, text, from);

            int nextClose = text.indexOf(closeTag, from);
            if (nextOpen >= 0 && (nextOpen < nextClose || nextClose < 0)) {
                int selfClose = findSelfClosingEnd(text, nextOpen);
                if (selfClose > nextOpen) {
                    from = selfClose;
                } else {
                    nesting++;
                    from = nextOpen + 3;
                }
            } else if (nextClose >= 0 && (nextClose < nextOpen || nextOpen < 0)) {
                nesting--;
                from = nextClose + closeTag.length();
            } else {
                return -1;
            }
        }
        return from;
    }

    private static String fixInnerOuterTagNesting(String oTag, String iTag, String text, String context) {
        boolean firstError = true;
        String lcText = text.toLowerCase();
        int nextO = findNextOpenTag(oTag, lcText, 0);
        int nextI = findNextOpenTag(iTag, lcText, 0);
        while (nextO >= 0 || nextI >= 0) {
            if (nextO >= 0 && (nextO <= nextI || nextI < 0)) {
                int endClose = findSelfClosingEnd(lcText, nextO);
                if (endClose < 0) {
                    endClose = findMatchingClosingTagIndex(oTag, lcText, nextO);

                    while (endClose < 0) {
                        if (firstError) {
                            SLF4J.LOGGER.warn("FIOTN({},{}): <{}> and </{}> not properly nested in '{}' of '{}'", oTag, iTag,
                                    oTag, oTag, text, context);
                            firstError = false;
                        }
                        text += "</" + oTag + ">";
                        lcText = text.toLowerCase();
                        endClose = findMatchingClosingTagIndex(oTag, lcText, nextO);
                    }

                    String embeddedText = fixInnerOuterTagNesting(oTag, iTag,
                            text.substring(nextO + 3, endClose - 3), context);

                    // replace inner part
                    text = text.substring(0, nextO + 3) + embeddedText + text.substring(endClose - 3);
                    lcText = text.toLowerCase();
                    nextO += embeddedText.length() + 6;
                } else {
                    nextO = endClose;
                }
                nextI = nextO;
            } else if (nextI >= 0 && (nextI <= nextO || nextO < 0)) {
                int endClose = findSelfClosingEnd(lcText, nextI);
                if (endClose < 0) {
                    endClose = findMatchingClosingTagIndex(iTag, lcText, nextI);

                    while (endClose < 0) {
                        if (firstError) {
                            SLF4J.LOGGER.warn("FIOTN({},{}): <{}> and </{}> not properly nested in '{}' of '{}'", oTag, iTag,
                                    iTag, iTag, text, context);
                            firstError = false;
                        }
                        text += "</" + iTag + ">";
                        lcText = text.toLowerCase();
                        endClose = findMatchingClosingTagIndex(iTag, lcText, nextI);
                    }

                    String embeddedText = fixInnerOuterTagNesting(iTag, oTag,
                            text.substring(nextI + 3, endClose - 3), context);

                    // replace inner part
                    text = text.substring(0, nextI + 3) + embeddedText + text.substring(endClose - 3);
                    lcText = text.toLowerCase();
                    nextI += embeddedText.length() + 6;
                } else {
                    nextI = endClose;
                }
                nextO = nextI;
            }
            nextO = findNextOpenTag(oTag, lcText, nextO);
            nextI = findNextOpenTag(iTag, lcText, nextI);
        }

        return text;
    }

    private static String removeOuterTagNesting(String oTag, String iTag, String text, String context) {
        int nextOpen = 0;
        boolean firstError = true;
        String lcText = text.toLowerCase();
        while (0 <= (nextOpen = findNextOpenTag(oTag, lcText, nextOpen))) {
            int endClose = findSelfClosingEnd(lcText, nextOpen);
            if (endClose < 0) {
                endClose = findMatchingClosingTagIndex(oTag, lcText, nextOpen);
                if (endClose < 0) {
                    if (findNextOpenTag(iTag, lcText, nextOpen + 3) >= 0) {
                        if (firstError) {
                            SLF4J.LOGGER.warn("ROTN({},{}): <{}> and </{}> not properly nested in '{}' of '{}'", oTag, iTag,
                                    oTag, oTag, text, context);
                            firstError = false;
                        }
                        // remove start tag only
                        text = removeTagAt(text, nextOpen);
                    } else {
                        // done
                        return text;
                    }
                } else {
                    String embeddedText = lcText.substring(nextOpen, endClose);
                    if (findNextOpenTag(iTag, embeddedText, 3) >= 0) {
                        // removing outer nesting
                        text = removeTagAt(text, endClose - (oTag.length() + 3));
                        text = removeTagAt(text, nextOpen);

                    } else {
                        nextOpen = endClose;
                    }
                }
                lcText = text.toLowerCase();
            } else {
                // skip self closing outer tag
                nextOpen = endClose;
            }
        }

        return text;
    }

    private static String removeInnerTagNesting(String oTag, String iTag, String text, String context) {
        int nextOpen = 0;
        String lcText = text.toLowerCase();
        while (0 <= (nextOpen = findNextOpenTag(oTag, lcText, nextOpen))) {
            int endClose = findSelfClosingEnd(lcText, nextOpen);
            if (endClose < 0) {
                endClose = findMatchingClosingTagIndex(oTag, lcText, nextOpen);
                if (endClose < 0) {
                    if (findNextOpenTag(iTag, lcText, nextOpen + 3) >= 0) {
                        SLF4J.LOGGER.error("RITN({},{}): <{}> and </{}> not properly nested in '{}' of '{}'", oTag, iTag,
                                oTag, oTag, text, context);
                    }
                    return text;
                } else {
                    String embeddedText = text.substring(nextOpen + 3, endClose - 3);
                    int nextI = 0;
                    while (0 <= (nextI = findNextOpenTag(iTag, embeddedText.toLowerCase(), nextI))) {
                        embeddedText = removeTagAt(embeddedText, nextI);
                    }
                    nextI = 0;
                    while (0 <= (nextI = embeddedText.toLowerCase().indexOf("</" + iTag + ">", nextI))) {
                        embeddedText = removeTagAt(embeddedText, nextI);
                    }

                    // replace inner part
                    text = text.substring(0, nextOpen + 3) + embeddedText + text.substring(endClose - 3);
                    lcText = text.toLowerCase();
                    nextOpen += embeddedText.length() + 6;
                }
            } else {
                nextOpen = endClose;
            }
        }

        return text;
    }

    public static int indexOfNonEscaped(String seq, String s, int from) {
        int idx = s.indexOf(seq, from);
        if (idx >= from && idx > 0 && s.charAt(idx - 1) == '\\') {
            return indexOfNonEscaped(seq, s, idx + 1);
        }
        return idx;
    }

    public static String deEscape(String s) {
        int backSlash = s.indexOf('\\');
        while (backSlash >= 0 && backSlash < s.length() - 1) {
            s = s.substring(0, backSlash) + s.substring(backSlash + 1);
            backSlash = s.indexOf('\\', backSlash + 1);
        }
        return s;
    }

    public static int countWords(String word, String text) {
        int numWords = 0;
        int pos = 0;
        while (0 <= (pos = text.indexOf(word, pos))) {
            numWords++;
            pos += word.length();
        }
        return numWords;
    }
}
