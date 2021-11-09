
import java.io.IOException;

public class LuceneSearchEngine {
    //!< the location where the index should be created
    private static final String cINDEX_DIRECTORY_LOCATION = "./index";

    //!<the location where the financial times limited files are stored
    private static final String cFINANCIAL_TIMES_LIMITED_LOCATION = "./resources/newsarticles/Assignment Two/ft";
    //!<the location where the federal register files are stored
    private static final String cFEDERAL_REGISTER_LOCATION = "./resources/newsarticles/Assignment Two/fr94";
    //!<the location where the foreign broadcast information service files are stored
    private static final String cFOREIGN_BROADCAST_INFORMATION_SERVICE_LOCATION = "./resources/newsarticles/Assignment Two/fbis";
    //!<the location where the los angeles times files are stored
    private static final String cLOS_ANGELES_LOCATION = "./resources/newsarticles/Assignment Two/latimes";
    //!<the location where the topics file is stored
    private static final String cTOPICS_LOCATION = "./resources/topics";

    public static void main(String[] args)
    {
        if(args.length != 2) {
            System.out.println("Wrong number of arguments passed. Expected 2. Actual: " + args.length);
            System.exit(1);
        }
        try {
            Parser parser = new Parser(args[0] /*the selected analyzer*/,
                    args[1] /*the selected similarity*/);
            if(parser.createIndex(cFINANCIAL_TIMES_LIMITED_LOCATION, 
                cFEDERAL_REGISTER_LOCATION, 
                cFOREIGN_BROADCAST_INFORMATION_SERVICE_LOCATION, 
                cLOS_ANGELES_LOCATION, 
                cINDEX_DIRECTORY_LOCATION)) 
            {
           //     HashMap<Integer,String> queries  = parser.createQueries(cTOPICS_LOCATION); // <- TODO: Implementation
           //     QueryIndex queryIndex = new QueryIndex(args[0] /*the selected analyzer*/,
           //             args[1] /*the selected similarity*/);
           //     queryIndex.queryMap(queries, cINDEX_DIRECTORY_LOCATION);
            }
            else {
                System.out.println("Error. Could not create index");
                System.exit(1);
            }
        } catch (IOException e){ //| ParseException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
