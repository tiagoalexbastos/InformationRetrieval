package T4.Evaluation;

import java.util.*;

public class Evaluation {

    private Map <Integer, Map<Integer, Integer>> map = null;


    public void setMap(Map<Integer, Map<Integer, Integer>> map_relv) {
        this.map = map_relv;
    }


    public ArrayList<Double> calculateDCG(Map<Integer, Map<Integer, Integer>> relevantDocsHM) {

        ArrayList<Double> queryIdDCGs = new ArrayList<>();
        for(Map.Entry<Integer, Map<Integer, Integer>> entry : relevantDocsHM.entrySet()) {
            double dcg=0;
            Map<Integer, Integer> docIdRelevance = entry.getValue();
            int i = 0;
            for(Map.Entry<Integer, Integer> docs : docIdRelevance.entrySet()){
                i++;
                if(i!=1)
                    dcg += docs.getValue()/(Math.log(i)/Math.log(2));
                else
                    dcg=docs.getValue();
            }
            queryIdDCGs.add(dcg);
        }
        return queryIdDCGs;
    }

    public ArrayList<Double> calculateIDCG(Map<Integer, Map<Integer, Integer>> relevantDocsHM) {

        ArrayList<Double> queryIdIDCGs = new ArrayList<>();
        for(Map.Entry<Integer, Map<Integer, Integer>> entry : relevantDocsHM.entrySet()) {
            double idcg=0;
            Map<Integer, Integer> docIdRelevance = entry.getValue(); // docId relevance

            ArrayList<Integer> orderedRelevances = new ArrayList<>();
            for(Map.Entry<Integer, Integer> docs : docIdRelevance.entrySet())
                orderedRelevances.add(docs.getValue());

            Collections.sort(orderedRelevances, Collections.reverseOrder());
            for(int i=1; i<= orderedRelevances.size(); i++){
                if(i!=1)
                    idcg += orderedRelevances.get(i-1)/ (Math.log(i)/Math.log(2));
                else
                    idcg=orderedRelevances.get(i-1);
            }
            queryIdIDCGs.add(idcg);
        }
        return queryIdIDCGs;
    }



    public Map<String, Double> countScores(TreeMap<Integer, Double> results, int queryId, int corpus_size, int rankSize){
        int truePos = 0, falsePos = 0, falseNeg = 0, trueNeg = 0, truePosRank=0, falsePosRank=0;


        Map<Integer, Integer> relevants_docs = map.get(queryId);

        ArrayList<Integer> docsId = new ArrayList<>();
        int countdocs = 0;
        double avgPrec = 0.0, mmrFirst = 0.0;
        boolean first = true;

        Set set = results.entrySet();
        Iterator iterator = set.iterator();
        while(iterator.hasNext()) {
            Map.Entry me = (Map.Entry)iterator.next();
            docsId.add((Integer) me.getKey());
        }

        for(int docId: docsId){

            countdocs++;
            if(relevants_docs.containsKey(docId)){
                if(first){
                    mmrFirst = (1.0/countdocs);
                    first = false;
                }
                truePos++;
                avgPrec += (truePos * 1.0 /countdocs * 1.0);
            }
            else
                falsePos++;
            if(countdocs <= rankSize){
                truePosRank = truePos;
                falsePosRank = falsePos;
            }
        }

        falseNeg = relevants_docs.keySet().stream().filter((relevant) -> (!docsId.contains(relevant))).map((_item) -> 1).reduce(falseNeg, Integer::sum);


        trueNeg = corpus_size - truePos - falsePos - falseNeg;

        Map<String, Double> valuescomputed = new HashMap<>();
        valuescomputed.put("truePos", (double)truePos);
        valuescomputed.put("trueNeg", (double)trueNeg);
        valuescomputed.put("falsePos", (double)falsePos);
        valuescomputed.put("falseNeg", (double)falseNeg);
        valuescomputed.put("avgPrecision", truePos != 0 ? avgPrec/truePos : 0);
        valuescomputed.put("truePosRank", (double)truePosRank);
        valuescomputed.put("falsePosRank", (double)falsePosRank);
        valuescomputed.put("recRank", mmrFirst);

        return valuescomputed;
    }


    public double precision(Map<String, Double> valuescomputed, Boolean ranked) {
        double truePos, falsePos;
        if(!ranked){
            truePos = valuescomputed.get("truePos");
            falsePos = valuescomputed.get("falsePos");
        }else{
            truePos = valuescomputed.get("truePosRank");
            falsePos = valuescomputed.get("falsePosRank");
        }

        if(truePos == 0)
            return 0.0;
        return truePos /(truePos+falsePos);
    }


    public double recall(Map<String, Double> valuescomputed){
        double truePos = valuescomputed.get("truePos");
        double falseNeg = valuescomputed.get("falseNeg");

        if(truePos == 0)
            return 0.0;
        return truePos /(truePos+falseNeg);
    }


    public double fmeasure(double recall, double precision){
        if(recall == 0.0 && precision == 0.0)
            return 0;
        else
            return 2.0*recall*precision/(recall+precision);
    }



}
