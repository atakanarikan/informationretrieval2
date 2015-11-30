import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Created by aarikan on 30/11/15.
 */
public class Main {
    static ArrayList<HashMap<String, Integer>> legitimateMails = new ArrayList<>();
    static ArrayList<HashMap<String, Integer>> spamMails = new ArrayList<>();
    static HashMap<String, Integer> legitVoc = new HashMap<>();
    static HashMap<String, Integer> spamVoc = new HashMap<>();

    public static void main(String[] args) throws IOException {
       // System.out.print(legitimateMails.get(0));
        run();
        System.out.println(spamVoc.size());
        System.out.println(legitVoc.size());
    }


    public static void readInput(String filepath, ArrayList storage, String type) throws IOException {
        // read all the files in a folder
        Files.walk(Paths.get(filepath)).forEach(filePath -> {
            if (Files.isRegularFile(filePath) && filePath.toString().endsWith("txt")) {
                HashMap<String, Integer> tempStorage = new HashMap<>();
                try {
                    String content = new Scanner(new File(filePath.toString())).useDelimiter("\\Z").next();
                    String[] tokens = tokenize(content).split(" ");
                    for(int i = 0; i < tokens.length; i++) {
                        if(tokens[i].length() > 0) { // handle the empty string
                            if(type.equals("spam")){ // we're processing spam mails, fill spam vocabulary
                                if (spamVoc.containsKey(tokens[i])) {
                                    int freq = spamVoc.get(tokens[i]);
                                    spamVoc.put(tokens[i], freq + 1);
                                } else {
                                    spamVoc.put(tokens[i], 1);
                                }
                            } else if(type.equals("legit")) { // we're processing legitimate mails, fill legitimate vocabulary
                                if (legitVoc.containsKey(tokens[i])) {
                                    int freq = legitVoc.get(tokens[i]);
                                    legitVoc.put(tokens[i], freq + 1);
                                } else {
                                    legitVoc.put(tokens[i], 1);
                                }                            }
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

    public static void kNN(String query, int k) {
        //TODO IMPLEMENT THIS MOTHERFUCKAAAA
        System.out.println("Running the kNN algorithm for k = " + k + "...");
    }

    public static void run() throws IOException {
        System.out.println("Hello, world!");
        System.out.print("Reading the legitimate data... ");
        readInput("dataset/training/legitimate", legitimateMails, "legit");
        System.out.println("Done!");
        System.out.print("Reading the spam data... ");
        readInput("dataset/training/spam", spamMails, "spam");
        System.out.println("Done!");
        Scanner scan = new Scanner(System.in);
        String userQuery;
        int k;
        while(true){
            System.out.print("Type \'q\' to exit the program, or press enter:");
            userQuery = scan.nextLine();
            if (userQuery.equals("q")) {
                scan.close();
                System.out.println("Goodbye, Cruel World!");
                System.exit(0);
            }
            System.out.print("Please enter the k value(Default is 1): ");
            userQuery = scan.nextLine();
            if(userQuery.equals("")) {
                k = 1;
            } else {
                k = Integer.parseInt(userQuery);
            }
            kNN(userQuery, k);
            //TODO Rocchio();
        }
    }
}
