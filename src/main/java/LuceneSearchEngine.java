import org.apache.lucene.queryparser.classic.ParseException;

import java.io.IOException;
import java.util.HashMap;

public class LuceneSearchEngine {
    //!< the location where the index should be created
    private static final String cINDEX_DIRECTORY_LOCATION = "../../index";
    //!< the location where the queries file is stored
    private static final String cQUERIES_LOCATION = "../../../cran/cran.qry";
    //!< the location where the cran dataset file is stored
    private static final String cCRAN_DATASET_LOCATION = "../../../cran/cran.all.1400";

    public static void main(String[] args)
    {
        if(args.length != 2) {
            System.out.println("Wrong number of arguments passed. Expected 2. Actual: " + args.length);
            System.exit(1);
        }
        try {
            Parser parser = new Parser(args[0] /*the selected analyzer*/,
                    args[1] /*the selected similarity*/);
            if(parser.createIndex(cCRAN_DATASET_LOCATION, cINDEX_DIRECTORY_LOCATION)) {
                HashMap<Integer,String> queries  = parser.createQueries(cQUERIES_LOCATION);
                QueryIndex queryIndex = new QueryIndex(args[0] /*the selected analyzer*/,
                        args[1] /*the selected similarity*/);
                queryIndex.queryMap(queries, cINDEX_DIRECTORY_LOCATION);
            }
            else {
                System.out.println("Error. Could not create index");
                System.exit(1);
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
