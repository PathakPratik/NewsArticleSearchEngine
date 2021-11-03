public enum FieldNames {
    ID("id"),
    TITLE("title"),
    AUTHOR("author"),
    BIBLIOGRAPHY("bibliography"),
    DESCRIPTION("description");

    private final String mName;

    FieldNames(String name) {
       mName = name;
    }

    public String getName(){
        return mName;
    }
}
