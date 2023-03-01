package phemeservice;

import io.github.redouane59.twitter.dto.tweet.TweetV2;
import org.junit.jupiter.api.Test;
import twitter.TwitterListener;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class Task3A {

    @Test
    public void testFetchRecentTweets() {
        TwitterListener tl = new TwitterListener(new File("secret/credentials.json"));
        tl.addSubscription("UBC");
        List<TweetV2.TweetData> tweets = tl.getRecentTweets();
        assertTrue(tweets.size() > 0);
    }

    @Test
    public void testDoubleFetchRecentTweets() {
        TwitterListener tl = new TwitterListener(new File("secret/credentials.json"));
        tl.addSubscription("UBC");
        List<TweetV2.TweetData> tweets = tl.getRecentTweets();
        assertTrue(tweets.size() > 0);
        tweets = tl.getRecentTweets();
        assertTrue(tweets.size() == 0); // second time around, in quick succession, no tweet
    }

}
