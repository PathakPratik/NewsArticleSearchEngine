import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;

public class QueryIndex {
    //<!
    private final short cTOP_TERMS_LIMIT = 20;
    //<!
    private final float cEXPANDED_TERM_WEIGHT = 0.5F;
    //<!
    private final short cMAX_RESULTS_FIRST_PASS = 10;
    //<! The maximum number of search results that are retrieved for a query
    private final short cMAX_RESULTS_SECOND_PASS = 1000;
    //<! The location where the file with the rankings of the queries is stored
    private final String cRANKINGS_LOCATION = "./rankings.txt";
    //<! identifier for the analyzer that is to be created from AnalyzerSimilarityFactory
    private String mAnalyzerString;
    //<! identifier for the similarity that is to be created from AnalyzerSimilarityFactory
    private String mSimilarityString;

    QueryIndex(String analyzer, String similarity) {
        mAnalyzerString = analyzer;
        mSimilarityString = similarity;
    }

    /**
     * This comparator ranks a map by its (double) value in descending order
     */
    static class TermMapComparator implements Comparator<String>{
        Map<String,Double> map;
        TermMapComparator(Map<String,Double> map) {
            this.map = map;
        }


        @Override
        public int compare(String o1, String o2) {
            if (map.get(o1) < map.get(o2)) {
                return 1;
            } else if (map.get(o1).equals(map.get(o2))) {
                return 0;
            }
            return -1;
        }
    }

    /**
     *  This function executes a set of queries on a given index and writes the resulting
     *  hit scores into a file located at cRANKINGS_LOCATION
     *
     * @param queries a map <Integer,String> which maps id of a query to its search text
     * @param indexDirectoryLocation location where the created index should be stored
     * @throws IOException when the directory could not be opened
     * @throws ParseException when a query could not be parsed
     */
    public void queryMap(HashMap<Integer,String> queries,
                         String indexDirectoryLocation) throws IOException, ParseException {
        Directory directory = FSDirectory.open(Paths.get(indexDirectoryLocation));
        DirectoryReader directoryReader = DirectoryReader.open(directory);
        IndexReader indexReader = DirectoryReader.open(directory);
        IndexSearcher indexSearcher = new IndexSearcher(directoryReader);
        indexSearcher.setSimilarity(AnalyzerSimilarityFactory.getSimilarity(mSimilarityString));
        MultiFieldQueryParser parser = new MultiFieldQueryParser(new String[]{FieldNames.TEXT.getName()},
                AnalyzerSimilarityFactory.getAnalyzer(mAnalyzerString));

        PrintWriter writer = new PrintWriter(cRANKINGS_LOCATION, StandardCharsets.UTF_8);
        System.out.println("Started querying");
        for (int id : queries.keySet()) {
            //parse the current original query and use it to search the index
            Query origQuery = parser.parse(QueryParser.escape(queries.get(id)));

            //get top results for first iteration of the query and extract their terms
            ScoreDoc[] hits = indexSearcher.search(origQuery, cMAX_RESULTS_FIRST_PASS).scoreDocs;
            Map<String,Double> termWeightMap = new HashMap<>();
            for (ScoreDoc hit : hits)
            {
                Document hitDoc = indexSearcher.doc(hit.doc);
                List<String> termList = tokenizeString(Arrays.toString(hitDoc.getValues(FieldNames.TEXT.getName())));
                for (String currTerm : termList) {

                    termWeightMap.put(currTerm, calculateTermWeight(currTerm,
                                                            termList,
                                                            indexSearcher,
                                                            indexReader));
                }
            }
            //rank all terms from the top documents, so we can get the top @cTOP_TERMS_LIMIT terms
            Map<String,Double> topTermMap = new TreeMap<>(new TermMapComparator(termWeightMap));
            topTermMap.putAll(termWeightMap);
            //reduce map to top @cTOP_TERMS_LIMIT terms
            topTermMap = topTermMap.entrySet().stream().limit(cTOP_TERMS_LIMIT)
                    .collect(TreeMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), Map::putAll);

            //construct the final query with the extracted terms
            BooleanQuery finalQuery = new BooleanQuery.Builder().add(origQuery, BooleanClause.Occur.SHOULD).build();
            for (String currTerm : topTermMap.keySet()){
                TermQuery termQuery = new TermQuery(new Term(FieldNames.TEXT.getName(),currTerm));
                BoostQuery boostQuery = new BoostQuery(termQuery, cEXPANDED_TERM_WEIGHT);
                finalQuery = new BooleanQuery.Builder().add(finalQuery, BooleanClause.Occur.SHOULD).
                        add(boostQuery, BooleanClause.Occur.SHOULD).build();
            }

            //finally, get the final results with the expanded query
            hits = indexSearcher.search(finalQuery, cMAX_RESULTS_SECOND_PASS).scoreDocs;
            for (ScoreDoc hit : hits)
            {
                Document hitDoc = indexSearcher.doc(hit.doc);
                writer.println(id + " 0 " + hitDoc.get(FieldNames.DOCNO.getName()) + " 0 " + hit.score + " GROUP10");
            }
        }

        //close everything we used
        writer.close();
        directoryReader.close();
        directory.close();
        System.out.println("Finished querying");
    }

    /**
     *  This function tokenizes a string with the analyzer that was selected by @mAnalyzerString
     *
     *  @param string the string to be tokenized
     *  @return the tokenized string
     */
    //Credits: https://stackoverflow.com/questions/6334692/how-to-use-a-lucene-analyzer-to-tokenize-a-string
    private List<String> tokenizeString(String string) {
        List<String> result = new ArrayList<String>();
        try {
            Analyzer analyzer = AnalyzerSimilarityFactory.getAnalyzer(mAnalyzerString);
            TokenStream stream  = analyzer.tokenStream(null, new StringReader(string));
            stream.reset();
            while (stream.incrementToken()) {
                result.add(stream.getAttribute(CharTermAttribute.class).toString());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    /**
     *
     * @param term
     * @param termList
     * @param searcher
     * @param reader
     * @return the calculated term weight
     */
    private double calculateTermWeight(String term,
                                       List<String> termList,
                                       IndexSearcher searcher,
                                       IndexReader reader) {
        CollectionStatistics collectionStats = null;
        try {
            collectionStats = searcher.collectionStatistics(FieldNames.TEXT.getName());
            double totalDocCount = collectionStats.docCount();
            Term termInstance = new Term(FieldNames.TEXT.getName(),term);
            double docCountWithTerm = reader.docFreq(termInstance);
            double tf = 1 + Math.log(1 + Math.log((double)Collections.frequency(termList,term)));
            double idf = Math.log10((totalDocCount+1)/docCountWithTerm);
            return tf*idf;
        } catch (IOException e) {
            System.out.println("Error while calculating term weight");
            e.printStackTrace();
        }
        return 0;
    }
}
