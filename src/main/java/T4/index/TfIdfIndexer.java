package T4.index;

import T4.utils.Posting;
import T4.utils.TfIdfWeighting;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.util.*;

/**
 * Created by pmatos9 on 09/11/17.
 */
public class TfIdfIndexer {

    private static final String FILE_NAME = "tf_idf_indexer";
    private int cont;
    private Map<String, List<Posting>> dic;
    private Map<String, List<TfIdfWeighting>> dic_weight;
    private Set<String> terms;
    private File outputDir;
    private int number_documents;

    public TfIdfIndexer(File outputDir, int number_documents){
        this.dic_weight = new TreeMap();
        this.outputDir = outputDir;
        this.cont = 0;
        this.number_documents = number_documents;
        start_index();
    }

    public void setDic(Map<String, List<Posting>> dic) {
        this.dic = dic;
    }

    public void setTerms(Set<String> terms) {
        this.terms = terms;
    }

    /**
     * Initializes inverted index.
     */
    private void start_index(){
     /*
        Create output dir
      */
        outputDir.mkdir();
        try {
            FileUtils.cleanDirectory(outputDir);
        } catch (IOException ex) {
            throw new RuntimeException("There was a problem cleaning the directory.", ex);
        }
    }



    public void addDocumentFrequency(){
        /* o terms contêm todos os termos
           temos de percorrer todos os termos, e a cada termo ir ao dicionario ver a lista de postings
           na lista de postings temos de ver em quantos dicionarios aparece ()
         */

        List<Posting> list;
        for (String term : terms) {
            list = dic.get(term);

            //percorrer os postings todos e obter a frequência total
            int docid;
            int freq;
            int doc_freq;
            double var_weight = 0.0;

            for(Posting post : list){
                docid = post.getDocId();
                freq = post.getFrequency();
                doc_freq = list.size();

                TfIdfWeighting tmp_weight = new TfIdfWeighting(term,docid,freq,doc_freq,this.number_documents);
                var_weight += Math.pow(tmp_weight.getFinal_weighting(),2);
                if(dic_weight.containsKey(term)){
                    dic_weight.get(term).add(tmp_weight);
                }
                else{
                    List<TfIdfWeighting> list_weights = new ArrayList<TfIdfWeighting>();
                    list_weights.add(tmp_weight);
                    dic_weight.put(term,list_weights);
                }
            }
            double norm = 1/Math.sqrt(var_weight);

            for(TfIdfWeighting w: dic_weight.get(term)){
                double weight_final = w.getFinal_weighting();
                w.setWeight_normalized(norm*weight_final);
            }

        }
    }

    /**
     * Write index file
     */
    public void writeFile(){
        File dir = new File(outputDir.getAbsolutePath());

        try {
            String blockFileName = FILE_NAME + cont++ + ".txt";
            PrintWriter pwt = new PrintWriter(new File(dir, blockFileName));

            for(Map.Entry<String, List<TfIdfWeighting>> entry : dic_weight.entrySet()){
                pwt.print(entry.getKey() + ";");

                int tmp_size = 0;
                for(TfIdfWeighting weight : entry.getValue()){
                    tmp_size++;
                    if (tmp_size<entry.getValue().size())
                        pwt.print(weight + ";");
                    else
                        pwt.print(weight);
                }
                pwt.println();
            }

            pwt.close();
        } catch (IOException ex) {
            throw new RuntimeException("There was a problem writing the index to a file", ex);
        }

    }




    /**
     * Check the percentage of memory used
     * @return double with the percentage
     */
    private double getMemory(){
        double usage = 0;
        for (MemoryPoolMXBean mpBean: ManagementFactory.getMemoryPoolMXBeans()) {
            if ((mpBean.getType() == MemoryType.HEAP) && mpBean.getName().equalsIgnoreCase("PS Eden Space")) {
                usage = ((double) mpBean.getUsage().getUsed()/mpBean.getUsage().getMax()) * 100;
            }
        }
        return usage;
    }

    public Map<String, List<TfIdfWeighting>> getDic_weight() {
        return dic_weight;
    }
}
