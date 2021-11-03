import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser
{
    //<! constant for an empty string
    private final String cEMPTY_STRING = "";
    //<! identifier for the analyzer that is to be created from AnalyzerSimilarityFactory
    private String mAnalyzerString;
    //<! identifier for the similarity that is to be created from AnalyzerSimilarityFactory
    private String mSimilarityString;

    Parser(String analyzer, String similarity) {
        mAnalyzerString = analyzer;
        mSimilarityString = similarity;
    }

    /**
        Extracts a substring from a string based on a regular expression

        @param input the input string
        @param regex the regular expression
        @return the extracted substring. Return an empty string, if the regex did not match
     */
    private String extractPattern(String input, String regex) {
        Pattern pattern = Pattern.compile(regex,Pattern.DOTALL);
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return matcher.group(0);
        }
        return cEMPTY_STRING;
    }

    /**
        This method extracts all the relevant info from the cran dataset that we need for
        creating the index. This method creates the index after extraction

        @param file location of the cran dataset
        @param indexDirectoryLocation location where the created index should be stored
        @return true if parsing was successful. Otherwise, false
     */
    public boolean createIndex(String file, String indexDirectoryLocation) throws IOException {
        ArrayList<Document> documents = new ArrayList<>();
        Directory directory = FSDirectory.open(Paths.get(indexDirectoryLocation));

        // Set up an index writer to add process and save documents to the index
        IndexWriterConfig config = new IndexWriterConfig(AnalyzerSimilarityFactory.getAnalyzer(mAnalyzerString));
        config.setSimilarity(AnalyzerSimilarityFactory.getSimilarity(mSimilarityString));
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter indexWriter = new IndexWriter(directory, config);

        try {
            String content = new String(Files.readAllBytes(Paths.get(file)));
            System.out.printf("Indexing \"%s\"\n", file);
            //split the document into the single entries
            String[]entries = content.split("(?=.I \\d{1,4}\\n)");
            for (String currEntry : entries)
            {
                //create a new document and add the respective fields
                Document doc = new Document();
                boolean empty = true;

                //extract id
                String id = extractPattern(currEntry,"(?<=.I )\\d{1,4}");
                if(!id.isEmpty()) {
                    doc.add(new TextField(FieldNames.ID.getName(), id, Field.Store.YES));
                }

                //extract title
                String title = extractPattern(currEntry,"(?<=.T\\n).*?(?=.A\\n)");
                if(!title.isEmpty()) {
                    doc.add(new TextField(FieldNames.TITLE.getName(), title, Field.Store.YES));
                    empty = false;
                }

                //extract author
                String authors = extractPattern(currEntry,"(?<=.A\\n).*?(?=.B\\n)");
                if(!authors.isEmpty()) {
                    doc.add(new TextField(FieldNames.AUTHOR.getName(), authors, Field.Store.YES));
                    empty = false;
                }

                //extract bibliography
                String bibliography = extractPattern(currEntry,"(?<=.B\\n).*?(?=.W\\n)");
                if(!bibliography.isEmpty()) {
                    doc.add(new TextField(FieldNames.BIBLIOGRAPHY.getName(), bibliography, Field.Store.YES));
                    empty = false;
                }

                //extract description
                String description = extractPattern(currEntry,"(?<=.W\\n).*");
                if(!description.isEmpty()) {
                    doc.add(new TextField(FieldNames.DESCRIPTION.getName(), description, Field.Store.NO));
                    empty = false;
                }
                //if the document contained nothing but an id -> do not add it to the index
                if(!empty)
                {
                    documents.add(doc);
                }
            }
        }
        catch (InvalidPathException | NoSuchFileException exception){
            System.out.println("Invalid path! Can not index: " + file);
            indexWriter.close();
            directory.close();
            return false;
        }
    
        //add the created documents to the index and close everything
        indexWriter.addDocuments(documents);
        indexWriter.close();
        directory.close();
        System.out.println("Indexing finished successfully");
        return true;
    }

    /**
     * This method extracts the queries from the cran.qry file and writes them
     * to a <Integer,String> map (id of the query is mapped to the search text).
     *
     * @param queryFileLocation the location of the file which contains the queries
     * @return a map <Integer,String> which maps id of a query to its search text
     * @throws IOException if the cran query file could not be read in
     */
    public HashMap<Integer,String> createQueries(String queryFileLocation) throws IOException {
        System.out.println("Started extracting queries");
        HashMap<Integer,String> queryMap = new HashMap<>();
        String content = new String(Files.readAllBytes(Paths.get(queryFileLocation)));

        //split the document into the single queries
        String[]queries = content.split("(?=.I \\d{1,4}\\n)");
        int id = 1;
        for (String currQuery : queries)
        {
            //extract the query text
            String queryText = extractPattern(currQuery,"(?<=.W\\n).*");

            if(!queryText.isEmpty()) {
                queryMap.put(id,queryText);
            }
            id++;
        }
        System.out.println("Finished extracting queries");
        return queryMap;
    }
}
