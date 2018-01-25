package T4.RelevanceFeedback;

import T4.Word2Vec.Word2Vec;
import T4.tokenizer.StrongTokenizer;
import T4.utils.Posting2;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class Relevances {


    private double alpha;
    private double betha;
    private double miu;
    private int corpus_size;
    private StrongTokenizer tokenizer;
    private TreeMap<Integer, TreeMap<Integer, Double>> mapImplicit;
    private TreeMap<Integer, TreeMap<Integer, Double>> mapExplicit;
    private TreeMap<String, LinkedList<Posting2>>dic_weight;
    private final TreeMap<Integer, TreeMap<Integer,Double>> mapWord2Vec;
    private final Word2Vec w2v = new Word2Vec();

    public Relevances(TreeMap<String, LinkedList<Posting2>>dic_weight, int corpus_size, String stop, double alpha, double betha, double miu) {

        this.alpha = alpha;
        this.betha = betha;
        this.miu = miu;

        this.corpus_size = corpus_size;
        this.tokenizer = new StrongTokenizer(stop);
        this.mapExplicit = new TreeMap<>();
        this.mapImplicit = new TreeMap<>();
        this.dic_weight = dic_weight;
        this.mapWord2Vec = new TreeMap<>();
    }


    public TreeMap<Integer, TreeMap<Integer, Double>> getMapWord2Vec() {
        return mapWord2Vec;
    }

    public TreeMap<Integer, TreeMap<Integer, Double>> getMapImplicit() {
        return mapImplicit;
    }

    public TreeMap<Integer, TreeMap<Integer, Double>> getMapExplicit() {
        return mapExplicit;
    }

    public void calculateImplicit(Map<Integer, ArrayList<Integer>> map_10_scores, int query_id, String query) {

        TreeMap<String, Double> results_implicit = new TreeMap<>();
        Map<String, Integer> term_count = new TreeMap<>();
        List<String> tokens = tokenizer.tokenize(query);

        ArrayList<Integer> relev_implicit = map_10_scores.get(query_id);

        Set<String> unique = new HashSet<>(tokens);
        unique.forEach((key) -> term_count.put(key, Collections.frequency(tokens, key)));

        double norm = getNormalizationValueQuery(term_count, corpus_size);

        for (String term : tokens) {

            if (dic_weight.containsKey(term)) {
                LinkedList<Posting2> posts = dic_weight.get(term);
                int termQueryFreq = Collections.frequency(tokens, term);

                for (Posting2 p : posts) {
                    if (relev_implicit.contains(p.getDocId())) {
                        double wtqOriginal = ((getTFQuery(termQueryFreq) * calculateIdfQuery(corpus_size, getNrDocsByTerm(term))) / norm) * alpha;
                        double wtdPositive = betha * p.getTermWeight() / (relev_implicit.size());
                        double finalScore = (wtqOriginal + wtdPositive);
                        if (!results_implicit.containsKey(term))
                            results_implicit.put(term, finalScore);
                        else
                            results_implicit.replace(term, results_implicit.get(term) + wtdPositive);

                    }
                }
            }
        }
        calcu_weights_implicit(results_implicit, query_id);
    }

    public void calculateExplicit(Map<Integer, ArrayList<Integer>> nonRelevantDocsHM, Map<Integer, Map<Integer, Integer>> relevante,
                                  int query_id, String query) {


        TreeMap<String, Double> results_explicit = new TreeMap<>();
        Map<String, Integer> term_count = new TreeMap<>();
        List<String> tokens = tokenizer.tokenize(query);

        ArrayList<Integer> non_rel = nonRelevantDocsHM.get(query_id);
        TreeMap<Integer, Integer> relev_explicit = (TreeMap<Integer, Integer>) relevante.get(query_id);

        Set<String> unique = new HashSet<>(tokens);
        unique.forEach((key) -> term_count.put(key, Collections.frequency(tokens, key)));

        double norm = getNormalizationValueQuery(term_count, corpus_size);


        for (String term : tokens) {

            if (dic_weight.containsKey(term)) {
                List<Posting2> posts = dic_weight.get(term);
                int termQueryFreq = Collections.frequency(tokens, term);

                for (Posting2 p : posts) {
                    if (relev_explicit.containsKey(p.getDocId())) {
                        int relevance = relev_explicit.get(p.getDocId());
                        double wtqOriginal = ((getTFQuery(termQueryFreq) * calculateIdfQuery(corpus_size, getNrDocsByTerm(term))) / norm) * alpha;
                        double wtdPositive = betha * relevance * p.getTermWeight() / getSumOfRelevant(relev_explicit);
                        double finalScore = (wtqOriginal + wtdPositive);
                        if (!results_explicit.containsKey(term))
                            results_explicit.put(term, finalScore);
                        else
                            results_explicit.replace(term, results_explicit.get(term) + wtdPositive);
                    } else {
                        double wtqOriginal = ((getTFQuery(termQueryFreq) * calculateIdfQuery(corpus_size, getNrDocsByTerm(term))) / norm) * alpha;
                        double wtdNegative = miu * p.getTermWeight() / (non_rel.size());
                        double finalScore = (wtqOriginal - wtdNegative);
                        if (!results_explicit.containsKey(term))
                            results_explicit.put(term, finalScore);
                        else
                            results_explicit.replace(term, results_explicit.get(term) - wtdNegative);
                    }
                }
            }
        }
        calcu_weights_explicit(results_explicit, query_id);
    }

    public double getSumOfRelevant(TreeMap<Integer, Integer> explicitRelevantDocs) {
        int sum = 0;
        for (Map.Entry<Integer, Integer> entry : explicitRelevantDocs.entrySet()) {
            sum += entry.getValue();
        }
        return sum / (explicitRelevantDocs.size());
    }

    public void calcu_weights_implicit(TreeMap<String, Double> tmpTM, int queryId) {

        for (Map.Entry<String, Double> entry : tmpTM.entrySet()) {
            if (dic_weight.containsKey(entry.getKey())) {
                List<Posting2> tmpPostings = dic_weight.get(entry.getKey());

                tmpPostings.forEach((entryPosting) -> {
                    double wtd = entryPosting.getTermWeight();
                    double wtq = entry.getValue();
                    double score = wtd * wtq;
                    if (score != 0) {
                        addToQueryIdDocIdScoreTM(entryPosting.getDocId(), queryId, score, true);
                    }
                });
            }
        }
    }

    public void calcu_weights_explicit(TreeMap<String, Double> tmpTM, int queryId) {

        for (Map.Entry<String, Double> entry : tmpTM.entrySet()) {
            if (dic_weight.containsKey(entry.getKey())) {
                List<Posting2> tmpPostings = dic_weight.get(entry.getKey());

                tmpPostings.forEach((entryPosting) -> {
                    double wtd = entryPosting.getTermWeight();
                    double wtq = entry.getValue();
                    double score = wtd * wtq;
                    if (score != 0) {
                        addToQueryIdDocIdScoreTM(entryPosting.getDocId(), queryId, score, false);
                    }
                });
            }
        }
    }

    public void addToQueryIdDocIdScoreTM(int docId, int queryId, double score, boolean implicit) {

        if (implicit) {
            TreeMap<Integer, Double> docsScores;
            if (!mapImplicit.containsKey(queryId)) {
                docsScores = new TreeMap<>();
                docsScores.put(docId, score);

            } else {
                docsScores = mapImplicit.get(queryId);
                if (docsScores.containsKey(docId)) {
                    double res = docsScores.get(docId);
                    res += score;
                    docsScores.replace(docId, res);
                } else
                    docsScores.put(docId, score);
            }
            mapImplicit.put(queryId, docsScores);
        } else {
            TreeMap<Integer, Double> docsScores;
            if (!mapExplicit.containsKey(queryId)) {
                docsScores = new TreeMap<>();
                docsScores.put(docId, score);

            } else {
                docsScores = mapExplicit.get(queryId);
                if (docsScores.containsKey(docId)) {
                    double res = docsScores.get(docId);
                    res += score;
                    docsScores.replace(docId, res);
                } else
                    docsScores.put(docId, score);
            }
            mapExplicit.put(queryId, docsScores);

        }

    }

    private double getNormalizationValueQuery(Map<String, Integer> tmpMap, Integer corpusSize) {
        double norm = 0;
        norm = tmpMap.entrySet().stream()
                .map((entry) ->
                        getTFQuery(entry.getValue()) * calculateIdfQuery(corpusSize, getNrDocsByTerm(entry.getKey())))
                .map((tmp) ->
                        Math.pow(tmp, 2))
                .reduce(norm, (accumulator, _item) -> accumulator + _item); // tmp += tf * idf;
        return Math.sqrt(norm);
    }


    private double getTFQuery(int freq) {
        return 1 + Math.log10(freq);
    }


    private double calculateIdfQuery(int corpusSize, int nrDocsByTerm) {
        if (nrDocsByTerm == 0)
            return 0;
        return Math.log10(corpusSize / nrDocsByTerm);
    }


    private int getNrDocsByTerm(String term) {
        if (dic_weight.containsKey(term)) {
            return dic_weight.get(term).size();
        } else {
            return 0;
        }
    }


    public void calculateScoreWord2Vec(String query, int queryId, int numWords) {

        Map<String, Integer> tmpMap = new TreeMap<>();
        List newProcessedList = new LinkedList<>();

        List<String> processedList = tokenizer.tokenize(query);

        newProcessedList.addAll(processedList);

        for(int i=0; i<processedList.size(); i++){
            String term = processedList.get(i);
            Collection<String> newWords=null;
            newWords = w2v.generateNearestWords(term, numWords);

            if(newWords!=null){
                for(String newWord: newWords)
                    newProcessedList.add(newWord);
            }
        }

        Set<String> unique = new HashSet<>(newProcessedList);
        unique.forEach((key) -> {
            Integer freq = Collections.frequency(newProcessedList, key);
            tmpMap.put(key, freq);
        });

        double norm = getNormalizationValueQuery(tmpMap, corpus_size);

        for(int query_term=0; query_term<newProcessedList.size(); query_term++){
            String key = (String) newProcessedList.get(query_term);

            if(dic_weight.containsKey(key)){
                LinkedList<Posting2> tmpPostings = dic_weight.get(key);
                int termQueryFreq = Collections.frequency(newProcessedList, key);

                tmpPostings.forEach((entryPosting) -> {
                    double wtd = entryPosting.getTermWeight();
                    double wtq = (getTFQuery(termQueryFreq) * calculateIdfQuery(corpus_size, getNrDocsByTerm(key)))/norm;
                    double score = wtd*wtq;
                    if (score != 0) {
                        addToQueryIdDocIdScoreWord2Vec(entryPosting.getDocId(), queryId, score, mapWord2Vec);
                    }
                });
            }
        }
    }

    public void addToQueryIdDocIdScoreWord2Vec(int docId, int queryId, double score, TreeMap<Integer, TreeMap<Integer,Double>> tmap){

        TreeMap<Integer, Double> docsScores;
        if(!tmap.containsKey(queryId)){
            docsScores = new TreeMap<>();
            docsScores.put(docId,score);

        } else {
            docsScores = tmap.get(queryId);
            if(docsScores.containsKey(docId))
                docsScores.replace(docId,docsScores.get(docId)+ score);
            else
                docsScores.put(docId,score);
        }
        tmap.put(queryId, docsScores);


    }





    public void writeScores(File dir, boolean implicit) {
        dir.mkdir();

        try {

            if (implicit) {

                String blockFileName = "implict.txt";
                PrintWriter pwt = new PrintWriter(new File(dir, blockFileName));
                pwt.println("query_id" + "\t" + "doc_id" + "\t" + "doc_score");
                Iterator it = mapImplicit.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry pair = (Map.Entry) it.next();

                    int query_id = (int) pair.getKey();

                    TreeMap<Integer, Double> map = (TreeMap<Integer, Double>) pair.getValue();
                    map.entrySet().stream()
                            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                            .forEach(integerDoubleEntry ->
                                    pwt.println(query_id + "\t\t\t" + integerDoubleEntry.getKey() + "\t\t\t" + integerDoubleEntry.getValue()));

                }
                pwt.close();
            } else {
                String blockFileName = "explicit.txt";
                PrintWriter pwt = new PrintWriter(new File(dir, blockFileName));
                pwt.println("query_id" + "\t" + "doc_id" + "\t" + "doc_score");
                Iterator it = mapExplicit.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry pair = (Map.Entry) it.next();

                    int query_id = (int) pair.getKey();

                    TreeMap<Integer, Double> map = (TreeMap<Integer, Double>) pair.getValue();
                    map.entrySet().stream()
                            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                            .forEach(integerDoubleEntry ->
                                    pwt.println(query_id + "\t\t\t" + integerDoubleEntry.getKey() + "\t\t\t" + integerDoubleEntry.getValue()));

                }
                pwt.close();


            }

        } catch (IOException ex) {
            throw new RuntimeException("There was a problem writing the index to a file", ex);
        }
    }


    public void printWord2Vec(File dir) {
        dir.mkdir();

        try {

            String blockFileName = "word2vec.txt";
            PrintWriter pwt = new PrintWriter(new File(dir, blockFileName));
            pwt.println("query_id" + "\t" + "doc_id" + "\t" + "doc_score");
            Iterator it = mapWord2Vec.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();

                int query_id = (int) pair.getKey();

                TreeMap<Integer, Double> map = (TreeMap<Integer, Double>) pair.getValue();
                map.entrySet().stream()
                        .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                        .forEach(integerDoubleEntry ->
                                pwt.println(query_id + "\t\t\t" + integerDoubleEntry.getKey() + "\t\t\t" + integerDoubleEntry.getValue()));
            }
            pwt.close();
        } catch (IOException ex) {
            throw new RuntimeException("There was a problem writing the index to a file", ex);
        }
    }
}
