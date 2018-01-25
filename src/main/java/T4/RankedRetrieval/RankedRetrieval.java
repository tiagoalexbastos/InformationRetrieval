package T4.RankedRetrieval;

import T4.tokenizer.StrongTokenizer;
import T4.utils.DocumentScore;
import T4.utils.TfIdfWeighting;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Created by pmatos9 on 13/11/17.
 */
public class RankedRetrieval {

    private StrongTokenizer tokenizer;
    private Map<Integer, Map<Double, Integer>> map_of_the_maps = new HashMap<>();
    private Map<Integer, ArrayList<Integer>> map_reduced = new HashMap<>();
    public RankedRetrieval(String stop){
        this.tokenizer = new StrongTokenizer(stop);
    }


    public List<String> ParseQuerys(File f){
        List<String> terms = new LinkedList<String>();

        try {
            terms = FileUtils.readLines(f,"utf-8");
        } catch (IOException ex) {
            throw new RuntimeException("There was a problem reading the document "
                    + f.getName(), ex);
        }
        return terms;
    }

    public long ProcessTerms(List<String> query, Map<String, List<TfIdfWeighting>> dic, File dir){
        long start = System.nanoTime();

        int query_id = 1;
        String query_term;

        for (String tQuery : query) {
            query_term = tQuery;
            List<String> tokens = tokenizer.tokenize(query_term);

            Map<String, Integer>  map_term_count = new HashMap<>();
            for(String tmp_token : tokens){
                if(map_term_count.containsKey(tmp_token)){
                    int value = map_term_count.get(tmp_token);
                    map_term_count.put(tmp_token,value+1);
                }
                else{
                    map_term_count.put(tmp_token,1);
                }

            }

            //create weights to the terms in the query
            Iterator it = map_term_count.entrySet().iterator();
            double normalization = 0;
            Map<String, Double> query_weights = new HashMap<>();
            while (it.hasNext()){
                Map.Entry pair = (Map.Entry) it.next();

                int count_term = (int) pair.getValue();
                String term = (String) pair.getKey();
                double term_freq_query = 1 +Math.log(count_term);
                query_weights.put(term,term_freq_query);

                normalization += Math.pow(term_freq_query,2);
            }

            // multiply the term weight * the full normalization value for the query
            double final_value_norm = 1 / Math.sqrt(normalization);
            Iterator it_norm = query_weights.entrySet().iterator();
            while(it_norm.hasNext()){
                Map.Entry pair = (Map.Entry) it_norm.next();

                double term_freq_query = (double) pair.getValue();
                double final_term_freq = term_freq_query * final_value_norm;
                query_weights.put((String) pair.getKey(),final_term_freq);
            }


            //Hash map with key that is doc_id and value is list of weights for the terms
            Map<Integer, List<DocumentScore>> document_weights = new HashMap<>();
            Iterator it_documents = dic.entrySet().iterator();
            while(it_documents.hasNext()){
                Map.Entry pair = (Map.Entry) it_documents.next();
                List<TfIdfWeighting> tmp_list = (List<TfIdfWeighting>) pair.getValue();
                for(int i = 0; i < tmp_list.size(); i++){
                    TfIdfWeighting tmp_w = tmp_list.get(i);
                    int tmp_docid = tmp_w.getDocId();
                    DocumentScore dc = new DocumentScore(tmp_w.getTerm(), tmp_w.getWeight_normalized());

                    if(document_weights.containsKey(tmp_docid)){
                        document_weights.get(tmp_docid).add(dc);
                    }
                    else{
                        List<DocumentScore> list_docscores = new ArrayList<DocumentScore>();
                        list_docscores.add(dc);
                        document_weights.put(tmp_docid,list_docscores);
                    }
                }
            }


            //iterator to multiply the query weights and the document weights
            Iterator it_final = document_weights.entrySet().iterator();
            while(it_final.hasNext()){
                Map.Entry pair = (Map.Entry) it_final.next();

                //get the document
                int doc_id = (Integer) pair.getKey();

                //ir a todos os termos do documento e ver se ele existe na query
                List<DocumentScore> list_docscores = (List<DocumentScore>) pair.getValue();
                double score = 0.0;
                for(DocumentScore doc: list_docscores){
                    //ver se existe na query
                    if(query_weights.containsKey(doc.getTerm())){
                        //peso do termo da query
                        double w_term_query = query_weights.get(doc.getTerm());
                        score += doc.getWt_n() * w_term_query;
                    }
                }

                if(score != 0.0){
                    //<Query_id, <doc_score, doc_id>>
                    if(map_of_the_maps.containsKey(query_id)){
                        map_of_the_maps.get(query_id).put(score,doc_id);
                    }
                    else{
                        Map<Double, Integer> tmp_mapv = new TreeMap(Collections.reverseOrder());
                        tmp_mapv.put(score,doc_id);
                        map_of_the_maps.put(query_id,tmp_mapv);
                    }
                }
            }
            query_id++;
        }
        long elapsedTime = System.nanoTime() - start;
        WriteScores(dir);
        return elapsedTime;
    }

