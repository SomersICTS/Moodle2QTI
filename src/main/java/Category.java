import utils.SLF4J;
import utils.XMLWriter;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Category {

    private QuestionBank questionBank;
    private Category parent;
    private String name;
    private String info;
    private long id;
    private List<Category> subCategories;
    private Set<Question> questions;

    public Category(String name, Category parent, QuestionBank questionBank) {
        this.name = name;
        this.parent = parent;
        this.questionBank = questionBank;
        this.subCategories = new ArrayList<>();
        this.questions = new HashSet<>();

        if (parent != null) {
            parent.subCategories.add(this);
        }
        SLF4J.LOGGER.debug("Created category: '{}'", this.getFullName());
    }

    public void add(Question question) {
        this.questions.add(question);
    }

    public Category findOrCreate(String[] path) {
        Category parent = this;
        for (String n : path) {
            Category child = parent.subCategories.stream()
                    .filter(c -> c.name.equals(n))
                    .findFirst().orElse(null);
            if (child == null) {
                child = new Category(n, parent, parent.getQuestionBank());
            }
            parent = child;
        }
        return parent;
    }

    public String getName() {
        return name;
    }
    public String getFullName() {
        if (this.parent == null) {
            return this.name;
        } else {
            return this.parent.getFullName() + '/' + this.name;
        }
    }
    public String getInfo() {
        return info;
    }
    public void setInfo(String info) {
        this.info = info;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int showSummary(int indent) {
        int numQuestions = this.questions.size();
        System.out.printf("%"+(1+indent)+"s[%d] %s\n", " ", this.questions.size(), this.name);
        for (Category c : this.subCategories) {
            numQuestions += c.showSummary(indent + 3);
        }
        return numQuestions;
    }

    public int countWords(String word) {
        int numWords = 0;
        for (Question q : this.questions) {
            numWords += QuestionBank.countWords(word, q.getQuestionText().toLowerCase());
        }
        for (Category c : this.subCategories) {
            numWords += c.countWords(word);
        }
        return numWords;
    }

    public QuestionBank getQuestionBank() {
        return questionBank;
    }

    public Category findNewRoot() {
        if (this.subCategories.size() == 1 && this.questions.isEmpty()) {
            return this.subCategories.get(0).findNewRoot();
        } else {
            return this;
        }
    }

    public void exportQTI21(XMLWriter manifest, String exportFolder) throws IOException, XMLStreamException {
        String categoryFolder = exportFolder + "/" + this.getFullName();
        Files.createDirectories(Paths.get(categoryFolder));

        for (Question q : this.questions) {
            q.exportQTI21(manifest, exportFolder);
        }

        for (Category c : this.subCategories) {
            c.exportQTI21(manifest, exportFolder);
        }

    }

    public Category getParent() {
        return parent;
    }

    public void setParent(Category parent) {
        this.parent = parent;
    }

    public List<Category> getSubCategories() {
        return subCategories;
    }

    public void setSubCategories(List<Category> subCategories) {
        this.subCategories = subCategories;
    }

    public Set<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(Set<Question> questions) {
        this.questions = questions;
    }

    public String getMediaFilesFolder() {
        return QuestionBank.QTI_MEDIAFILES;
    }
}
