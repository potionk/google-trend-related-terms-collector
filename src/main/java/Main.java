import model.Word;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        String inputFile = "src/main/resources/word.json";
        String outputFile = "google-trend-synonym.json";
        int threadNum = 1000;

        RelatedTerm relatedTerm = new RelatedTerm();
        List<Word> words = relatedTerm.readWordJsonList(inputFile);
        if (relatedTerm.jsonToFile(relatedTerm.getRelatedTerms(words, threadNum), outputFile)) {
            System.out.println("저장에 성공하였습니다.");
        } else {
            System.out.println("저장에 실패하였습니다.");
        }
    }
}