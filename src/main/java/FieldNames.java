public enum FieldNames {
    DOCNO("docno"),
    HEADLINE("headline"),
    TEXT("text");

    private final String mName;

    FieldNames(String name) {
       mName = name;
    }

    public String getName(){
        return mName;
    }
}
