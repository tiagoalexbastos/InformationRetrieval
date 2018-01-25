package T4.Word2Vec;

import org.deeplearning4j.text.sentenceiterator.BasicLineIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;

import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.logging.Level;

public class Word2Vec {

    private static String filePath;
    private static SentenceIterator iter;
    private static org.deeplearning4j.models.word2vec.Word2Vec vec;
    private static final String filename = "word_2_vec.txt";

    public Word2Vec() {
        filePath = filename;
        initialize();
    }

    private void initialize() {

        try {
            iter = new BasicLineIterator(filePath);
        } catch (FileNotFoundException ex) {
            java.util.logging.Logger.getLogger(Word2Vec.class.getName()).log(Level.SEVERE, null, ex);
        }

        TokenizerFactory t = new DefaultTokenizerFactory();

        t.setTokenPreProcessor(new CommonPreprocessor());

        vec = new org.deeplearning4j.models.word2vec.Word2Vec.Builder()
                .minWordFrequency(5)
                .iterations(1)
                .layerSize(100)
                .seed(42)
                .windowSize(5)
                .iterate(iter)
                .tokenizerFactory(t)
                .build();

        vec.fit();
    }

    public Collection<String> generateNearestWords(String term, int numWords) {
        Collection<String> lst = vec.wordsNearest(term, numWords);
        return lst;
    }

}
