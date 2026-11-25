package app3_query_server;

import com.google.gson.Gson;
import common.SearchRequest;
import common.SearchResponse;
import common.SearchResponseResultItem;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QueryServer {

    //διαβάζει το αντεστραμμένο ευρετήριο από τον φάκελο data/lucene_inverted_index μέσω του DirectoryReader
    //αρχικοποιεί τον IndexSearcher και τον ίδιο EnglishAnalyzer με το App 2 για τη σωστή επεξεργασία των ερωτημάτων
    //ανοίγει ένα TCP ServerSocket στη θύρα 9090 και μπαίνει σε ατέρμονο server loop περιμένοντας αιτήματα από clients
    //διαβάζει το JSON request από το socket
    // αν λάβει την εντολή "shutdown" ή "terminate" τερματίζει χειροκίνητα το loop με break
    //υποβάλλει το ερώτημα στον IndexSearcher μέσω του QueryParser και ανακτά τα κορυφαία αποτελέσματα (TopDocs) βάσει συνάφειας
    //τέλος μετατρέπει τα αποτελέσματα (filename, path, score, snippet) σε JSON response και τα στέλνει πίσω στο port του client

    private static final String invertIndexDirectory = "data/lucene_inverted_index";
    private static final String inputPdfsDirectory = "data/input_pdfs/";
    private static final int port = 9090;//θύρα στην οποία "ακούει" ο tcp server

    public static void main(String[] args){

        Gson gson = new Gson();

        try(Directory indexDir = FSDirectory.open(Paths.get(invertIndexDirectory))){//σύνδεση με το directory data/lucene_inverted_index
            Analyzer analyzer = new EnglishAnalyzer();
            IndexReader reader = DirectoryReader.open(indexDir);//άνοιγμα του αντεστραμμένου ευρετηρίου για ανάγνωση
            ServerSocket socket = new ServerSocket(port);//εκκίνηση tcp server στην θύρα 9090

            IndexSearcher searcher = new IndexSearcher(reader);//δημιουργία βασικού εργαλείου αναζήτησης, εκτελεί τα queries, υπολογίζει το scoring και επιστρέφει τα id των κορυφαίων εγγράφων
            StoredFields storedFields = searcher.storedFields();

            System.out.println("==========================================");
            System.out.println("Ο Query Server (App 3) ξεκίνησε επιτυχώς!");
            System.out.println("Φορτώθηκαν " + reader.numDocs() + " έγγραφα από το ευρετήριο.");
            System.out.println("Αναμονή αιτημάτων στη θύρα TCP: " + port);
            System.out.println("==========================================");

            while(true){
                try(Socket clientSocket = socket.accept();//αποδοχή εισερχόμενης σύνδεσης client
                    BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                    PrintWriter output = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true);
                    ){

                    String requestJson = input.readLine();//ανάγνωση γραμμής Json από τον client
                    if(requestJson == null || requestJson.isBlank()){//έλεγχος για κενό αίτημα
                        continue;
                    }
                    System.out.println("[Request] "+requestJson);

                    SearchRequest request;
                    try{
                        request =  gson.fromJson(requestJson, SearchRequest.class);//μετατροπή Json σε SearchRequest
                    }catch(Exception e){
                        SearchResponse errorResponse = new SearchResponse(0, Collections.emptyList(),"ERROR: Invalid JSON");
                        output.println(gson.toJson(errorResponse));//αποστολή μηνύματος σφάλματος στον client
                        continue;
                    }

                    String rawQuery = request.getQuery() != null ? request.getQuery().trim() : "";//εξαγωγή κειμένου ερωτήματος
                    if(rawQuery.equalsIgnoreCase("shutdown") || rawQuery.equalsIgnoreCase("terminate")){
                        System.out.println("Λήφθηκε εντολή τερματισμού ("+rawQuery+"). Κλείσιμο server...");
                        SearchResponse endResponse = new SearchResponse(0, Collections.emptyList(),"SHUTDOWN");
                        output.println(gson.toJson(endResponse));
                        break;
                    }
                    if(rawQuery.isEmpty()){
                        SearchResponse emptyResponse = new SearchResponse(0,Collections.emptyList(),"EMPTY QUERY");
                        output.println(gson.toJson(emptyResponse));
                        continue;
                    }

                    SearchResponse response = executeSearch(searcher, storedFields, analyzer, rawQuery, request.getMaxResults());
                    String responseJson = gson.toJson(response);
                    output.println(responseJson);
                    System.out.println("[Response] Βρέθηκαν "+response.getTotalHits()+" έγγραφα και επεστράφησαν "+response.getResults().size());
                }catch(IOException e){
                    System.out.println("Σφάλμα επικοινωνίας με client: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }catch(IOException e){
            System.out.println("Κρίσιμο σφάλμα στον Query Server: "+ e.getMessage());
            e.printStackTrace();
        }
        System.out.println("Ο Query Server (App 3) τερματίστηκε οριστικά.");
    }

    public static SearchResponse executeSearch(IndexSearcher searcher, StoredFields storedFields, Analyzer analyzer, String rawQuery, int maxResults){
        try{
            QueryParser parser = new QueryParser("content", analyzer);
            Query query = parser.parse(rawQuery);

            TopDocs topDocs = searcher.search(query,maxResults);//αναζήτηση των κορυφαίων εγγράφων στο ευρετήριο
            long totalHits = topDocs.totalHits.value();//συνολικός αριθμός αποτελεσμάτων που βρέθηκαν

            List<SearchResponseResultItem> items = new ArrayList<>();//λίστα με τα αποτελέσματα που θα επιστραφούν

            for(ScoreDoc scoreDoc : topDocs.scoreDocs){//επανάληψη για κάθε αποτέλεσμα
                Document doc = storedFields.document(scoreDoc.doc);
                String filename = doc.get("filename");
                String path = inputPdfsDirectory+filename;//πλήρης διαδρομή του αρχείου

                double roundedScore = Math.round(scoreDoc.score*100)/100.0;

                //δημιουργία γρήγορου preview κυρίως των πρώτων 200 χαρακτήρων του αποθηκευμένου κειμένου
                String content = doc.get("content");
                String snippet = (content!=null && content.length() > 200)
                        ? content.substring(0, 200)+"..."
                        : (content!=null ? content:"");
                items.add(new SearchResponseResultItem(filename,path, roundedScore, snippet));
            }
            return new SearchResponse(totalHits, items,"OK");
        }catch(ParseException e){
            return new SearchResponse(0,Collections.emptyList(),"ERROR: Invalid query syntax - "+e.getMessage());
        }catch(IOException e){
            return new SearchResponse(0,Collections.emptyList(),"ERROR: Search I/0 failure - "+e.getMessage());
        }
    }
}