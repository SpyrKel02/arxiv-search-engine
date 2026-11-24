package app2_preprocessor_indexer;

import com.google.gson.Gson;
import common.RepoDocument;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;

public class LuceneIndexer {

    //εκκίνηση προεπεξεργασίας κειμένου μέσω διάφορων τεχνικών όπως Casefolding, punctuation removal, stopword removal, stemming
    //διαβάζει τα αρχεία/έγγραφα εισόδου format .jsonl από το data/repository
    //στην συνέχεια για κάθε έγγραφο δημιουργείται ένα αντικείμενο Lucene Document στην μνήμη
    //έπειτα κάθε επεξεργασμένο κείμενο στέλνεται στον Lucene IndexWriter για την κατασκευή αντεστραμμένου ευρετηρίου στον φάκελο lucene_inverted_index
    //μέσα στο ευρετήριο αποθηκεύονται τα docIDs, Term-Document Frequencies, Positions καθώς και το αρχικό κείμενο εισόδου
    //τέλος γίνεται commit και κλείσιμο του IndexWriter ώστε να γραφτούν οριστικά τα δεδομένα στον δίσκο

    private static final String repoDirectory = "data/repository";
    private static final String invertIndexDirectory = "data/lucene_inverted_index";

    public static void main(String[] args){

        long startTime = System.currentTimeMillis();

        File repoFolfer = new File(repoDirectory);//δημιουργία αντικειμένου repoFolder τύπου File για το φάκελο repository
        File[] repoFiles = repoFolfer.listFiles((dir, name) -> name.toLowerCase().endsWith(".jsonl"));
        if(repoFiles == null || repoFiles.length == 0){//έλεγχος αν υπάρχουν τα αρχεία jsonl στον φάκελο repository
            System.out.println("Δεν βρέθηκαν jsonl αρχεία στον φάκελο "+repoFolfer.getAbsolutePath());
            return;
        }
        Arrays.sort(repoFiles, Comparator.comparing(File::getName));
        System.out.println("Βρέθηκαν "+repoFiles.length+" αρχεία jsonl στον φάκελο repository");

        FieldType fieldType = new FieldType();//δημιουργία αντικειμένου για που θα ρυθμίζει το πεδίο κειμένου
        fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);//αποθήκευση docIDs, frequencies & positions
        fieldType.setTokenized(true);
        fieldType.setStored(true);
        fieldType.freeze();

        Gson gson = new Gson();//χρήση Gson για ανάγνωση των jsonl αρχείων
        int indexCount = 0;

        try(Directory indexDir = FSDirectory.open(Paths.get(invertIndexDirectory))){//σύνδεση με το directory data/lucene_inverted_index
            Analyzer analyzer = new EnglishAnalyzer();//ορίζει τον τρόπο με τον οποίο θα αναλύεται το αγγλικό κείμενο πριν αποθηκευτεί
            IndexWriter writer = new IndexWriter(indexDir, new IndexWriterConfig(analyzer).setOpenMode(IndexWriterConfig.OpenMode.CREATE));
            for(File repoFile : repoFiles){//επανάληψη για όλα τα αρχεία στον φάκελο data/repository
                try(BufferedReader reader = Files.newBufferedReader(repoFile.toPath(), StandardCharsets.UTF_8)){
                    String line;
                    while((line = reader.readLine()) != null){//διάβασμα μέχρι το τέλος του αρχείου
                        if(line.isBlank()) continue;//αγνοεί τυχαίες γραμμές
                        RepoDocument repoDoc = gson.fromJson(line, RepoDocument.class);//μετατροπή της γραμμής που είναι se json format σε αντικείμενο RepoDocument
                        Document lucenedoc = new Document();//δημιουργία αντικειμένου lucene

                        lucenedoc.add(new StringField("id",repoDoc.getId(), Field.Store.YES));//προσθήκη του id

                        lucenedoc.add(new StringField("filename",repoDoc.getSource(), Field.Store.YES));//προσθήκη του ονόματος του αρχείου

                        String text = cleanText(repoDoc.getText());//καθαρισμός του κειμένου από τεράστια tokens (>100 χαρακτήρες) και αλλαγές γραμμής \r\n
                        lucenedoc.add(new TextField("content",text, Field.Store.YES));//προσθήκη του κειμένου του αρχείου

                        writer.addDocument(lucenedoc);//αποστολή στον lucene IndexWriter για την κατασκευή του αντεστραμμένου ευρετηρίου
                        indexCount+=1;

                        if(indexCount%1000==0){//εμφάνιση προόδου για κάθε 1000 εγγραφά το οποίο δεν χρειάζεται μιας και είναι 4
                            System.out.println("Ευρετηριάστηκαν "+indexCount+" έγγραφα...");
                        }
                    }
                }
            }
            writer.commit();//οριστική αποθήκευση αλλαγών
        }catch (IOException e){
            e.printStackTrace();
        }

        /*
        Βρέθηκαν 4 αρχεία jsonl στον φάκελο repository
        Nov 22, 2026 5:43:23 PM org.apache.lucene.internal.vectorization.VectorizationProvider lookup
        WARNING: Java vector incubator module is not readable. For optimal vector performance, pass '--add-modules jdk.incubator.vector' to enable Vector API.
        Ευρετηριάστηκαν 1000 έγγραφα...
        Ευρετηριάστηκαν 2000 έγγραφα...
        Ευρετηριάστηκαν 3000 έγγραφα...
        Ευρετηριάστηκαν 4000 έγγραφα...
        Ευρετηριάστηκαν 5000 έγγραφα...
        Ευρετηριάστηκαν 6000 έγγραφα...
        Ευρετηριάστηκαν 7000 έγγραφα...
        Ευρετηριάστηκαν 8000 έγγραφα...
        Ευρετηριάστηκαν 9000 έγγραφα...
        Ευρετηριάστηκαν 10000 έγγραφα...
        ==========================================
        Ολοκληρώθηκε το App 2 (Lucene Indexer)!
        Συνολικά έγγραφα στο ευρετήριο: 10000
        Χρόνος εκτέλεσης: 73 δευτερόλεπτα
        Το ευρετήριο αποθηκεύτηκε στον φάκελο: data/lucene_inverted_index
        */
    }

    private static String cleanText(String rawText) {//καθαρισμός του κειμένου από τεράστια tokens (>100 χαρακτήρες) και αλλαγές γραμμής \r\n
        if (rawText == null || rawText.isEmpty()) return ""; // αν είναι κενό, επέστρεψε κενό string
        // αντικαθιστά \r, \n, tabs και πολλαπλά κενά με ένα απλό κενό
        String normalized = rawText.replaceAll("\\s+", " ");
        StringBuilder sb = new StringBuilder(normalized.length()); // Buffer για το καθαρό κείμενο
        for (String token : normalized.split(" ")) { // Χωρίζει το κείμενο σε λέξεις βάσει κενού
            if (token.length() <= 100) { // κρατά μόνο λέξεις φυσιολογικού μήκους (<=100 χαρακτήρες)
                sb.append(token).append(' '); // προσθέτει τη λέξη και ένα κενό
            }
        }
        return sb.toString().trim(); // Επιστρέφει το τελικό καθαρό κείμενο
    }
}