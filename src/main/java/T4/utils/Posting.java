package T4.utils;

/**
 * Create by Pedro Matos & Tiago Bastos
 * Class that represents the Posting, which is important to the terms in the inverted index.
 * DocID and the frequency are the important aspects.
 */
public class Posting {

    private final int docId;

    private final int frequency;

    public Posting(int docId, int frequency){
        this.docId = docId;
        this.frequency = frequency;
    }

    public int getDocId() {
        return docId;
    }

    public int getFrequency() {
        return frequency;
    }

    @Override
    public String toString() {
        return docId + ":" + frequency;
    }
}
