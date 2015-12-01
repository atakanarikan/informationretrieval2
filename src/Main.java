import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by aarikan on 30/11/15.
 */
public class Main {
    static ArrayList<HashMap<String, Integer>> legitimateMails = new ArrayList<>();
    static ArrayList<HashMap<String, Integer>> spamMails = new ArrayList<>();
    static HashMap<String, Integer> vocabulary = new HashMap<>();
    static HashMap<String, Integer> queryMail = new HashMap<>();
    static HashMap<String, Double> idf = new HashMap<>();
    static ArrayList<HashMap<String, Double>> tfidfLegit = new ArrayList<>();
    static ArrayList<HashMap<String, Double>> tfidfSpam = new ArrayList<>();
    static HashMap<String, Double> tfidfQuery = new HashMap<>();
    static HashMap<String, Double> spamCentroid = new HashMap<>();
    static HashMap<String, Double> legitCentroid = new HashMap<>();


    public static void main(String[] args) throws IOException {
        run();
    }

    /*
    prepares the given query mail to be compatible to the calculateTFIDF method.
    then calls calculateTFIDF(queryMail, "query"); which fills tfidfQuery Map
     */
    public static void processQuery(String filePath) throws IOException {
        String content = new Scanner(new File(filePath.toString())).useDelimiter("\\Z").next(); // read the mail
        String[] tokens = tokenize(content).split(" ");
        for(int i = 0; i < tokens.length; i++) {
            if(tokens[i].length() > 0) { // handle the empty string
                if (queryMail.containsKey(tokens[i])) {
                    int freq = queryMail.get(tokens[i]);
                    queryMail.put(tokens[i], freq + 1);
                } else {
                    queryMail.put(tokens[i], 1);
                }
            }
        }
        calculateTFIDF(queryMail, "query");
    }

