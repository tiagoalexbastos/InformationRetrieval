package T4.utils;

public class Posting2 {


    private final Integer docId;
    private final double termWeight;

    public Posting2(Integer docId, double termWeight) {
        this.docId = docId;
        this.termWeight = termWeight;
    }

    public Integer getDocId() {
        return docId;
    }

    public double getTermWeight() {
        return termWeight;
    }
}
