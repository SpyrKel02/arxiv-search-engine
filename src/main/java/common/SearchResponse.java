package common;

import java.util.List;

public class SearchResponse {//κλάση για απάντηση του query server προς τον client
    private long totalHits;//συνολικός αριθμός εγγραφών που βρέθηκαν
    private List<SearchResponseResultItem> results;// λίστα με τα κορυφαία αποτελέσματα
    private String status;// κατάσταση απάντησης πχ ok,error,shutdown

    public SearchResponse() {}

    public SearchResponse(long totalHits, List<SearchResponseResultItem> results, String status){
        this.totalHits = totalHits;
        this.results = results;
        this.status = status;
    }

    public long getTotalHits() {return totalHits;}
    public List<SearchResponseResultItem> getResults() {return results;}
    public String getStatus() {return status;}
}