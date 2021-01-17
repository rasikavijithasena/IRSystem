package jassjr;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class RemoveStopWords {
    private List<String> stopwords;

    public static void main(String[] args) throws Exception {

    }

    public boolean checkStopWord(String token) throws IOException{
        String word = token.toLowerCase();
        stopwords = Files.readAllLines(Paths.get("english_stopwords.txt"));
        if(! stopwords.contains(word)) {
           return true;
        }
        return false;
    }

}
