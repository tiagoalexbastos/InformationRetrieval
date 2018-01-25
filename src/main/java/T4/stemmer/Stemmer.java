package T4.stemmer;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import java.util.ArrayList;
import java.util.List;

/**
 * Create by Pedro Matos & Tiago Bastos
 */

public class Stemmer {

    private SnowballStemmer stemmer;

    public Stemmer() {
        this.stemmer = new englishStemmer();
    }

    public List<String> stem(List<String> words) {

        List<String> stemmedWords = new ArrayList<String>();

        for (String word : words) {
            stemmedWords.add(stem(word));
        }

        return stemmedWords;
    }

    public String stem(String word) {

        stemmer.setCurrent(word);

        if (stemmer.stem()) {
            return stemmer.getCurrent();
        }

        return word;
    }
}
