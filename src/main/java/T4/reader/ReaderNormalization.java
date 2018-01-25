package T4.reader;

import T4.utils.Posting2;

import java.util.*;
import java.util.stream.Collectors;

public class ReaderNormalization {

    private final TreeMap<String, LinkedList<Posting2>> tokenDocIdFreq = new TreeMap<>();
    private LinkedList<Posting2> postings;


    public TreeMap<String, LinkedList<Posting2>> getTokenDocIdFreq() {
        return tokenDocIdFreq;
    }

    public void addToSetAndCount(List<String> processedTerms, Integer docId) {
        Set<String> unique = new HashSet<>(processedTerms);
        LinkedList<Posting2> tmpFT = new LinkedList<>();

        unique.stream().map((key) -> Collections.frequency(processedTerms, key)).map((freq) -> new Posting2(docId, freq)).forEach((tmpPosting) -> {
            tmpFT.push(tmpPosting);
        });

        
        LinkedList tmp = tmpFT.stream()
                .map(p-> new Posting2(p.getDocId(), 1 + Math.log10(p.getTermWeight())))
                .collect(Collectors.toCollection(LinkedList::new));

        int i=0;
        double norm = getNormalizationValueDoc(tmp);

        for(String key: unique){
            double tf = ((Posting2)tmp.get(i)).getTermWeight();
            addToTokenDocIdFreq(key, docId, normalizeTFDoc(tf, norm));
            i++;
        }
    }


    private double getNormalizationValueDoc(LinkedList<Posting2> tmp){
        double som=0;
        for(int i=0; i<tmp.size(); i++)
            som += Math.pow(tmp.get(i).getTermWeight(), 2);
        return Math.sqrt(som);
    }


    private double normalizeTFDoc(double tf, double som){
        return  tf/som;
    }


    public void addToTokenDocIdFreq(String token, int docId, double termWeight){
        Posting2 tmpPost = new Posting2(docId, termWeight);
        if(!tokenDocIdFreq.containsKey(token)){
            postings = new LinkedList<>();
            postings.add(tmpPost);
            tokenDocIdFreq.put(token, postings);
        }else{
            // caso key exista
            postings = tokenDocIdFreq.get(token);
            postings.add(tmpPost);
            tokenDocIdFreq.put(token, postings);
        }
    }

}
