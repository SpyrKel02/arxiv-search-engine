package app4_query_client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import common.SearchRequest;
import common.SearchResponse;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class QueryClient {

    private static final int server_port = 9090;
    private static final String server_host = "localhost";

    public static void main(String[] args){

        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);//αρχικοποίηση αντικειμένου τύπου Scanner για ανάγνωση από το πληκτρολόγιο

        System.out.print("Client(App 4) - Σύνδεση με "+server_host+":"+server_port+"\n");
        System.out.print("Πληκτρολογήστε το ερώτημα σας (π.χ. artificial intelligence, machine learning, cybersecurity");
        System.out.println("\nΠληκτρολογήστε 'shutdown' για τον τερματισμό του Server ή 'exit' για τον τερματισμό του Client");

        while(true){
            System.out.print("\nΑναζήτηση: ");

            if(!scanner.hasNextLine()){ break; }//έλεγχος για το αν έκλεισε η ροή εισόδου

            String userInput = scanner.nextLine().trim();
            if(userInput.isEmpty()){ continue; }//σε περίπτωση που πατήθηκε Enter, τότε αυτό αγνοείται και συνεχίζει κανονικά

            if(userInput.equalsIgnoreCase("exit")){//έλεγχος για το αν πληκτρολογήθηκε το exit
                System.out.println("Έξοδος από τον Client");
                break;//διακόπτεται ο βρόχος και γίνεται έξοδος από τον Client
            }

            boolean serverShutdown = sendUserInput(userInput,10);
            if(serverShutdown){//έλεγχος για τον αν πληκτρολογήθηκε shutdown στην κονσόλα για να τερματιστεί ο client και η σύνδεση με τον server
                break;
            }
        }
    }

    public static boolean sendUserInput(String userInput, int maxResults){
        Gson gson = new Gson();
        Gson readableGson = new GsonBuilder().setPrettyPrinting().create();//αντικείμενο που είναι υπεύθυνο για ευανάγνωστη εκτύπωση

        SearchRequest searchRequest = new SearchRequest(userInput, maxResults);//δημιουργία αντικειμένου αιτήματος
        String request =  gson.toJson(searchRequest);//μετατροπή του αιτήματος σε json string

        try(Socket socket = new Socket(server_host, server_port)){//σύνδεση στην θύρα 9090 στο local host

            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8 ),true);//υπεύθυνο για την ροή αποστολής
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8));//υπεύθυνο για την ροή λήψης

            out.println(request);
            String responseJson = in.readLine();
            if(responseJson == null){
                System.out.println("Ο Server έκλεισε την σύνδεση χωρίς απάντηση");
                return false;
            }

            SearchResponse response = gson.fromJson(responseJson,SearchResponse.class);
            System.out.println(readableGson.toJson(response));
            return "SHUTDOWN".equalsIgnoreCase(response.getStatus());

        }catch(IOException e){
            System.out.println("Αδυναμία σύνδεσης με τον Server ("+server_host+":"+server_port+"). Πρέπει να τρέχει και το App 3 (Query Server) ταυτόχρονα.");
            return false;
        }
    }

}
