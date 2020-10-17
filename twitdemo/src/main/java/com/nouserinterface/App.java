package com.nouserinterface;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.nouserinterface.entities.Tweet;
import com.sun.istack.internal.NotNull;
import org.apache.commons.cli.*;
import twitter4j.*;
import org.apache.commons.lang3.StringUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Array;
import java.util.*;

public class App {

    private static Twitter twitter = new TwitterFactory().getInstance();
    private static Gson gson = new Gson();

    public static Map<Long, String> getUserTimeline(String username, long offset, int max, long wait) throws InterruptedException {
        Map<Long, String> ans = new HashMap<Long, String>();
        Paging paging = new Paging(1, max);
        if (offset != 0)
            paging.setSinceId(offset);
        do {
            try {
                List<Status> temp = twitter.getUserTimeline(username, paging);
                System.err.println("Updating: " + username + " Added: " + temp.size() + " Registries");
                for (Status s : temp)
                    ans.put(s.getId(), gson.toJson(s));
                if (temp.size() == 0 || temp.size() < max)
                    break;
                else
                    paging.setMaxId(temp.get(temp.size() - 1).getId());
                System.err.println(username + "(" + String.valueOf(temp.size()) + "/" + String.valueOf(ans.size()) + ")");
            } catch (TwitterException e) {
                if (e.getErrorCode() == 88){ //Limit exceeded
                    int waitingSeconds = e.getRateLimitStatus().getSecondsUntilReset()*1000;
                    System.err.println("Limit exceeded, waiting " + waitingSeconds/1000 + " Seconds");
                    Thread.sleep(waitingSeconds);
                    System.err.println("Restarting...");
                    twitter = new TwitterFactory().getInstance();
                }
                e.getMessage();
            }
        } while (true);

        return ans;
    }

    public  static void etlToDataBase(@NotNull String sFolder){
        File folder = new File(sFolder);
        File[] listOfFiles = folder.listFiles();
        if(listOfFiles != null)
            for (File file :  listOfFiles) {
                ArrayList<Tweet> tweetsOfFile = extract(file);
                ArrayList<Tweet> fixedTweets = tranform(tweetsOfFile);
                if(load(fixedTweets)){
                    System.out.println(fixedTweets.size() + " Tweets loaded to the database of " + file.getName());
                }
            }
    }

    private static boolean load(ArrayList<Tweet> fixedTweets) {
        /*Database BD = new Database("BDTwitter","jdbc:sqlserver://localhost:1433;databaseName=BDTwitter;integratedSecurity=true");

            fixedTweets.forEach((tweet -> {
                try {
                    BD.runSql("INSERT INTO [dbo].[Tweet] ([tweet]) VALUES ('" + gson.toJson(tweet) + "')");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } ));*/
        return true;
    }

    private static ArrayList<Tweet> tranform(ArrayList<Tweet> tweetsOfFile) {
        // 1: If reTweeted replace text for original text
        tweetsOfFile.forEach( (tweet) ->  {tweet.text = tweet.retweetedStatus != null ? tweet.retweetedStatus.text: tweet.text;});
        // 2: Count Tweets by profile
        int numeberOfTweets = tweetsOfFile.size();
        tweetsOfFile.forEach( (tweet) ->  {tweet.tweetsProfile = numeberOfTweets;});
        // 3: Count HashTags
        tweetsOfFile.forEach( (tweet) ->  {tweet.hashTagsUsed = tweet.hashtagEntities != null ? tweet.hashtagEntities.length : 0;});
        // 4: Normalize accents
        tweetsOfFile.forEach( (tweet) ->  {tweet.text = StringUtils.stripAccents(tweet.text);});
        // 5: Remove special characters
        tweetsOfFile.forEach( (tweet) ->  {tweet.text = tweet.text.replaceAll("[^a-zA-Z0-9\\s]", " ");});
        return tweetsOfFile;
    }

    private static ArrayList<Tweet> extract(File sFile){
        ArrayList<Tweet> tweets = new ArrayList<Tweet>();
        BufferedReader reader;
        try {
            if (sFile.isFile()) {
                reader = new BufferedReader(new FileReader(sFile));
                String line = reader.readLine();
                do {
                    try {
                        line = line.substring(line.indexOf("{"));
                        Tweet tweet = gson.fromJson(line, Tweet.class);
                        tweets.add(tweet);
                    } catch (JsonParseException ex) {
                        System.err.println("{Error in file: " + sFile.getName() + "}, {line: "+ line + "}, {exception: " + ex.getMessage() + "}");
                    } finally {
                        line = reader.readLine();
                    }
                }
                while (line != null);
                reader.close();
            }
        }
        catch (Exception ex){
            ex.printStackTrace();
        }

        return tweets;
    }

    public static void search(String txt, int max, long wait) {

        int total = 0;
        try {
            Query query = new Query(txt);
            query.setResultType(Query.RECENT);
            query.setLang("ES");
            query.setCount(200);
            QueryResult result;
            do {
                result = twitter.search(query);
                List<Status> tweets = result.getTweets();
                for (Status tweet : tweets) {
                    System.err.println(tweet.getId() + " " +gson.toJson(tweet));
                    System.out.println(tweet.getUser().getScreenName());
                    total++;
                }
                Thread.sleep(wait);
                if (total > max)
                    break;
            } while ((query = result.nextQuery()) != null);
        } catch (TwitterException te) {
            te.printStackTrace();
            System.out.println("Failed to search tweets: " + te.getMessage());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        String username = null;
        String query = null;
        String folder = null;
        long offset = 0;
        int max = 20;
        long wait = 2000;

        Options options = new Options();
        Option usernameOption = Option.builder("u").hasArg(true).longOpt("username").build();
        options.addOption(usernameOption);
        Option offsetOption = Option.builder("o").hasArg(true).longOpt("offset").build();
        options.addOption(offsetOption);
        Option maxOption = Option.builder("m").hasArg(true).longOpt("max").build();
        options.addOption(maxOption);
        Option waitOption = Option.builder("w").hasArg(true).longOpt("wait").build();
        options.addOption(waitOption);
        Option queryOption = Option.builder("q").hasArg(true).longOpt("query").build();
        options.addOption(queryOption);
        Option loadDataBase = Option.builder("l").hasArg(true).longOpt("load").build();
        options.addOption(loadDataBase);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        try {
            CommandLine line = parser.parse(options, args);
            if (line.hasOption("u"))
                username = line.getOptionValue("u");
            if (line.hasOption("o"))
                offset = Long.parseLong(line.getOptionValue("o"));
            if (line.hasOption("m"))
                max = Integer.parseInt(line.getOptionValue("m"));
            if (line.hasOption("w"))
                wait = Long.parseLong(line.getOptionValue("w"));
            if (line.hasOption("q"))
                query = line.getOptionValue("q");
            if (line.hasOption("l"))
                folder = line.getOptionValue("l");
        } catch (ParseException exp) {
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
            formatter.printHelp("utility-name", options);
            System.exit(1);
        }

        if (username != null)
            for (Map.Entry<Long, String> s : getUserTimeline(username, offset, max, wait).entrySet())
                System.out.println(s.getKey().toString() + " " + s.getValue());
        else if (query != null)
            search(query, max, wait);
        else if (folder != null) {
            etlToDataBase(folder);
        }
    }
}
