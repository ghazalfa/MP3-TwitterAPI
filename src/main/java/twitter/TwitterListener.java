package twitter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.redouane59.twitter.TwitterClient;
import io.github.redouane59.twitter.dto.endpoints.AdditionalParameters;
import io.github.redouane59.twitter.dto.tweet.TweetList;
import io.github.redouane59.twitter.dto.tweet.TweetV2;
import io.github.redouane59.twitter.dto.user.User;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

// TODO: write a description for this class
// TODO: complete all methods, irrespective of whether there is an explicit TODO or not
// TODO: write clear specs
// TODO: State the rep invariant and abstraction function
// TODO: what is the thread safety argument?
public class TwitterListener {

    TwitterClient twitter;
    private static final LocalDateTime OCT_1_2022 = LocalDateTime.parse("2022-10-01T00:00:00");
    private static LocalDateTime LastFetchedTime;
    Map<String, List<String>> subscriptions;

    // create a new instance of TwitterListener
    // the credentialsFile is a JSON file that
    // contains the API access keys
    // consider placing this file in the
    // 'secret' directory but the constructor
    // should work with any path
    public TwitterListener(File credentialsFile) {
        twitter = new TwitterClient(TwitterClient.getAuthentication(credentialsFile));
        TwitterListener tl = new TwitterListener(credentialsFile);
        LastFetchedTime = OCT_1_2022;
        subscriptions = new HashMap<>();
    }

    // add a subscription for all tweets made by a specific. If already contains return false
    // Twitter user
    public boolean addSubscription(String twitterUserName) {
        if (!isValidUser(twitterUserName) || subscriptions.containsKey(twitterUserName)) {
            return false;
        }
        subscriptions.put(twitterUserName, new ArrayList<>());
        return true;
    }

    private boolean isValidUser(String twitterUserName) {
            return false;
    }


    // add a subscription for all tweets made by a specific
    // Twitter user that also match a given pattern
    // for simplicity, a match is an exact match of strings but
    // ignoring case
    public boolean addSubscription(String twitterUserName, String pattern) {
        if (!isValidUser(twitterUserName)) {
            return false;
        }
        String addPattern = pattern.toLowerCase();

        if(subscriptions.containsKey(twitterUserName)){
            subscriptions.get(twitterUserName).add(addPattern);
        } else {
            List<String> list = new ArrayList<>();
            list.add(pattern);
            subscriptions.put(twitterUserName, list);
        }
        return true;
    }

    // cancel a previous subscription
    // will also cancel subscriptions to specific patterns
    // from the twitter user, false if not subscribed
    public boolean cancelSubscription(String twitterUserName) {
        if (!subscriptions.containsKey(twitterUserName)) {
            return false;
        }
        subscriptions.remove(twitterUserName);

        return true;
    }

    // cancel a specific user-pattern subscription, false if pattern or username not present
    public boolean cancelSubscription(String twitterUserName, String pattern) {
        if (!subscriptions.containsKey(twitterUserName)) {
            return false;
        }

        String deletePattern = pattern.toLowerCase();
        List<String> patterns = subscriptions.get(twitterUserName);

        //check if pattern is present, if so remove
        if (!patterns.remove(deletePattern)) return false;

        subscriptions.put(twitterUserName, patterns);

        return true;
    }

    // get all subscribed tweets since the last tweet or
    // set of tweets was obtained
    public List<TweetV2.TweetData> getRecentTweets() {
    return null;
    }

    // get all the tweets made by a user
    // within a time range.
    // method has been implemented to help you.
    public List<TweetV2.TweetData> getTweetsByUser(String twitterUserName,
                                                   LocalDateTime startTime,
                                                   LocalDateTime endTime) {
        User twUser = twitter.getUserFromUserName(twitterUserName);
        if (twUser == null) {
            throw new IllegalArgumentException();
        }
        TweetList twList = twitter.getUserTimeline(twUser.getId(), AdditionalParameters.builder().startTime(startTime).endTime(endTime).build());
        return twList.getData();
    }

    public static void main(String[] args) {
        TwitterListener tl = new TwitterListener(new File("secret/credentials.json"));
        List<TweetV2.TweetData> tweets = tl.getTweetsByUser("ubc", TwitterListener.OCT_1_2022, LocalDateTime.now());

        System.out.println(tweets.size());

        TweetV2.TweetData tweet = tweets.get(0);

        System.out.println(tweet.getText());
    }

}
