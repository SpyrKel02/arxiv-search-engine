package app1_store;

import com.google.gson.Gson;
import common.RepoDocument;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;


public class StoredModule {

    private static final String pdfsDirectory = "data/input_pdfs/arxiv_papers";
    private static final String repoDirectory = "data/repository";
    private static final long MAX_BYTES = 128L * 1024 * 1024;//μέγιστο επιτρεπτό μέγεθος ανά αρχείο (128 bytes)

    public static void main(String[] args){
        File inputFolder = new File(pdfsDirectory);//δημιουργία αντικειμένου όπου αποθηκεύουμε το absolute path του arxiv_papers αρχείου
        File[] pdfFiles = inputFolder.listFiles((dir, name) -> name.endsWith(".pdf"));//αποθήκευση των αρχείων με κατάληξη pdf σε έναν πίνακα
        if(pdfFiles==null || pdfFiles.length==0){//έλεγχος αν το directory δεν περιλαμβάνει pdf αρχεία ή αν είναι άδειος
            System.out.println("Δεν βρέθηκαν pdf αρχεία");
            return;
        }
        Arrays.sort(pdfFiles, Comparator.comparing(File::getName));//ταξινόμηση αλφαβητικά
        System.out.println("Βρέθηκαν "+pdfFiles.length+" αρχεία PDF");

        //Αρχή Δημιουργίας Repository
        Gson gson = new Gson();//Βιβλιοθήκη της Java που μπορεί να χρησιμοποιηθεί για τη μετατροπή αντικειμένων Java στην αναπαράστασή τους σε μορφή JSON
        PDFTextStripper stripper = new PDFTextStripper();//αρχικοποίηση εξαγωγέα κειμένου

        BufferedWriter writer = null;//writer που γράφει στο τρέχον αρχείο του repository
        int fileIndex = 0;//μετρητής για την αρίθμηση των αρχείων εξόδου
        int processedPdfCount = 0;//πόσα pdf έχουν επεξεργαστεί επιτυχώς
        int errorPdfCount = 0;//πόσα pdf δεν επεξεργάστηκαν επιτυχώς και έβγαλαν πρόβλημα κατά την ανάγνωση
        int currentChunkBytes = 0;//πόσα bytes έχουν γραφτεί μέχρι στιγμής στο αρχείο των 128MB

        try{
            Files.createDirectories(Paths.get(repoDirectory));//δημιουργεί τον φάκελο data/repository αν δεν υπάρχει
            writer = createChunkWriter(fileIndex);

            for(File pdfFile: pdfFiles){
                try(PDDocument document = Loader.loadPDF(pdfFile)){
                    String rawText = stripper.getText(document);//εξαγωγή όλου του κειμένου από το pdf
                    String fileName = pdfFile.getName();//παίρνουμε το όνομα του αρχείου
                    String docId = fileName.replaceFirst("(?i)\\.pdf$","");//αφαιρούμε την κατάληξη pdf

                    RepoDocument repoDocument = new RepoDocument(docId, rawText, fileName);//αντικείμενο repoDocument με id, κείμενο και όνομα αρχείου
                    String jsonLine = gson.toJson(repoDocument)+"\n";//μετατροπή σε Json
                    byte[] lineBytes = jsonLine.getBytes(StandardCharsets.UTF_8);//υπολογίζει το ακριβό μέγεθος της γραμμής σε bytes

                    if(currentChunkBytes > 0 && (currentChunkBytes + lineBytes.length > MAX_BYTES )){
                        writer.close();
                        fileIndex++;
                        writer = createChunkWriter(fileIndex);
                        currentChunkBytes = 0;
                    }
                    writer.write(jsonLine);//εγγραφή της γραμμής json στο τρέχων αρχείο του repository
                    currentChunkBytes += lineBytes.length;
                    processedPdfCount++;
                    if(errorPdfCount % 500 == 0){//για κάθε 500 pdf αρχεία εκτυπώνει μήνυμα προόδου
                        System.out.printf("Επεξεργάστηκαν %d / %d PDF (Τρέχον αρχείο: repo_%03d.jsonl)...%n",
                                processedPdfCount, pdfFiles.length, fileIndex);
                    }
                }catch(Exception e){
                    errorPdfCount++;
                    System.out.print("Σφάλμα ανάγνωσης στο αρχείο "+pdfFile.getName()+": "+e.getMessage());
                }
            }
        }catch(IOException e){
            e.printStackTrace();
        }finally {
            if(writer!=null){
                try{
                    writer.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
        System.out.println("==========================================");
        System.out.println("Ολοκληρώθηκε το App 1 (Store Module)!");
        System.out.println("Επιτυχή έγγραφα: " + processedPdfCount);
        System.out.println("Αποτυχημένα/κατεστραμμένα PDF: " + errorPdfCount);
        System.out.println("Δημιουργήθηκαν " + (fileIndex + 1) + " αρχεία Repository στον φάκελο " + repoDirectory);

        /* Επεξεργάστηκαν 10000 / 10000 PDF (Τρέχον αρχείο: repo_003.jsonl)...
        ==========================================
        Ολοκληρώθηκε το App 1 (Store Module)!
        Επιτυχή έγγραφα: 10000
        Αποτυχημένα/κατεστραμμένα PDF: 0
        Δημιουργήθηκαν 4 αρχεία Repository στον φάκελο data/repository
        */
    }

    public static BufferedWriter createChunkWriter(int fileIndex) throws IOException{
        String chunkName = String.format("repo_%03d.jsonl", fileIndex);
        Path path = Paths.get(repoDirectory, chunkName);
        System.out.println("Δημιουργία νέου αρχείου Repository: "+chunkName);
        return Files.newBufferedWriter(path,StandardCharsets.UTF_8);
    }
}