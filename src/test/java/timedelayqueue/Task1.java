package timedelayqueue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class Task1 {

    private static final int DELAY        = 40; // delay of 40 milliseconds
    private static final int MSG_LIFETIME = 80;
    private static final Gson gson;

    static {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.serializeNulls();
        gson = gsonBuilder.create();
    }

    @Test
    public void testBasicAddRetrieve_NoDelay() {
        TimeDelayQueue tdq = new TimeDelayQueue(DELAY);
        UUID sndID     = UUID.randomUUID();
        UUID rcvID     = UUID.randomUUID();
        String msgText = gson.toJson("test");
        PubSubMessage msg1 = new PubSubMessage(sndID, rcvID, msgText);
        tdq.add(msg1);
        PubSubMessage msg2 = tdq.getNext();
        assertEquals(PubSubMessage.NO_MSG, msg2);
    }

    @Test
    public void testBasicAddRetrieve_Delay() {
        TimeDelayQueue tdq = new TimeDelayQueue(DELAY);
        UUID sndID     = UUID.randomUUID();
        UUID rcvID     = UUID.randomUUID();
        String msgText = gson.toJson("test");
        PubSubMessage msg1 = new PubSubMessage(sndID, rcvID, msgText);
        tdq.add(msg1);
        try {
            Thread.sleep(2 * DELAY);
        }
        catch (InterruptedException ie) {
            // nothing to do but ...
            fail();
        }
        PubSubMessage msg2 = tdq.getNext();
        assertEquals(msg1, msg2);
    }

    @Test
    public void testTransientMsg_InTime() {
        TimeDelayQueue tdq = new TimeDelayQueue(DELAY);

        UUID sndID     = UUID.randomUUID();
        UUID rcvID     = UUID.randomUUID();
        String msgText = gson.toJson("test");
        TransientPubSubMessage msg1 = new TransientPubSubMessage(sndID, rcvID, msgText, MSG_LIFETIME);
        PubSubMessage          msg2 = new PubSubMessage(sndID, rcvID, msgText);
        tdq.add(msg1);
        tdq.add(msg2);
        try {
            Thread.sleep(DELAY + 1);
        }
        catch (InterruptedException ie) {
            fail();
        }
        assertEquals(msg1, tdq.getNext());
        assertEquals(msg2, tdq.getNext());
    }

    @Test
    public void testTransientMsg_Late() {
        TimeDelayQueue tdq = new TimeDelayQueue(DELAY);

        UUID sndID     = UUID.randomUUID();
        UUID rcvID     = UUID.randomUUID();
        String msgText = gson.toJson("test");
        TransientPubSubMessage msg1 = new TransientPubSubMessage(sndID, rcvID, msgText, MSG_LIFETIME);
        PubSubMessage          msg2 = new PubSubMessage(sndID, rcvID, msgText);
        tdq.add(msg1);
        tdq.add(msg2);
        try {
            Thread.sleep(MSG_LIFETIME + 1);
        }
        catch (InterruptedException ie) {
            fail();
        }
        assertEquals(msg2, tdq.getNext()); // msg1 would have expired
    }

    @Test
    public void testMsgCount() {
        TimeDelayQueue tdq = new TimeDelayQueue(DELAY);

        final int NUM_MSGS = 10;
        for (int i = 0; i < NUM_MSGS; i++) {
            UUID sndID        = UUID.randomUUID();
            UUID rcvID        = UUID.randomUUID();
            String msgText    = gson.toJson("test");
            PubSubMessage msg = new PubSubMessage(sndID, rcvID, msgText);
            tdq.add(msg);
        }

        try {
            Thread.sleep(2 * DELAY);
        }
        catch (InterruptedException ie) {
            fail();
        }

        for (int i = 0; i < NUM_MSGS; i++) {
            tdq.getNext();
        }

        assertEquals(NUM_MSGS, tdq.getTotalMsgCount());
    }

    @Test
    public void testTransientMsg_New_Transient_After_Remove() {
        // Add a transient message
        // Wait for it to expire and get removed from the queue
        // Add a different transient message
        // You should be able to read that new transient
        TimeDelayQueue tdq = new TimeDelayQueue(DELAY);

        UUID sndID     = UUID.randomUUID();
        UUID rcvID     = UUID.randomUUID();
        String msgText = gson.toJson("test");
        TransientPubSubMessage msg1 = new TransientPubSubMessage(sndID, rcvID, msgText, MSG_LIFETIME);
        tdq.add(msg1);
        try {
            Thread.sleep(MSG_LIFETIME + 1);
        }
        catch (InterruptedException ie) {
            fail();
        }

        UUID sndID2     = UUID.randomUUID();
        UUID rcvID2     = UUID.randomUUID();
        String msgText2 = gson.toJson("test");
        TransientPubSubMessage msg2 = new TransientPubSubMessage(sndID2, rcvID2, msgText2, MSG_LIFETIME);
        tdq.add(msg2);

        // Delay to allow to read the next message
        try {
            Thread.sleep(DELAY + 1);
        }
        catch (InterruptedException ie) {
            fail();
        }

        assertEquals(msg2, tdq.getNext());
    }

    @Test
    public void testGetNextTransientRemove() {
        // Pre empt to the next test where we try to get msg4
        // Test that we can get msg3
        // In this test, at the time of getNext() we have {expired, msg3, expired, msg4}
        TimeDelayQueue tdq = new TimeDelayQueue(DELAY);

        UUID sndID     = UUID.randomUUID();
        UUID rcvID     = UUID.randomUUID();
        String msgText = gson.toJson("test");
        TransientPubSubMessage msg1 = new TransientPubSubMessage(sndID, rcvID, msgText, MSG_LIFETIME);
        TransientPubSubMessage msg2 = new TransientPubSubMessage(sndID, rcvID, msgText, MSG_LIFETIME);
        PubSubMessage msg3 = new PubSubMessage(sndID, rcvID, msgText);

        tdq.add(msg1);
        tdq.add(msg3);
        try {
            Thread.sleep(MSG_LIFETIME / 2);
        }
        catch (InterruptedException ie) {
            fail();
        }

        tdq.add(msg2);
        try {
            Thread.sleep(MSG_LIFETIME + 1);
        }
        catch (InterruptedException ie) {
            fail();
        }

        PubSubMessage msg4 = new PubSubMessage(sndID, rcvID, msgText);
        tdq.add(msg4);

        // Delay to allow to read the next message
        try {
            Thread.sleep(DELAY + 1);
        }
        catch (InterruptedException ie) {
            fail();
        }

        // msg 1 and 2 are expired and removed
        assertEquals(msg3, tdq.getNext());
    }

    @Test
    public void testGetNextTransientMultipleRemoveMultiple() {
        // Test is multiple expired messages will be removed
        // In this test, at the time of getNext() we have {expired, msg3, expired, msg4}
        // Try to get msg 4
        TimeDelayQueue tdq = new TimeDelayQueue(DELAY);

        UUID sndID     = UUID.randomUUID();
        UUID rcvID     = UUID.randomUUID();
        String msgText = gson.toJson("test");
        TransientPubSubMessage msg1 = new TransientPubSubMessage(sndID, rcvID, msgText, MSG_LIFETIME);
        TransientPubSubMessage msg2 = new TransientPubSubMessage(sndID, rcvID, msgText, MSG_LIFETIME);
        PubSubMessage msg3 = new PubSubMessage(sndID, rcvID, msgText);

        tdq.add(msg1);
        tdq.add(msg3);
        try {
            Thread.sleep(MSG_LIFETIME / 2);
        }
        catch (InterruptedException ie) {
            fail();
        }

        tdq.add(msg2);
        try {
            Thread.sleep(MSG_LIFETIME + 1);
        }
        catch (InterruptedException ie) {
            fail();
        }

        PubSubMessage msg4 = new PubSubMessage(sndID, rcvID, msgText);
        tdq.add(msg4);

        // Delay to allow to read the next message
        try {
            Thread.sleep(DELAY + 1);
        }
        catch (InterruptedException ie) {
            fail();
        }

        // msg 1 and 2 are expired and removed
        // Remove msg 3 and then get msg 4
        tdq.getNext();
        assertEquals(msg4, tdq.getNext());
    }

    @Test
    public void testGetNextTransientMultipleRemoveMultiple2() {
        // This varies by the test above since the first and second expired message
        // Must be removed in separate calls to removeTransientMsg()

        // Test is multiple expired messages will be removed
        // In this test, at the time of getNext() we have {expired, msg3, expired, msg4}
        // Try to get msg 4
        TimeDelayQueue tdq = new TimeDelayQueue(DELAY);

        UUID sndID     = UUID.randomUUID();
        UUID rcvID     = UUID.randomUUID();
        String msgText = gson.toJson("test");
        TransientPubSubMessage msg1 = new TransientPubSubMessage(sndID, rcvID, msgText, MSG_LIFETIME);
        TransientPubSubMessage msg2 = new TransientPubSubMessage(sndID, rcvID, msgText, MSG_LIFETIME);
        PubSubMessage msg3 = new PubSubMessage(sndID, rcvID, msgText);

        tdq.add(msg1);
        tdq.add(msg3);
        try {
            Thread.sleep(MSG_LIFETIME / 2);
        }
        catch (InterruptedException ie) {
            fail();
        }

        try {
            Thread.sleep(MSG_LIFETIME + 1);
        }
        catch (InterruptedException ie) {
            fail();
        }

        PubSubMessage msg4 = new PubSubMessage(sndID, rcvID, msgText);
        tdq.add(msg4);

        // Delay to allow to read the next message
        try {
            Thread.sleep(DELAY + 1);
        }
        catch (InterruptedException ie) {
            fail();
        }
        tdq.add(msg2);
        try {
            Thread.sleep(MSG_LIFETIME * 2);
        }
        catch (InterruptedException ie) {
            fail();
        }

        // msg 1 and 2 are expired and removed
        // Remove msg 3 and then get msg 4
        tdq.getNext();
        assertEquals(msg4, tdq.getNext());
    }

    @Test
    public void testGetNextTransientAndNormal() {
        // This varies by the test above since the ONLY the first message will be expired
        // In this test, at the time of getNext() we have {expired, msg3, not expired, msg4}
        // Get msg4
        TimeDelayQueue tdq = new TimeDelayQueue(DELAY);

        UUID sndID     = UUID.randomUUID();
        UUID rcvID     = UUID.randomUUID();
        String msgText = gson.toJson("test");
        TransientPubSubMessage msg1 = new TransientPubSubMessage(sndID, rcvID, msgText, MSG_LIFETIME);
        tdq.add(msg1);  // now {trans1}

        PubSubMessage msg3 = new PubSubMessage(sndID, rcvID, msgText);
        tdq.add(msg3); // now {trans1, msg3}

        try {
            Thread.sleep(MSG_LIFETIME + 1);
        }
        catch (InterruptedException ie) {
            fail();
        }

        TransientPubSubMessage msg2 = new TransientPubSubMessage(sndID, rcvID, msgText, MSG_LIFETIME);
        PubSubMessage msg4 = new PubSubMessage(sndID, rcvID, msgText);
        tdq.add(msg2); // now {expired trans1, msg3, trans2}
        tdq.add(msg4); // now {expired trans1, msg3, trans2, msg4}

        // msg 1 is expired and removed
        // Remove msg 3
        tdq.getNext(); // after will be {trans2, msg4}

        // Delay to allow to read the next message
        try {
            Thread.sleep(DELAY + 1);
        }
        catch (InterruptedException ie) {
            fail();
        }

        // Remove trans message not expired
        // Still here since MSG_LIFETIME > DELAY + 1
        tdq.getNext(); // after will be {msg4}
        // Get msg 4
        assertEquals(msg4, tdq.getNext());
    }

    @Test
    public void testTransientMsg_Remove_And_Add_Back() {
        // Add a transient message
        // Wait for it to expire and get removed from the queue
        // Add back another one of the same transient messages
        // This should be allowed because it is as if it was never added
        //      at the point in which the user tries to add it
        TimeDelayQueue tdq = new TimeDelayQueue(DELAY);

        UUID sndID     = UUID.randomUUID();
        UUID rcvID     = UUID.randomUUID();
        String msgText = gson.toJson("test");
        TransientPubSubMessage msg1 = new TransientPubSubMessage(sndID, rcvID, msgText, MSG_LIFETIME);
        tdq.add(msg1);
        try {
            Thread.sleep(MSG_LIFETIME + 1);
        }
        catch (InterruptedException ie) {
            fail();
        }

        // Create the same msg1 with an updated timestamp to current time
        // This message will have the same id, thus checking if the same
        //      message can be added (equality is by id)
        TransientPubSubMessage msg2 = new TransientPubSubMessage(msg1.getId(), new Timestamp(System.currentTimeMillis()),
                                        sndID, rcvID, msgText, msg1.getType(), MSG_LIFETIME);
        tdq.add(msg2);

        // Delay to allow to read the next message
        try {
            Thread.sleep(DELAY + 1);
        }
        catch (InterruptedException ie) {
            fail();
        }

        assertEquals(msg2, tdq.getNext());
    }

    @Test
    public void testTransientMsg_Add_Duplicate_Message_Within_Lifetime() {
        // Add a transient message to the queue
        // Then before that first message expires
        // Try to add it again
        // This should not be allowed and return false
        // when trying to add the second message (same message)
        TimeDelayQueue tdq = new TimeDelayQueue(DELAY);

        UUID sndID     = UUID.randomUUID();
        UUID rcvID     = UUID.randomUUID();
        String msgText = gson.toJson("test");
        TransientPubSubMessage msg1 = new TransientPubSubMessage(sndID, rcvID, msgText, 10000);
        PubSubMessage          msg2 = new PubSubMessage(sndID, rcvID, msgText);
        tdq.add(msg1);

        // --- --- ---
        // Extra redundant operations
        tdq.add(msg2);
        try {
            Thread.sleep(MSG_LIFETIME / 4); // Time will still be within lifetime by the next add()
        }
        catch (InterruptedException ie) {
            fail();
        }
        // --- --- ---

        assertFalse(tdq.add(msg1));
    }

    @Test
    public void testGetFromQueueWithNoMessage() {
        // Should return PubSubMessage.NO_MSG
        TimeDelayQueue tdq = new TimeDelayQueue(DELAY);
        assertEquals(PubSubMessage.NO_MSG, tdq.getNext()); // msg1 would have expired
    }



}
