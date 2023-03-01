package main.java.pheme;

import timedelayqueue.PubSubMessage;

import java.io.File;
import java.util.*;

// TODO: write a description for this class
// TODO: complete all methods, irrespective of whether there is an explicit TODO or not
// TODO: write clear specs
// TODO: State the rep invariant and abstraction function
// TODO: what is the thread safety argument?
public class PhemeService {

    public static final int DELAY = 1000; // 1 second or 1000 milliseconds
    private File twitterCredentialsFile;
    private Set<PhemeServiceUser> users;

    public PhemeService(File twitterCredentialsFile) {
        this.twitterCredentialsFile = twitterCredentialsFile;
        this.users = new HashSet<>();
    }

    public void saveState(String configDirName) {

    }

    public boolean addUser(UUID userID, String userName, String hashPassword) {
        PhemeServiceUser user = new PhemeServiceUser(userID, userName, hashPassword);

        return true;
    }

    public boolean removeUser(String userName, String hashPassword) {
        return true;
    }

    public boolean cancelSubscription(String userName,
                                      String hashPassword,
                                      String twitterUserName) {
        return false;
    }

    public boolean cancelSubscription(String userName,
                                      String hashPassword,
                                      String twitterUserName,
                                      String pattern) {
        return false;
    }

    public boolean addSubscription(String userName, String hashPassword,
                                   String twitterUserName) {
        return false;
    }

    public boolean addSubscription(String userName, String hashPassword,
                                   String twitterUserName,
                                   String pattern) {
        return false;
    }

    public boolean sendMessage(String userName,
                               String hashPassword,
                               PubSubMessage msg) {
        return false;
    }

    public List<Boolean> isDelivered(UUID msgID, List<UUID> userList) {
        return new ArrayList<Boolean>();
    }

    public boolean isDelivered(UUID msgID, UUID user) {
        return false;
    }

    public boolean isUser(String userName) {
        return false;
    }

    public PubSubMessage getNext(String userName, String hashPassword) {
        return PubSubMessage.NO_MSG;
    }

    public List<PubSubMessage> getAllRecent(String userName, String hashPassword) {
        return new ArrayList<PubSubMessage>();
    }
}