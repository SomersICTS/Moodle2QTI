import utils.SLF4J;
import utils.XMLWriter;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Category {

    private QuestionBank questionBank;
    private Category parent;
    private String name;
    private String info;
    private long id;

    public double getDefaultMark() {
        return defaultMark;
    }

    private double defaultMark = 0.0;
    private List<Category> subCategories;
    private List<Question> questions;

    public Category(String name, Category parent, QuestionBank questionBank) {
        this.name = name;
        int dgIdx = name.indexOf("[mark=");
        if (dgIdx >= 0) {
            this.defaultMark = Double.valueOf(name.substring(dgIdx+6,dgIdx+7));
            this.name = name.substring(0, dgIdx) + name.substring(dgIdx+8);
        }
        this.parent = parent;
        this.questionBank = questionBank;
        this.subCategories = new ArrayList<>();
        this.questions = new ArrayList<>();

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

    public Map<String,int[]> mapQuestionScores(Map<String,int[]> scoreCounts) {
        if (scoreCounts == null) scoreCounts = new HashMap<>();
        for (Question q : this.questions) {
            q.registerScore(scoreCounts);
        }
        for (Category c : this.subCategories) {
            scoreCounts = c.mapQuestionScores(scoreCounts);
        }
        return scoreCounts;
    }

    public List<String> findDeviatingScores(Map<String,Integer> defaultScores, List<String> deviations) {
        if (deviations == null) deviations = new ArrayList<>();
        for (Question q : this.questions) {
            q.registerDeviatingScore(defaultScores, deviations);
        }
        for (Category c : this.subCategories) {
            deviations = c.findDeviatingScores(defaultScores, deviations);
        }
        return deviations;
    }

    public static Map<String,Integer> findDefaultScores(Map<String,int[]> scoreCounts) {
        Map<String,Integer> defaultScores = new HashMap<>();
        for (Map.Entry<String,int[]> e : scoreCounts.entrySet()) {
            String type = e.getKey();
            int[] scores =  e.getValue();
            int maxCountIndex = 0;
            for (int i = 1; i < scores.length; i++)
                if (scores[i] > scores[maxCountIndex]) maxCountIndex = i;

            defaultScores.put(type, maxCountIndex);
        }
        return defaultScores;
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

    public void sortContent() {
        this.subCategories.sort( (c1,c2) -> c1.getName().compareTo(c2.getName()) );
        this.questions.sort( (q1,q2) -> q1.getFlatName().compareTo(q2.getFlatName()) );
        for (Category category: this.subCategories) {
            category.sortContent();
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

    public List<Question> getQuestions() {
        return questions;
    }

    public String getMediaFilesFolder() {
        return QuestionBank.QTI_MEDIAFILES;
    }
}
