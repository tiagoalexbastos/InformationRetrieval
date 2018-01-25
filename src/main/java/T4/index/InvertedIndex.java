package T4.index;

import T4.utils.Posting;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.util.*;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.apache.commons.io.FileUtils;

/**
 * Created by Pedro Matos & Tiago Bastos
 * This is the class responsible for the Inverted Index
 *
 * Here we have a TreeMap that will work as a dictionary, with the terms and their's posting list.
 */
public class InvertedIndex {

    private static final String FILE_NAME = "index";
    private int cont;
    private Map<String, List<Posting>> dic;
    private Set<String> terms;
    private Map<String, Integer> term_count;
    private File outputDir;

    public InvertedIndex(File outputDir){

        this.dic = new TreeMap();
        this.terms = new HashSet<String>();
        this.outputDir = outputDir;
        this.cont = 0;
        this.term_count = new HashMap<>();
        start_index();
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

    public void addTokens(int docId, List<String> tokens){
        /*
            Count the tokens
         */
        Multiset<String> multiset = HashMultiset.create(tokens);

        Posting tmp_posting;

        for(String token : multiset.elementSet()){
            tmp_posting = new Posting(docId,multiset.count(token));

            if(dic.containsKey(token)){
                dic.get(token).add(tmp_posting);
            }
            else{
                List<Posting> list = new ArrayList<Posting>();
                list.add(tmp_posting);
                dic.put(token,list);
            }
            terms.add(token);

            if(term_count.containsKey(token)){
                int value = term_count.get(token);
                value++;
                term_count.put(token,value);
            }
            else{
                term_count.put(token,1);
            }

        }

        /*
           Check memory usage by dictionary. Print file and erase if bigger than 80%
         */
        if(getMemory()>80){
            writeFile();
            dic.clear();
            System.gc();
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

            for(Map.Entry<String, List<Posting>> entry : dic.entrySet()){
                pwt.println(entry.getKey() + "\t" + entry.getValue());
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

    public Map<String, List<Posting>> getDic() {
        return dic;
    }

    public Set<String> getTerms() {
        return terms;
    }

    public Map<String, Integer> getTerm_count() {
        return term_count;
    }

    /**
     * Write the file if the dic is not empty. Can be empty if it has been writing when the memory was low
     */
    public void close() {
        if (!dic.isEmpty()) {
            writeFile();
        }
    }

}
