package T4.cli;

import T4.Evaluation.Evaluation;
import T4.RankedRetrieval.RankedRetrieval;
import T4.RelevanceFeedback.Relevances;
import T4.index.InvertedIndex;
import T4.index.TfIdfIndexer;
import T4.reader.ReaderNormalization;
import T4.tokenizer.StrongTokenizer;
import T4.tokenizer.Tokenizer;
import T4.utils.*;
import T4.reader.Reader;
import java.io.*;
import java.util.*;


/**
 * Created by Pedro Matos & Tiago Bastos
 *
 * This is the main interface of our application. It works like a command line interface to the user.
 */
public class ClInterface {
    public static void main(String[] args) throws IOException {

        String file_relv = "cranfield.query.relevance.txt";
        Map<Integer, Map<Integer, Integer>> map_relv = readRelevances(file_relv);
        //Map<String, List<T4.utils.Posting>> dic_postings = readPostings(file_post);



        String input_p = "cranfield/";
        String stopwords = "stopwords.txt";
        String out_dir = "output/";
        String queries = "cranfield.queries.txt";

        File folder = new File(input_p);
        File test = new File(input_p);
        int docs_number = 0;
        if (!test.isDirectory() || !test.canRead()) {
            System.out.println("The specified corpus directory is not a directory or is not readable.");
            System.exit(0);
        }
        else{
            docs_number = test.listFiles().length;
        }

        File output = new File(out_dir);
        File f = new File(queries);

        Tokenizer tokenizer = new StrongTokenizer(stopwords);
        Reader reader = new Reader();
        InvertedIndex invIndexer = new InvertedIndex(output);
        TfIdfIndexer tfidfIndexer = new TfIdfIndexer(output,docs_number);

        File[] listOfFiles = folder.listFiles();
        String[] tags = {"<TEXT>", "<AUTHOR>"};

        Map<String, List<Posting>> dic_postings;

        ReaderNormalization teste = new ReaderNormalization();
        int id = 1;
        for (File listOfFile : listOfFiles) {
            if (listOfFile.isFile()){
                /*
                 * read the file and get the corpus and id
                 */
                Document corpus = reader.readFile(listOfFile.getPath(), tags,id);
                List<String> tokens = tokenizer.tokenize(corpus.getCorpus());
                teste.addToSetAndCount(tokens,id);
                /*
                 * Add tokens to inverted index
                 */
                invIndexer.addTokens(corpus.getId(), tokens);
                id++;
            }
        }
        dic_postings = invIndexer.getDic();
        Set<String> terms = invIndexer.getTerms();
        tfidfIndexer.setDic(dic_postings);
        tfidfIndexer.setTerms(terms);
        tfidfIndexer.addDocumentFrequency();

        Map<String, List<TfIdfWeighting>> dic_weight = tfidfIndexer.getDic_weight();
        RankedRetrieval ranked_ret = new RankedRetrieval(stopwords);
        List<String> querys = ranked_ret.ParseQuerys(f);
        ranked_ret.ProcessTerms(querys, dic_weight, output);
        Map<Integer, ArrayList<Integer>> map_10_scores = ranked_ret.getMap10();


        // guião 4 calculations

        TreeMap<String, LinkedList<Posting2>> new_postings = teste.getTokenDocIdFreq();

        Relevances relev = new Relevances(new_postings,docs_number,stopwords,
                1.0,0.0,0.0);

        int query_id = 1;
        for(String query : querys){
            relev.calculateImplicit(map_10_scores,query_id,query);
            query_id++;
        }
        relev.writeScores(output,true);

        relevantAndNonRel(map_relv,map_10_scores);
        query_id = 1;
        for(String query : querys){
            relev.calculateExplicit(nonRelevantDocsHM,map_relv,query_id,query);
            query_id++;
        }
        relev.writeScores(output,false);

        //calculate NDGC
        Evaluation evaluation = new Evaluation();
        evaluation.setMap(map_relv);
        //evaluation.readQueryRelevanceFile(file_relv);
        double precision =0,recall =0, fmeasure=0, precisionRanked=0;
        double ndcg_explicit=0;
        double ndcg_implicit=0;
        double sumAvgPrec=0;
        double sumMMR=0;
        int rankSize = 10;
        Map<String, Double> mapScores;


        ArrayList<Double> dcgs_explict = new ArrayList<>();
        ArrayList<Double> idcgs__explict = new ArrayList<>();
        ArrayList<Double> dcgs_implicit = new ArrayList<>();
        ArrayList<Double> idcgs_implicit = new ArrayList<>();


        //for explicit
        dcgs_explict = evaluation.calculateDCG(map_relv);
        idcgs__explict = evaluation.calculateIDCG(map_relv);

        //for implicit
        dcgs_implicit = evaluation.calculateDCG(relevantDocsHM);
        idcgs_implicit = evaluation.calculateIDCG(relevantDocsHM);

        TreeMap<Integer, TreeMap<Integer, Double>> map_explicit = relev.getMapExplicit();
        TreeMap<Integer, TreeMap<Integer, Double>> map_implicit = relev.getMapImplicit();

        //for implicit
        int i=0;
        for(Map.Entry<Integer, TreeMap<Integer, Double>> entry : map_implicit.entrySet()){
            Integer corpusSize = docs_number;
            mapScores = evaluation.countScores(entry.getValue(), entry.getKey(), corpusSize, rankSize);
            precision += evaluation.precision(mapScores, false);
            recall += evaluation.recall(mapScores);
            fmeasure += evaluation.fmeasure(recall, precision);
            precisionRanked += evaluation.precision(mapScores, true);
            sumAvgPrec += mapScores.get("avgPrecision");
            sumMMR += mapScores.get("recRank");
            if(idcgs_implicit.get(i) != 0.0)
                ndcg_implicit += dcgs_implicit.get(i)/idcgs_implicit.get(i);
            i++;
        }

        System.out.println("Implicit: ");
        System.out.printf("Mean Precision: %.6f \n",precision/map_implicit.entrySet().size());
        System.out.printf("Mean Recall: %.6f \n",recall/map_implicit.entrySet().size());
        System.out.printf("Mean F-measure: %.6f \n",fmeasure/map_implicit.entrySet().size());
        System.out.printf("Mean average Precision: %.6f \n",sumAvgPrec/map_implicit.entrySet().size());
        System.out.printf("Mean Precision at Rank %d: %.6f\n", rankSize, precisionRanked/rankSize);
        System.out.printf("Mean Reciprocal Rank: %.6f\n",sumMMR/map_implicit.entrySet().size());
        System.out.printf("NDCG at Rank 10: %f\n",ndcg_implicit/map_implicit.entrySet().size());
        System.out.println("----------------------------------------------------");


        precision =0;
        recall =0;
        fmeasure=0;
        precisionRanked=0;
        ndcg_explicit=0;
        ndcg_implicit=0;
        sumAvgPrec=0;
        sumMMR=0;
        rankSize = 10;

        //for explicit
        int j=0;
        for(Map.Entry<Integer, TreeMap<Integer, Double>> entry : map_explicit.entrySet()){
            Integer corpusSize = docs_number;
            mapScores = evaluation.countScores(entry.getValue(), entry.getKey(), corpusSize, rankSize);
            precision += evaluation.precision(mapScores, false);
            recall += evaluation.recall(mapScores);
            fmeasure += evaluation.fmeasure(recall, precision);
            precisionRanked += evaluation.precision(mapScores, true);
            sumAvgPrec += mapScores.get("avgPrecision");
            sumMMR += mapScores.get("recRank");
            if(idcgs__explict.get(j) != 0.0)
                ndcg_explicit += dcgs_explict.get(j)/idcgs__explict.get(j);
            j++;
        }
        System.out.println("Explicit:");
        System.out.printf("Mean Precision: %.6f \n",precision/map_explicit.entrySet().size());
        System.out.printf("Mean Recall: %.6f \n",recall/map_explicit.entrySet().size());
        System.out.printf("Mean F-measure: %.6f \n",fmeasure/map_explicit.entrySet().size());
        System.out.printf("Mean average Precision: %.6f \n",sumAvgPrec/map_explicit.entrySet().size());
        System.out.printf("Mean Precision at Rank %d: %.6f\n", rankSize, precisionRanked/rankSize);
        System.out.printf("Mean Reciprocal Rank: %.6f\n",sumMMR/map_explicit.entrySet().size());
        System.out.printf("NDCG at Rank 10: %f\n",ndcg_explicit/map_explicit.entrySet().size());
        System.out.println("----------------------------------------------------");


        //alinea 2
        int id_wv = 1;
        for(String query : querys){
            relev.calculateScoreWord2Vec(query,id_wv,4);
            id_wv++;
        }

        //imprimir com word2vec
        relev.printWord2Vec(output);

        TreeMap<Integer, TreeMap<Integer, Double>> map_words2vec = relev.getMapWord2Vec();
        precision =0;
        recall =0;
        fmeasure=0;
        precisionRanked=0;
        ndcg_explicit=0;
        ndcg_implicit=0;
        sumAvgPrec=0;
        sumMMR=0;
        rankSize = 10;
        //for implicit
        i=0;
        for(Map.Entry<Integer, TreeMap<Integer, Double>> entry : map_words2vec.entrySet()){
            Integer corpusSize = docs_number;
            mapScores = evaluation.countScores(entry.getValue(), entry.getKey(), corpusSize, rankSize);
            precision += evaluation.precision(mapScores, false);
            recall += evaluation.recall(mapScores);
            fmeasure += evaluation.fmeasure(recall, precision);
            precisionRanked += evaluation.precision(mapScores, true);
            sumAvgPrec += mapScores.get("avgPrecision");
            sumMMR += mapScores.get("recRank");
            if(idcgs_implicit.get(i) != 0.0)
                ndcg_implicit += dcgs_implicit.get(i)/idcgs_implicit.get(i);
            i++;
        }
        System.out.println("Implicit with word2vec:");
        System.out.printf("Mean Precision: %.6f \n",precision/map_words2vec.entrySet().size());
        System.out.printf("Mean Recall: %.6f \n",recall/map_words2vec.entrySet().size());
        System.out.printf("Mean F-measure: %.6f \n",fmeasure/map_words2vec.entrySet().size());
        System.out.printf("Mean average Precision: %.6f \n",sumAvgPrec/map_words2vec.entrySet().size());
        System.out.printf("Mean Precision at Rank %d: %.6f\n", rankSize, precisionRanked/rankSize);
        System.out.printf("Mean Reciprocal Rank: %.6f\n",sumMMR/map_words2vec.entrySet().size());
        System.out.printf("NDCG at Rank 10: %f\n",ndcg_implicit/map_words2vec.entrySet().size());
        System.out.println("----------------------------------------------------");


        //for explicit
        precision =0;
        recall =0;
        fmeasure=0;
        precisionRanked=0;
        ndcg_explicit=0;
        ndcg_implicit=0;
        sumAvgPrec=0;
        sumMMR=0;
        rankSize = 10;
        j=0;
        for(Map.Entry<Integer, TreeMap<Integer, Double>> entry : map_words2vec.entrySet()){
            Integer corpusSize = docs_number;
            mapScores = evaluation.countScores(entry.getValue(), entry.getKey(), corpusSize, rankSize);
            precision += evaluation.precision(mapScores, false);
            recall += evaluation.recall(mapScores);
            fmeasure += evaluation.fmeasure(recall, precision);
            precisionRanked += evaluation.precision(mapScores, true);
            sumAvgPrec += mapScores.get("avgPrecision");
            sumMMR += mapScores.get("recRank");
            if(idcgs__explict.get(j) != 0.0)
                ndcg_explicit += dcgs_explict.get(j)/idcgs__explict.get(j);
            j++;
        }

        System.out.println("Explicit with word2vec:");
        System.out.printf("Mean Precision: %.6f \n",precision/map_words2vec.entrySet().size());
        System.out.printf("Mean Recall: %.6f \n",recall/map_words2vec.entrySet().size());
        System.out.printf("Mean F-measure: %.6f \n",fmeasure/map_words2vec.entrySet().size());
        System.out.printf("Mean average Precision: %.6f \n",sumAvgPrec/map_words2vec.entrySet().size());
        System.out.printf("Mean Precision at Rank %d: %.6f\n", rankSize, precisionRanked/rankSize);
        System.out.printf("Mean Reciprocal Rank: %.6f\n",sumMMR/map_words2vec.entrySet().size());
        System.out.printf("NDCG at Rank 10: %f\n",ndcg_explicit/map_words2vec.entrySet().size());
        System.out.println("----------------------------------------------------");



    }

