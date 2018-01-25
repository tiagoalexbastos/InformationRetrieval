package T4.stopwords;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * Create by Pedro Matos & Tiago Bastos
 */
public class StopWordsRemover {

    private HashSet<String> stopwords;
    private String stopWordsfile;

    public StopWordsRemover(String stopWordsfile) {
        stopwords = new HashSet<String>();
        this.stopWordsfile = stopWordsfile;
        loadWords();
    }

    private void loadWords() {
        String line;
        try {
            BufferedReader br = new BufferedReader(new FileReader(stopWordsfile));
            while ((line = br.readLine()) != null)
                stopwords.add(line.replaceAll("'",""));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public HashSet<String> getStopwords() {
        return stopwords;
    }

    public boolean containsStopWord(String word) {
        return stopwords.contains(word);
    }

    public List<String> removeStopWords(List<String> tokens) {
        List<String> filteredTokens = new ArrayList<String>(tokens);

        for (String token : new HashSet<String>(tokens)) {
            if (stopwords.contains(token)) {
                filteredTokens.removeAll(Collections.singleton(token));
            }
        }

        return filteredTokens;
    }

}
