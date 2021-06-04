import lombok.AllArgsConstructor;
import model.Word;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RelatedTerm {
    private static final Map<String, Object> map = new ConcurrentHashMap<>();

    public List<Word> readWordJsonList(String file) {
        List<Word> wordsList = new ArrayList<>();
        try {
            JSONObject jsonObject = (JSONObject) new JSONParser().parse(new FileReader(file));
            for (Object word : jsonObject.keySet()) {
                wordsList.add(new Word(word.toString(), jsonObject.get(word).toString()));
            }
            return wordsList;
        } catch (Exception e) {
            return null;
        }
    }

    public JSONArray getRelatedTerms(List<Word> words, int threadNum) {
        if (words != null) {
            int idx = 0;
            int size = words.size();
            while (idx < size) {
                ArrayList<Thread> threads = new ArrayList<>();
                int count = 0;
                while (idx + count < size && (count < threadNum)) {
                    Word word = words.get(idx + count);
                    Thread thread = new RelatedTermThread(word);
                    thread.start();
                    threads.add(thread);
                    count++;
                }
                idx += threadNum;
                for (Thread thread : threads) {
                    try {
                        thread.join();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                System.out.println(getProcessPercent(size, idx) + "%...");
            }
            return wordListToJSON(map);
        } else {
            System.out.println("입력 파일을 확인하십시오.");
            return null;
        }
    }

    public int getProcessPercent(int size, int idx) {
        return Math.min(idx * 100 / size, 100);
    }

    public JSONArray wordListToJSON(Map<String, Object> map) {
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(map);
        return jsonArray;
    }

    public boolean jsonToFile(JSONArray jsonArray, String file) {
        try (FileWriter fw = new FileWriter(file)) {
            String jsonStr;
            try {
                jsonStr = jsonArray.toJSONString();
            } catch (Exception e) {
                return false;
            }
            fw.write(jsonStr);
            fw.flush();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static String encodeUTF8(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    @AllArgsConstructor
    private static class RelatedTermThread extends Thread {
        Word word;
        final Map<String, Object> wordInfoMap = new HashMap<>();

        public void run() {
            getRelatedTerm();
        }

        private void getRelatedTerm() {
            HttpURLConnection conn;
            try {
                URL url = new URL("https://trends.google.com/trends/api/autocomplete/" + encodeUTF8(word.getWord()) + "?hl=ko");
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestMethod("GET");
                int responseCode;
                try {
                    responseCode = conn.getResponseCode();
                } catch (Exception e) {
                    getRelatedTerm();
                    return;
                }
                if ((responseCode /= 100) == 4 || responseCode == 500) {
                    getRelatedTerm();
                } else {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    JSONObject jsonObject = (JSONObject) new JSONParser().parse(sb.substring(5));
                    JSONArray relatedTermArr = new JSONArray();
                    JSONArray topics = (JSONArray) ((JSONObject) jsonObject.get("default")).get("topics");
                    for (Object topicObj : topics) {
                        JSONObject topic = (JSONObject) topicObj;
                        Map<String, String> topicMap = new HashMap<>();
                        topicMap.put("mid", topic.get("mid").toString());
                        topicMap.put("title", topic.get("title").toString());
                        topicMap.put("type", topic.get("type").toString());
                        relatedTermArr.add(topicMap);
                    }
                    wordInfoMap.put("tag", word.getTag());
                    wordInfoMap.put("relatedTerm", relatedTermArr);
                    map.put(word.getWord(), wordInfoMap);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
