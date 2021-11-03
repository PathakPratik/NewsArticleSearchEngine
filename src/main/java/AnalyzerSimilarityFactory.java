import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.similarities.*;

public class AnalyzerSimilarityFactory {

    /**
     * This method constructs and returns different types of
     * analyzers based on an input string
     *
     * @param analyzerType the analyzer that should be returned
     * @return the respective analyzer
     */
    public static Analyzer getAnalyzer(String analyzerType){
        if(analyzerType.equalsIgnoreCase("standard")) {
            return new StandardAnalyzer();
        }
        if(analyzerType.equalsIgnoreCase("english")) {
            return new EnglishAnalyzer();
        }
        if(analyzerType.equalsIgnoreCase("whitespace")) {
            return new WhitespaceAnalyzer();
        }
        if(analyzerType.equalsIgnoreCase("custom")) {
            return new CustomAnalyzer();
        }

        System.out.println("WARNING! NO VALID ANALYZER SELECTED");
        return null;
    }

    /**
     * This method constructs and returns different types of
     * similarities based on an input string
     *
     * @param similaritiesType the similarity that should be returned
     * @return the respective similarity
     */
    public static Similarity getSimilarity(String similaritiesType){
        if(similaritiesType.equalsIgnoreCase("bm25")) {
            return new BM25Similarity();
        }
        if(similaritiesType.equalsIgnoreCase("classic")) {
            return new ClassicSimilarity();
        }
        if(similaritiesType.equalsIgnoreCase("lmd")) {
            return new LMDirichletSimilarity();
        }
        System.out.println("WARNING! NO VALID SIMILARITY SELECTED");
        return null;
    }
}
