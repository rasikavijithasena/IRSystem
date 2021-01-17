package jassjr;

public class PorterStemmerWithRules {
    public String stemming(String word){
        String afterStemming = word;
        word = word.toLowerCase();
        if(word.endsWith("ies")){
            afterStemming = word.substring(0, word.length()-2);
        } else if(word.endsWith("sses")){
            afterStemming = word.substring(0, word.length()-2);
        } else if(word.endsWith("ss")){
            afterStemming = word;
        } else if(word.endsWith("s")){
            afterStemming = word.substring(0, word.length()-1);
        }
        return afterStemming;

    }
}
