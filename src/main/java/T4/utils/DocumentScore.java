package T4.utils;

/**
 * Created by pmatos9 on 18/11/17.
 */
public class DocumentScore {

    private String term;
    private double wt_n;

    public DocumentScore(String term, double wt_n) {
        this.term = term;
        this.wt_n = wt_n;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public double getWt_n() {
        return wt_n;
    }

    public void setWt_n(double wt_n) {
        this.wt_n = wt_n;
    }

    @Override
    public String toString() {
        return "DocumentScore{" +
                "term='" + term + '\'' +
                ", wt_n=" + wt_n +
                '}';
    }
}
