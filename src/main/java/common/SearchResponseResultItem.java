package common;

public class SearchResponseResultItem {//κλάση για κάθε επιστημονικό άρθρο στα αποτελέσματα
    private String filename;//όνομα αρχείου pdf
    private String path;//διαδρομή του αρχείου pdf
    private double score;
    private String snippet;//σύντομο απόσπασμα κειμένου για εμφάνιση στην διεπαφή

    public SearchResponseResultItem() {}

    public SearchResponseResultItem(String filename, String path, double score, String snippet) {
        this.filename = filename;
        this.path = path;
        this.score = score;
        this.snippet = snippet;
    }

    public String getFilename() {return this.filename;}
    public String getPath() {return this.path;}
    public double getScore() {return this.score;}
    public String getSnippet() {return this.snippet;}
}