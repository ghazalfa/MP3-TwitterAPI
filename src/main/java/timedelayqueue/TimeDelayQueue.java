package main.java.timedelayqueue;


import java.sql.Timestamp;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.*;
import java.util.stream.Collectors;

// Description: A TimeDelayQueue stores messages, the total number of messages ever added
//              and a history of the operations performed on the queue (add/getNext).
//              The TimeDelayQueue uses TimeStamps and System time to determine when
//              a message can be removed and when transient message are out of their lifetime

// Representation Invariant: Returns objects in an order that is determined by their individual timestamps.
//                           Stores a delay that does not change after being assigned during initialization.
//                           Increments the total message count when a message is added
//                           the count is retained and never decrements.
//                           Objects in the time delay queue are either persistent or transient.

// Abstraction Function: AF(q) = TimeDelayQueue such that
//                          q.messages = Objects in TimeDelayQueue
//                          q.totalMessageCount = counter of all objects that have been added
//                          q.delay = delay for getNext() behavior
//                          q.history = all timestamps of when operations have occurred

// Thread safety: We use the synchronized keyword on the methods below so that
//                threads operate in a mutually exclusive manner, using the class itself
//                as the mutex for synchronization
//
//                The methods, add(), getTotalMsgCount(), getNext(), getPeakLoad(), removeTransientMsg():
//                could face concurrent modification and require synchronization of their shared resources
//                mainly the list of messages and the history list.
//
//                For example, getPeakLoad() could be iterating over the history list
//                while another thread is modifying the history list by adding a message to the queue.
//
//                Note, we opted for a more coarse grained locking mechanism to optimize for safety
//                over efficiency. This design choice allows for modifications to be implemented
//                later that could increase the efficiency. The other way around or starting with efficiency
//                would be much less robust/safe.

public class TimeDelayQueue {

    // Store all current messages in a list
    private List<PubSubMessage> messages;

    // Store the total number of messages added (irrespective of those that have been removed)
    long totalMessageCount = 0;

    // Store the delay of the TimeDelayQueue (initialized in constructor)
    int delay;

    // Store all operations that have occurred by storing the timestamp in a list
    // Assume that multiple operations cannot happen at the same millisecond
    List<Long> history;

    // a comparator to sort messages
    private class PubSubMessageComparator implements Comparator<PubSubMessage> {
        public int compare(PubSubMessage msg1, PubSubMessage msg2) {
            return msg1.getTimestamp().compareTo(msg2.getTimestamp());
        }
    }

    /**
     * Create a new TimeDelayQueue
     * @param delay the delay, in milliseconds, that the queue can tolerate, >= 0
     */
    public TimeDelayQueue(int delay) {
        this.delay = delay;
        this.messages = new ArrayList<>();
        this.history = new ArrayList<>();
    }

    private synchronized void addToHistory() {
        history.add(System.currentTimeMillis());
    }

    /**
     * Add a message to the TimeDelayQueue
     * @param msg the message to add
     * @return false is a message with the same id exists
     */
    public synchronized boolean add(PubSubMessage msg) {
        addToHistory();

        // Since we are interacting with the TimeDelayQueue, update to match RI
        // Specifically, remove all transient messages that are beyond their lifetime
        //      before accessing the TimeDelayQueue for external operations
        removeTransientMsg();

        // The message that was removed can be added back again
        // So put this in a seperate conditional
        if (!messages.contains(msg)) {
            messages.add(msg);
            Collections.sort(messages, new PubSubMessageComparator());
            totalMessageCount++;
            return true;
        }
        return false;
    }

    /**
     * Get the count of the total number of messages processed
     * by this TimeDelayQueue over the lifetime of the queue
     * @return the total number of objects added to the queue
     */
    public synchronized long getTotalMsgCount() {
        return this.totalMessageCount;
    }

    /**
     * Get the next PubSubMessage in the TimeDelayQueue with the earliest Timestamp,
     * and whose time in the queue exceeds this.delay
     * @return the next PubSubMessage and PubSubMessage.NO_MSG if there is no suitable message
     */
    public synchronized PubSubMessage getNext() {
        addToHistory();

        // If nothing in queue, can't get next, return NO_MSG
        if (messages.size() == 0) return PubSubMessage.NO_MSG;

        // Since we are interacting with the TimeDelayQueue, update to match RI
        // Specifically, remove all transient messages that are beyond their lifetime
        //      before accessing the TimeDelayQueue for external operations
        removeTransientMsg();

        // There could be nothing left after removing the transient messages
        if (messages.size() == 0) return PubSubMessage.NO_MSG;
        PubSubMessage nextMsg = messages.get(0);

        Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
        if (currentTimestamp.getTime() - nextMsg.getTimestamp().getTime() >= delay) {
            messages.remove(nextMsg);
            return nextMsg;
        }
        return PubSubMessage.NO_MSG;
    }

    /**
     * Get the maximum number of operations performed on TimDelayQueue over any window
     * of time (the operations of interest are add and getNext)
     * @param timeWindow length of time of the window, must be >= 0
     * @return the maximum number of operations performed in the time window
     **/
    public synchronized int getPeakLoad(int timeWindow) {
        int temp = 0;
        int count = 0;
        long end_timestamp;
        int highest = 0;

        for(int i = 0; i<history.size(); i++){
            count = 0;
            end_timestamp = history.get(i) +timeWindow;
            temp = i;
            // Check that iterator, temp, doesn't go outside history list
            // Also stop once the current timestamps are beyond the starting timestamp + time window
            while(temp< history.size()&& history.get(temp)<=end_timestamp){
                count++;
                temp++;
            }

            if(count>highest){
                highest = count;
            }
        }

        return highest;
    }

    /**
     * Remove all TransientPubSubMessages from this.messages
     * if its time in the TimeDelayQueue exceeds the TransientPubSubMessage's lifetime
     * modifies: this.messages
     */
    public synchronized void removeTransientMsg() {
        // To work around concurrent modification exception, store objects to remove
        List<PubSubMessage> toRemove = new ArrayList<>();

        Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());

        // Get all transient messages outside their lifetime
        toRemove = messages.stream()
                .filter(PubSubMessage::isTransient) // equiv to (msg -> msg.isTransient())
                .filter(msg -> currentTimestamp.getTime() >= msg.getTimestamp().getTime() + (long) ((TransientPubSubMessage) msg).getLifetime())
                .toList();  // equiv to .collect(toList()) or .collect(Collectors.toList())

        messages.removeAll(toRemove);

        // Alternative
//        messages = messages.stream()
//                .filter(msg -> !(msg.isTransient() && currentTimestamp.getTime() >= msg.getTimestamp().getTime() + (long) ((TransientPubSubMessage) msg).getLifetime())) // equiv to (msg -> msg.isTransient())
//                .collect(Collectors.toList());  // equiv to .collect(toList()) or .collect(Collectors.toList())

    }
}
