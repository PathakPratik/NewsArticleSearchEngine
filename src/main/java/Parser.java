import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.*;

public class Parser {
    // <! constant for an empty string
    private final String cEMPTY_STRING = "";
    // <! identifier for the analyzer that is to be created from
    // AnalyzerSimilarityFactory
    private String mAnalyzerString;
    // <! identifier for the similarity that is to be created from
    // AnalyzerSimilarityFactory
    private String mSimilarityString;

    Parser(String analyzer, String similarity) {
        mAnalyzerString = analyzer;
        mSimilarityString = similarity;
    }

    /**
     * Extracts a substring from a string based on a regular expression
     * 
     * @param input the input string
     * @param regex the regular expression
     * @return the extracted substring. Return an empty string, if the regex did not
     *         match
     */
    private String extractPattern(String input, String regex) {
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return matcher.group(0);
        }
        return cEMPTY_STRING;
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
        IndexWriterConfig config = new IndexWriterConfig(AnalyzerSimilarityFactory.getAnalyzer(mAnalyzerString));
        config.setSimilarity(AnalyzerSimilarityFactory.getSimilarity(mSimilarityString));
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter indexWriter = new IndexWriter(directory, config);

        String[] locations = {fbisLocation, latimesLocation, fr94Location};
        boolean successful = indexCollections(locations, indexWriter) && indexFt(ftLocation, indexWriter);

        // add the created documents to the index and close everything
        indexWriter.close();
        directory.close();
        System.out.println("Indexing finished successfully");
        return successful;
    }

    /**
     * This method extracts all the relevant info from the Financial Times Limited
     * dataset that we need for creating the index. This method creates the index
     * after extraction
     * 
     * @param ftLocation  location of the Financial Times Limited dataset
     * @param indexWriter the index writer used to create the index
     * @return true if parsing was successful. Otherwise, false
     */
    private boolean indexFt(String ftLocation, IndexWriter indexWriter) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            org.w3c.dom.Document xmlDoc;
            Enumeration<InputStream> stream;
            SequenceInputStream sequenceStream;

            File dir = new File(ftLocation);
            for (File nestedDir : dir.listFiles()) {
                System.out.println(nestedDir.getName());
                if (!nestedDir.getName().equals("readfrcg") && !nestedDir.getName().equals("readmeft") && !nestedDir.getName().equals(".DS_Store")) {
                    for (File file : nestedDir.listFiles()) {
                        System.out.println("    -" + file.getName());
                        stream = Collections.enumeration(
                                Arrays.asList(new InputStream[] { new ByteArrayInputStream("<root>".getBytes()),
                                        new FileInputStream(file), new ByteArrayInputStream("</root>".getBytes()), }));
                        sequenceStream = new SequenceInputStream(stream);
                        xmlDoc = db.parse(sequenceStream);

                        xmlDoc.getDocumentElement().normalize();
                        NodeList nodeList = xmlDoc.getElementsByTagName("DOC");

                        ArrayList<Document> documents = new ArrayList<>();

                        for (int i = 0; i < nodeList.getLength(); i++) {
                            Node node = nodeList.item(i);
                            // System.out.println("\nNode Name:" + node.getNodeName());
                            Element eElement = (Element) node;

                            Document luceneDoc = new Document();
                            boolean empty = true;

                            String docNo = eElement.getElementsByTagName("DOCNO").item(0).getTextContent().replaceAll("\n(?!\n)", " ").trim();
                            // System.out.println(docNo);
                            if (!docNo.isEmpty()) {
                                luceneDoc.add(new TextField(FieldNames.DOCNO.getName(), docNo, Field.Store.YES));
                            }

                            NodeList headlineField = eElement.getElementsByTagName("HEADLINE");
                            String headline = "";
                            if(headlineField.getLength() > 0)
                                 headline = headlineField.item(0).getTextContent().replaceAll("\n(?!\n)", " ").trim();
                            // System.out.println(headline);
                            if (!headline.isEmpty()) {
                                luceneDoc.add(new TextField(FieldNames.HEADLINE.getName(), headline, Field.Store.YES));
                                empty = false;
                            }

                            NodeList textField = eElement.getElementsByTagName("TEXT");
                            String text = "";
                            if(textField.getLength() > 0)
                                text = textField.item(0).getTextContent().replaceAll("\n(?!\n)", " ").trim();
                            // System.out.println(text);
                            if (!text.isEmpty()) {
                                luceneDoc.add(new TextField(FieldNames.TEXT.getName(), text, Field.Store.YES));
                                empty = false;
                            }

                            if (!empty) {
                                documents.add(luceneDoc);
                            }
                        }
                        indexWriter.addDocuments(documents);
                    }
                }
            }
            return true;
        } catch (Exception e) {
            // TODO
            e.printStackTrace();
            return false;
        }
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
                scan.useDelimiter(Pattern.compile("<DOC>"));
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
        document.add(new TextField(FieldNames.TEXT.getName(), doc.get(FieldNames.TEXT.getName()).toString() , Field.Store.NO));
        return document;
    }

    /**
     * This method extracts the queries from the cran.qry file and writes them to a
     * <Integer,String> map (id of the query is mapped to the search text).
     *
     * @param queryFileLocation the location of the file which contains the queries
     * @return a map <Integer,String> which maps id of a query to its search text
     * @throws IOException if the cran query file could not be read in
     */
    public HashMap<Integer, String> createQueries(String queryFileLocation) throws IOException {
        System.out.println("Started extracting queries");
        HashMap<Integer, String> queryMap = new HashMap<>();
        String content = new String(Files.readAllBytes(Paths.get(queryFileLocation)));

        // split the document into the single queries
        String[] queries = content.split("(?=.I \\d{1,4}\\n)");
        int id = 1;
        for (String currQuery : queries) {
            // extract the query text
            String queryText = extractPattern(currQuery, "(?<=.W\\n).*");

            if (!queryText.isEmpty()) {
                queryMap.put(id, queryText);
            }
            id++;
        }
        System.out.println("Finished extracting queries");
        return queryMap;
    }
}