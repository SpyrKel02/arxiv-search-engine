package common;

public class SearchRequest {//κλάση για το αίτημα αναζήτησης του χρήστη
    private String query;//κείμενο αναζήτησης
    private int maxResults;//μέγιστος αριθμός αποτελεσμάτων

    public SearchRequest() {}

    public SearchRequest(String query, int maxResults) {
        this.query = query;
        this.maxResults = maxResults;
    }

    public String getQuery() {return this.query;}
    public int getMaxResults() {return this.maxResults > 0 ? this.maxResults : 10;}//10 by default αν δεν δοθεί θετικός αριθμός
}