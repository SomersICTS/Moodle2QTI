import utils.SLF4J;
import utils.XMLParser;

import javax.print.DocFlavor;
import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;

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
        while (duplicateImage != null &&
                // duplicateImage.getSize() != this.getSize()
                this.getSize() != 0 && duplicateImage.getDataHash() != this.getDataHash()
        ) {
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

            return image.reconcileOrStoreImage(questionBank, parentText);

        }
        return null;
    }

    private Image reconcileOrStoreImage(QuestionBank questionBank, String parentText) {
        Image duplicateImage = this.findDuplicateImage(questionBank);

        if (parentText != null && !parentText.contains("@@PLUGINFILE@@" + this.getFullURL())) {
            SLF4J.LOGGER.debug("Ignoring unused image '{}' with size = {}, hash = {}",
                    this.getFullName(), this.getSize(), this.getDataHash());
            this.sequenceNr = 0;
            return this;
        } else if (duplicateImage != null) {
            return duplicateImage;
        } else if (this.sequenceNr > 1) {
            SLF4J.LOGGER.info("Duplicate image '{}' loaded with size = {}, data hash = {}",
                    this.getVersionedFullName(), this.getSize(), this.getDataHash() );
        } else {
            SLF4J.LOGGER.debug("New image '{}' loaded with size = {}, data hash = {}",
                    this.getVersionedFullName(), this.getSize(), this.getDataHash() );
        }

        questionBank.getImages().put(this.getVersionedFullName(), this);

        return this;
    }

    public static Image retrieveFromHTTP(String url, QuestionBank questionBank) {
        String lcUrl = url.toLowerCase();
        int filePhpIdx = lcUrl.indexOf("file.php/");
        int fileIdx = lcUrl.lastIndexOf("/");
        Image image = new Image(url.substring(fileIdx+1));
        image.path = filePhpIdx > 0 ? url.substring(filePhpIdx+8,fileIdx+1) : "/";
        while (image.path.replace("/","").length() < image.path.length()-4) {
            image.path = image.path.substring(image.path.indexOf('/',1));
        }

        //return image.downloadFromHttp(url, questionBank);
        return image.downloadFromHttpConnection(url, questionBank);
    }

    private Image downloadFromHttp(String url, QuestionBank questionBank) {
        try (InputStream response = new URL(url).openStream()) {
            byte[] dataBuffer = new byte[4096];
            this.data = new byte[0];
            int bytesRead;
            while ((bytesRead = response.read(dataBuffer, 0, 4096)) != -1) {
                byte[] newData = Arrays.copyOf(this.data, this.data.length + bytesRead);
                System.arraycopy(dataBuffer, 0, newData, this.data.length, bytesRead);
                this.data = newData;
            }

            return this.reconcileOrStoreImage(questionBank, null);
        } catch (IOException ex) {
            // handle exception
            //SLF4J.logException(String.format("IO error while downloading image from '%s'", url), ex);
        }
        return null;
    }

    private static class MyAuthenticator extends Authenticator {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(
                    "username",
                    "password".toCharArray());
        }
    }
    private Image downloadFromHttpConnection(String url, QuestionBank questionBank) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .GET()
                    .build();

            HttpResponse<byte[]> response =
                    HttpClient.newBuilder()
                            .authenticator(new MyAuthenticator())
                            //.followRedirects(HttpClient.Redirect.ALWAYS)
                            .build()
                            .send(request, HttpResponse.BodyHandlers.ofByteArray());

            this.data = response.body();
            return this.reconcileOrStoreImage(questionBank, null);

        } catch (Exception ex) {
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
