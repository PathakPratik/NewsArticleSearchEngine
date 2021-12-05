import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

import org.jsoup.*;

public class Parser {
    // <! identifier for the analyzer that is to be created from
    // AnalyzerSimilarityFactory
    private String mAnalyzerString;
    // <! identifier for the similarity that is to be created from
    // AnalyzerSimilarityFactory
    private String mSimilarityString;
    // Identifier used to separate collection into lucene documents
    private static final String cDOCUMENT_SEPARATOR = "<DOC>";
    // Identifier used to separate the individual queries
    private static final String cQUERIES_SEPARATOR = "<top>";
    // Identifier used to separate the individual queries
    private static final String cQUERIES_TITLE = "title";
    // Identifier used to separate the individual queries
    private static final String cQUERIES_DESCRIPTION = "desc";
    // Identifier used to separate the individual queries
    private static final String cQUERIES_NARRATIVE = "narr";
    // Identifier used to identify the topic number
    private static final String cQUERIES_NUM = "num";


    Parser(String analyzer, String similarity) {
        mAnalyzerString = analyzer;
        mSimilarityString = similarity;
    }

    /**
     * This method extracts all the relevant info from the dataset that we need
     * for creating the index. This method creates the index after extraction
     *
     * @param ftLocation             location of the Financial Times Limited dataset
     * @param fr94Location           location of the Federal Register dataset
     * @param fbisLocation           location of the Foreign Broadcast Information
     *                               Service dataset
     * @param latimesLocation        location of the Los Angeles Times dataset
     * @param indexDirectoryLocation location where the created index should be
     *                               stored
     * @return true if parsing was successful. Otherwise, false
     */
    public boolean createIndex(String ftLocation, String fr94Location, String fbisLocation, String latimesLocation,
                               String indexDirectoryLocation) throws IOException {
        System.out.println(Paths.get(indexDirectoryLocation).toAbsolutePath());
        Directory directory = FSDirectory.open(Paths.get(indexDirectoryLocation));

        // Set up an index writer to add process and save documents to the index
        IndexWriterConfig config = new IndexWriterConfig(AnalyzerSimilarityFactory.getAnalyzer(mAnalyzerString,"index"));
        config.setSimilarity(AnalyzerSimilarityFactory.getSimilarity(mSimilarityString));
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter indexWriter = new IndexWriter(directory, config);

        String[] locations = {ftLocation, fbisLocation, latimesLocation, fr94Location};
        boolean successful = indexCollections(locations, indexWriter);

        // add the created documents to the index and close everything
        indexWriter.close();
        directory.close();
        System.out.println("Indexing finished successfully");
        return successful;
    }

    /**
     * Generic function to recursively get all the files to be indexed from a directory
     * @param dir Files are searched in this directory
     * @return List of all files to be indexed
     */
    private static ArrayList<File> getFilesFromDir(File dir){
        ArrayList<File> files = new ArrayList();
        for (File nested : dir.listFiles()) {
            if(nested.isDirectory()) {
                files.addAll(getFilesFromDir(nested));
            }else if( !nested.getName().matches(".*(read|Store).*") ){
                files.add(nested);
            }
        }
        return files;
    }

    /**
     * This will index all the documents from FR94
     * @param locations Location of FR94 dataset
     * @param indexWriter the index writer used to create the index
     * @return boolean success value
     * @throws IOException
     */
    private static boolean indexCollections(String[] locations, IndexWriter indexWriter) throws IOException {
        for (String location: locations) {
            File dir = new File(location);
            List<Document> documents = new ArrayList<>();

            for (File file:getFilesFromDir(dir)) {
                Scanner scan = new Scanner(file);
                scan.useDelimiter(Pattern.compile(cDOCUMENT_SEPARATOR));
                System.out.println(file.getName());
                while (scan.hasNext()) {
                    String docRaw = scan.next();
                    Document luceneDoc = createDocument(formatDocument(docRaw));
                    documents.add(luceneDoc);
                }
            }
            indexWriter.addDocuments(documents);
        }
        return true;
    }

    /**
     * Extracts documents for field information
     * @param docRaw documents to be parsed
     * @return document parsed map of fields
     */
    private static Map formatDocument(String docRaw){
        org.jsoup.nodes.Document docu = Jsoup.parse(docRaw);
        String text = docu.body().select(FieldNames.TEXT.getName()).text();
        String docno = docu.body().select(FieldNames.DOCNO.getName()).text();

        Map<String, String> doc = new HashMap<>();

        doc.put(FieldNames.DOCNO.getName(), docno);
        doc.put(FieldNames.TEXT.getName(), text);

        return doc;
    }

    /**
     * Creates a new Lucene document with given data
     * @param doc Map of data to create a doc
     * @return Lucene document
     */
    private static Document createDocument(Map doc)
    {
        Document document = new Document();
        document.add(new TextField(FieldNames.DOCNO.getName(), doc.get(FieldNames.DOCNO.getName()).toString() , Field.Store.YES));
        document.add(new TextField(FieldNames.TEXT.getName(), doc.get(FieldNames.TEXT.getName()).toString() , Field.Store.YES));
        return document;
    }

    /**
     * This method extracts the queries from the topics file and writes them to a
     * <Integer,String> map (id of the query is mapped to the search text).
     *
     * @param queryFileLocation the location of the file which contains the queries
     * @return a map <Integer,String> which maps id of a query to its search text
     * @throws IOException if the topics query file could not be read in
     */
    public HashMap<Integer, String[]> createQueries(String queryFileLocation) throws IOException {
        System.out.println("Started extracting queries");
        HashMap<Integer, String[]> queryMap = new HashMap<>();
        Scanner scan = new Scanner(new File(queryFileLocation));
        scan.useDelimiter(Pattern.compile(cQUERIES_SEPARATOR));

        //int id = 1;
        while (scan.hasNext()) {
            String docRaw = scan.next();
            org.jsoup.nodes.Document docu = Jsoup.parse(docRaw);
            String idText = docu.body().select(cQUERIES_NUM).text();
            int id = Integer.parseInt(idText.split(" ")[1]);
            String title = docu.body().select(cQUERIES_TITLE).text();
            String description = docu.body().select(cQUERIES_DESCRIPTION).
                    get(0).
                    ownText().
                    replace("Description: ", "");   // the desc tag always contains a first line
                                                                    // "Description: ". We replace this, since
                                                                    // this is of no use for the queries
            String narrative = docu.body().select(cQUERIES_NARRATIVE).
                    get(0).
                    ownText().
                    replace("Narrative: ", "")
                    .replace("relevant", "")
                    .replace("documents", "")
                    .replace("document", "");   // the narrative tag always contains a first line
                                                            // "Narrative: ". We replace this, since
                                                            // this is of no use for the queries
            
            String[] queryArray = {title,description,narrative};

            queryMap.put(id, queryArray);
        }

        System.out.println("Finished extracting queries");
        return queryMap;
    }
}