    private static TreeMap<Integer, Map<Integer, Integer>> relevantDocsHM = new TreeMap<>();
    private static TreeMap<Integer, ArrayList<Integer>> nonRelevantDocsHM = new TreeMap<>();

    public static Map<Integer, Map<Integer, Integer>> readRelevances(String filename) throws IOException {

        FileReader freader = new FileReader(filename);
        BufferedReader br = new BufferedReader(freader);
        String s;
        Map<Integer, Map<Integer, Integer>> map_of_relevances = new HashMap<>();

        while((s = br.readLine()) != null) {
            String[] parsed = s.split(" ");
            int query_id = Integer.parseInt(parsed[0]);
            int doc_id = Integer.parseInt(parsed[1]);
            int relv = Integer.parseInt(parsed[2]);
            relv = 5-relv;

            if(map_of_relevances.containsKey(query_id)){
                map_of_relevances.get(query_id).put(doc_id,relv);
            }
            else{
                Map<Integer, Integer> tmp_mapv = new TreeMap();
                tmp_mapv.put(doc_id,relv);
                map_of_relevances.put(query_id,tmp_mapv);
            }

        }
        freader.close();
        return map_of_relevances;

    }

    public static void relevantAndNonRel(Map<Integer, Map<Integer, Integer>> explicit_rel, Map<Integer, ArrayList<Integer>> implicit_rel){

        for(Map.Entry<Integer, ArrayList<Integer>> entry : implicit_rel.entrySet()){
            ArrayList<Integer> tmpArrayList = entry.getValue(); // arraylist docs
            HashMap<Integer, Integer> docIdRelevanceHM = new HashMap<>();
            ArrayList<Integer> nonRelevantDocs = new ArrayList<>();
            TreeMap<Integer, Integer> tmp = (TreeMap<Integer, Integer>) explicit_rel.get(entry.getKey());
            for(Integer docRel: tmpArrayList) {
                if(explicit_rel.containsKey(entry.getKey())) {
                    if(tmp.containsKey(docRel))
                        docIdRelevanceHM.put(docRel, tmp.get(docRel));
                    else
                        nonRelevantDocs.add(docRel);
                }
            }
            relevantDocsHM.put(entry.getKey(), docIdRelevanceHM);
            nonRelevantDocsHM.put(entry.getKey(), nonRelevantDocs);
        }
    }


}
