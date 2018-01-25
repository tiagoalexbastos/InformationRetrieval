package T4.utils;


/**
 * Create by Pedro Matos & Tiago Bastos
 *
 * Class that represents the object Document. ID and Corpus the important information from the files
 */
public class Document {

    private final int id;

    private final String corpus;

    public Document(int id, String corpus){
        this.id = id;
        this.corpus = corpus;
    }

    public int getId() {
        return id;
    }

    public String getCorpus() {
        return corpus;
    }
}