    /*
    reads the input from the files and fills legitimateMails, spamMails & vocabulary
     */
    public static void readInput(String filepath, ArrayList storage) throws IOException {
        // read all the files in given folder
        Files.walk(Paths.get(filepath)).forEach(filePath -> {
            if (Files.isRegularFile(filePath) && filePath.toString().endsWith("txt")) {
                HashMap<String, Integer> tempStorage = new HashMap<>();
                try {
                    String content = new Scanner(new File(filePath.toString())).useDelimiter("\\Z").next();
                    String[] tokens = tokenize(content).split(" ");
                    for (int i = 0; i < tokens.length; i++) {
                        if (tokens[i].length() > 0) { // handle the empty string
                            if (vocabulary.containsKey(tokens[i])) {
                                int freq = vocabulary.get(tokens[i]);
                                if (!tempStorage.keySet().contains(tokens[i])) { // if we counted this word already for this doc, don't do anything
                                    vocabulary.put(tokens[i], freq + 1);
                                }
                            } else {
                                vocabulary.put(tokens[i], 1);
                            }
                            if (tempStorage.containsKey(tokens[i])) {
                                int freq = tempStorage.get(tokens[i]);
                                tempStorage.put(tokens[i], freq + 1);
                            } else {
                                tempStorage.put(tokens[i], 1);
                            }
                        }
                    }
                    storage.add(tempStorage);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /*
    takes the input, makes all lowercased.
    gets rid of the words containing all nonword characters.
    gets rid of the nonword characters at the beginning of a word.
    gets rid of the nonword characters at the end of a word.
    gets rid of any nonword character in a word (excluding digits) // 19.2 or 16,3 will pass, whereas "It's okay" will be "its okay"
     */
    public static String tokenize(String line) {
        String[] prettyTokens = line.substring(9).toLowerCase().split(" "); // split the lowercase string by whitespace and avoid the "Subject: " in the beginning
        StringBuilder bigBuilder = new StringBuilder();
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < prettyTokens.length; i++) {
            String active = prettyTokens[i];
            if (active.length() > 0) { // parseDouble thinks "1980." a double. avoid that.
                if (!Character.isDigit(active.charAt(active.length() - 1)) && !Character.isLetter(active.charAt(active.length() - 1))) {
                    active = active.substring(0, active.length() - 1);
                }
            }
            try {
                double x = Double.parseDouble(active);
                bigBuilder.append(active);
                bigBuilder.append(" ");
            } catch (Exception e) {  // so this is not a double value
                String activeTemp = active.replace(',', '.'); //try again
                try {
                    double x = Double.parseDouble(activeTemp);
                    bigBuilder.append(activeTemp);
                    bigBuilder.append(" ");

                } catch (Exception e1) {
                    if (active.contains("&lt;")) { // handle words including this
                        active = active.substring(0, active.indexOf("&lt;")) + active.substring(active.indexOf("&lt;") + 3);
                    }
                    for (int j = 0; j < active.length(); j++) {
                        if (Character.isDigit(active.charAt(j)) || Character.isLetter(active.charAt(j))) {
                            stringBuilder.append(active.charAt(j));
                        }
                    }
                    bigBuilder.append(stringBuilder.toString());
                    bigBuilder.append(" ");
                    stringBuilder.setLength(0);
                }
            }
        }
        return bigBuilder.toString();

    }

    /*
    using the vocabulary and the document frequency, fills the idf map with (word, idf_value) tuples.
     */
    public static void doIDF() {
        Iterator itr = vocabulary.keySet().iterator();
        while(itr.hasNext()) {
            String key = (String)itr.next();
            int docFreq = vocabulary.get(key);
            idf.put(key, Math.log10(480 / docFreq));
        }
    }

    /*
    calculates the TFIDF values for all mails in the given type: spam or legitimate
     */
    public static void calculateTFIDFforAll(ArrayList<HashMap<String, Integer>> mailSet, String type){
        mailSet.forEach(mail -> { // for all the mails in the spam/legit folder
            calculateTFIDF(mail, type);
        });
    }

    /*
    according to the given type, fills either tfidfSpam, tfidfLegit or tfidfMap
    tf is read from input parameter, idf is read from global idf map.
     */
    public static void calculateTFIDF(HashMap<String, Integer> input, String type){
        HashMap<String, Double> tfidfForCurrentDoc = new HashMap<>();
        Iterator itr = vocabulary.keySet().iterator(); // for all the words in our vocabulary
        while(itr.hasNext()) {
            String wordKey = (String) itr.next();
            if(input.keySet().contains(wordKey)){
                tfidfForCurrentDoc.put(wordKey, (1.0 + Math.log10(input.get(wordKey))) * idf.get(wordKey));
            } else {
                tfidfForCurrentDoc.put(wordKey, 0.0);
            }
        }
        if(type.equals("spam")) {
            tfidfSpam.add(tfidfForCurrentDoc);
        }
        else if(type.equals("legit")) {
            tfidfLegit.add(tfidfForCurrentDoc);
        }
        else if(type.equals("query")) {
            tfidfQuery = tfidfForCurrentDoc;
        }
    }

    /*
    returns the normalized cosine similarity between given inputs
     */
    public static double cosineSimilarity(HashMap<String, Double> query, HashMap<String, Double> mail){
        double lengthQuery = 0;
        double lengthMail = 0;
        double dividend = 0;
        if(query.size() != mail.size()) {
            System.out.println("Their vectors spaces do not have the same size!");
            return -1;
        }
        Iterator itrQuery = query.keySet().iterator();
        Iterator itrMail = mail.keySet().iterator();
        while(itrMail.hasNext()){ // since their sizes are equal we dont have to check twice
            double queryElement = query.get(itrQuery.next());
            double mailElement = mail.get(itrMail.next());
            dividend += queryElement * mailElement;
            lengthMail += mailElement * mailElement;
            lengthQuery += queryElement * queryElement;
        }
        return dividend/(Math.sqrt(lengthMail)*Math.sqrt(lengthQuery));
    }

    /*
    calculates cosine similarity between each of the spam mails and given input in a loop.
    returns the normalized result.
     */
    public static ArrayList<Double> cosineSimilaritiesSpam(){
        ArrayList<Double> results = new ArrayList<>();
        tfidfSpam.forEach(mail -> {
            results.add(cosineSimilarity(tfidfQuery, mail));
        });
        return results;
    }

    /*
    calculates cosine similarity between each of the legitimate mails and given input in a loop.
    returns the normalized result.
     */
    public static ArrayList<Double> cosineSimilaritiesLegitimate(){
        ArrayList<Double> results = new ArrayList<>();
        tfidfLegit.forEach(mail -> {
            results.add(cosineSimilarity(tfidfQuery, mail));
        });
        return results;
    }

    /*
    gets k from user as an input.
    Selects the closest k vectors from either spam space or legitimate space
    replies with a resulting string.
     */
    public static void kNN(int k) {
        System.out.println("Running the kNN algorithm for k = " + k + "...");
        ArrayList <Double> spamResults = cosineSimilaritiesSpam();
        ArrayList <Double> legitResults = cosineSimilaritiesLegitimate();
        Collections.sort(spamResults);
        Collections.sort(legitResults);
        int spamIndex = spamResults.size() - 1;
        int spamCount = 0;
        int legitIndex = legitResults.size() - 1;
        int legitCount = 0;
        for(int i = 0; i < k; i++) {
            if(spamResults.get(spamIndex) > legitResults.get(legitIndex)){
                spamIndex--;
                spamCount++;
            } else {
                legitIndex--;
                legitCount++;
            }
        }
        if(legitCount > spamCount) {
            System.out.println("It's labeled as LEGITIMATE with " + legitCount + " legitimate, " + spamCount + " spam score.");
        } else {
            System.out.println("It's labeled as SPAM with " + legitCount + " legitimate, " + spamCount + " spam score.");
        }
    }

    /*
    calculates the centroid of the given mail list.
    basically, iterates over each dimension, adds them up, divides them with the size of the list.
     */
    public static void calculateCentroid(ArrayList<HashMap<String, Double>> mailList, HashMap<String, Double> givenCentroid){
        for (String key : vocabulary.keySet()) {
            double tempResult = 0;
            for(int j = 0; j < mailList.size(); j++) {
                tempResult += mailList.get(j).get(key);
            }
            givenCentroid.put(key, tempResult/(double)mailList.size());
        }
    }

    /*
    calculates the cosine similarity to both spam and legit centroids.
    decides upon the result and replies with a resulting string.
     */
    public static void Rocchio(){
        double spamResult = cosineSimilarity(tfidfQuery, spamCentroid);
        double legitResult = cosineSimilarity(tfidfQuery, legitCentroid);
        if(legitResult > spamResult) {
            System.out.println("It's labeled as LEGITIMATE with score of " + legitResult + "  (" + spamResult + " spam score)");
        }
        else {
            System.out.println("It's labeled as SPAM with score of " + spamResult + "  (" + legitResult + " legitimate score)");
        }
    }

    public static void test(String type) throws IOException {
        Files.walk(Paths.get("dataset/test/" + type)).forEach(filePath -> {
            if (Files.isRegularFile(filePath) && filePath.toString().endsWith("txt")) {
                try {
                    processQuery(filePath.toString());
                    kNN(199);
                    Rocchio();
                    System.out.println("*****************************************************");
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    public static void calculatetotaltfidfspam() {
        HashMap<String, Double> tfidfSpamTotal = new HashMap<>();
        for (HashMap<String, Double> map : tfidfSpam) {
            Iterator itr = map.keySet().iterator();
            while (itr.hasNext()) {
                String key = (String) itr.next();
                if (tfidfSpamTotal.keySet().contains(key)) {
                    double x = tfidfSpamTotal.get(key);
                    tfidfSpamTotal.put(key, x + map.get(key));
                } else {
                    tfidfSpamTotal.put(key, map.get(key));
                }
            }
        }
        ArrayList <String> top = new ArrayList<>(20);
        for(int i = 0; i<20; i++){
            double max = 0;
            String maxKey = "";
            Iterator it = tfidfSpamTotal.keySet().iterator();
            while (it.hasNext()) {
                String key = (String) it.next();
                if(tfidfSpamTotal.get(key) > max) {
                    max = tfidfSpamTotal.get(key);
                    maxKey = key;
                }
            }
            top.add(i, maxKey);
            System.out.println(maxKey + " -> " + max);
            tfidfSpamTotal.remove(maxKey);
        }
    }
    /*
    just runs the process. Calls all the methods and has a infinite loop.
     */
    public static void run() throws IOException {
        System.out.println("Hello, world!");
        System.out.print("Reading the legitimate data... ");
        readInput("dataset/training/legitimate", legitimateMails);
        System.out.println("Done!");
        System.out.print("Reading the spam data... ");
        readInput("dataset/training/spam", spamMails);
        System.out.println("Done!");
        System.out.print("Calculating the idf values for whole vocabulary... ");
        doIDF();
        System.out.println("Done!");
        System.out.print("Calculating the tf-idf values for legitimate mails... ");
        calculateTFIDFforAll(legitimateMails, "legit");
        System.out.println("Done!");
        System.out.print("Calculating the tf-idf values for spam mails... ");
        calculateTFIDFforAll(spamMails, "spam");
        System.out.println("Done!");
        System.out.print("Calculating the centroid of legitimate mails... ");
        calculateCentroid(tfidfLegit, legitCentroid);
        System.out.println("Done!");
        System.out.print("Calculating the centroid of legitimate mails... ");
        calculateCentroid(tfidfSpam, spamCentroid);
        System.out.println("Done!");
        //test("spam");
        Scanner scan = new Scanner(System.in);
        String userQuery;
        int k;
        while(true){
            System.out.print("Please enter the k value(Default is 1): ");
            userQuery = scan.nextLine();
            if (userQuery.equals("q")) {
                scan.close();
                System.out.println("Goodbye, Cruel World!");
                System.exit(0);
            }
            if(userQuery.equals("")) {
                k = 1;
            } else {
                k = Integer.parseInt(userQuery);
            }
            System.out.print("Please enter your query: ");
            userQuery = scan.nextLine();
            processQuery(userQuery);
            kNN(k);
            Rocchio();
            System.out.println("*****************************************************");
        }
    }
}
