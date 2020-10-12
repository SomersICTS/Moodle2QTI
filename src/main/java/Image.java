import utils.SLF4J;
import utils.XMLParser;

import javax.xml.stream.XMLStreamException;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class Image {

    public String getName() {
        return name;
    }
    public String getFullName() {
        return path+name;
    }
    public String getVersionedFullName() { return path+getVersionedName(); }
    public String getFullURL() {
        return this.getFullName().replace(" ", "%20");
    }
    public String getVersionedFullURL() {
        return this.getVersionedFullName().replace(" ", "%20");
    }
    public int getSize() {
        return this.data.length;
    }
    public int getDataHash() {
        return this.data.hashCode();
    }

    private String name;
    private String path;
    private int sequenceNr = 1;
    private String encoding;
    private byte[] data;

    public Image(String name) {
        this.name = name;
    }

    private String getVersionedName() {
        int lastDotInName = this.name.lastIndexOf('.');
        if (this.sequenceNr < 2 || lastDotInName < 0) {
            return this.name;
        }
        return this.name.substring(0, lastDotInName) + "-v" + this.sequenceNr + this.name.substring(lastDotInName);
    }

    private Image findDuplicateImage(QuestionBank questionBank) {
        this.sequenceNr = 1;
        Image duplicateImage = questionBank.getImages().get(this.getFullName());
        while (duplicateImage != null && duplicateImage.getSize() != this.getSize()) {
            this.sequenceNr++;
            duplicateImage = questionBank.getImages().get(this.getVersionedFullName());
        }
        return duplicateImage;
    }

    public static Image parseMXML(XMLParser xmlParser, QuestionBank questionBank, String parentText) throws XMLStreamException {
        if (xmlParser.nextBeginTag("file")) {
            Image image = new Image(xmlParser.getAttributeValue(null, "name"));
            image.path = xmlParser.getAttributeValue(null, "path", "/");
            if (!image.path.endsWith("/")) image.path += "/";
            String encoding = xmlParser.getAttributeValue(null, "encoding", "");
            String encodedData = xmlParser.getElementText();
            if (encoding.equals("base64")) {
                image.data = Base64.getMimeDecoder().decode(encodedData);
            } else {
                image.data = encoding.getBytes();
            }
            xmlParser.findAndAcceptEndTag("file");

            Image duplicateImage = image.findDuplicateImage(questionBank);

            if (parentText != null && !parentText.contains("@@PLUGINFILE@@" + image.getFullURL())) {
                SLF4J.LOGGER.debug("Ignoring unused image '{}' with size = {}, hash = {}",
                        image.getFullName(), image.getSize(), image.getDataHash());
                image.sequenceNr = 0;
                return image;
            } else if (duplicateImage != null) {
                return duplicateImage;
            } else if (image.sequenceNr > 1) {
                SLF4J.LOGGER.info("Duplicate image loaded '{}' with size = {}, data hash = {}",
                        image.getVersionedFullName(), image.getSize(), image.getDataHash() );
            }

            questionBank.getImages().put(image.getVersionedFullName(), image);

            return image;
        }
        return null;
    }

    public void exportToFile(String exportPath) {

        try  {
            String imagePath = exportPath + this.getVersionedFullName();
            SLF4J.LOGGER.debug("exporting image '{}' with size = {}", imagePath, this.getSize() );
            Files.createDirectories(Path.of(exportPath + this.path));
            FileOutputStream fos = new FileOutputStream(imagePath);
            fos.write(this.data);
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
