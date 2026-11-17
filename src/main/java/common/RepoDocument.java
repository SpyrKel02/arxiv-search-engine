package common;

public class RepoDocument {

    private String id;//το id του άρθρου χωρίς την κατάληξη pdf πχ 0704.0001v2
    private String text;//το κείμενο που υπάρχει μέσα στο άρθρο
    private String source;//το όνομα του αρχείου με την κατάληξη pdf πχ 0704.0001v2.pdf

    public RepoDocument(String id, String text, String source) {
        this.id = id;
        this.text = text;
        this.source = source;
    }

    public String getId() {return this.id;}
    public String getText() {return this.text;}
    public String getSource() {return this.source;}
}