    public Map<Integer, ArrayList<Integer>> getMap10(){
        // Map<Integer, Map<Double, Integer>>
        Iterator final_it = map_of_the_maps.entrySet().iterator();
        while (final_it.hasNext()){
            int cont_final = 0;
            Map.Entry pair = (Map.Entry) final_it.next();
            int query_id = (int) pair.getKey();
            Map<Double, Integer> tmp_mapv = (Map<Double, Integer>) pair.getValue();
            Iterator tmp_it = tmp_mapv.entrySet().iterator();

            while(tmp_it.hasNext()){
                Map.Entry pair_query = (Map.Entry) tmp_it.next();
                double score = (double) pair_query.getKey();
                int doc_id = (int) pair_query.getValue();


                if(cont_final < 10){

                    if(map_reduced.containsKey(query_id)){
                        ArrayList<Integer> x = map_reduced.get(query_id);
                        x.add(doc_id);
                        map_reduced.replace(query_id,x);
                    }
                    else{
                        ArrayList<Integer> map = new ArrayList<>();
                        map.add(doc_id);
                        map_reduced.put(query_id,map);
                    }
                    cont_final++;
                }

            }
        }

        return map_reduced;
    }

    private void WriteScores(File dir){
        dir.mkdir();
        try {
            FileUtils.cleanDirectory(dir);
        } catch (IOException ex) {
            throw new RuntimeException("There was a problem cleaning the directory.", ex);
        }

        try {
            String blockFileName = "scores.txt";
            String blockFileName_limited = "scores_limited.txt";
            PrintWriter pwt = new PrintWriter(new File(dir, blockFileName));
            PrintWriter pwt_limited = new PrintWriter(new File(dir, blockFileName_limited));
            pwt.println("query_id" + "\t" + "doc_id" + "\t" + "doc_score");
            pwt_limited.println("query_id" + "\t" + "doc_id" + "\t" + "doc_score");

            Iterator final_it = map_of_the_maps.entrySet().iterator();
            while (final_it.hasNext()){
                int cont_final = 0;
                Map.Entry pair = (Map.Entry) final_it.next();
                int query_id = (int) pair.getKey();
                Map<Double, Integer> tmp_mapv = (Map<Double, Integer>) pair.getValue();
                Iterator tmp_it = tmp_mapv.entrySet().iterator();

                while(tmp_it.hasNext()){
                    Map.Entry pair_query = (Map.Entry) tmp_it.next();
                    double score = (double) pair_query.getKey();
                    int doc_id = (int) pair_query.getValue();
                    pwt.println(query_id + "\t\t\t" + doc_id + "\t\t\t" + score);

                    if(cont_final < 10){
                        pwt_limited.println(query_id + "\t\t\t" + doc_id + "\t\t\t" + score);
                        cont_final++;
                    }

                }
            }
            pwt_limited.close();
            pwt.close();
        } catch (IOException ex) {
            throw new RuntimeException("There was a problem writing the index to a file", ex);
        }
    }

    public Map<Integer, Map<Double, Integer>> getMap_of_the_maps() {
        return map_of_the_maps;
    }
}
