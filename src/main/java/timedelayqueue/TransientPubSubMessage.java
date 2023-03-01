package main.java.timedelayqueue;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Description: A transient PubSubMessage is a subclass of PubSubMessage that is automatically
//              removed from a TimeDelayQueue after a specified lifetime field

// Representation Invariant: stores a constant lifetime value and stores an isTransient flag as true

// Abstraction Function:  AF(t) = TransientPubSubMessage such that
//                           t.lifetime = lifetime
//                           t.isTransient = indicate this is a transient message

// Thread safety: The TransientPubSubMessage is thread safe because the representation
//                is strictly composed of immutable fields and no shared resources.
//                This makes logical sense since a message behaves in a manner
//                in which it is observed and does not change. Hence, multiple threads
//                could stochastically access the TransientPubSubMessage and observe
//                the fields, yet they will not be able to break the RI in any way.

public class TransientPubSubMessage extends PubSubMessage {
    private final int     lifetime;
    private final boolean isTransient = true;

    // create a TransientPubSubMessage instance with explicit args;
    // content should be in JSON format to accommodate a variety of
    // message types (e.g., TweetData)
    public TransientPubSubMessage(UUID id, Timestamp timestamp,
                                  UUID sender, UUID receiver, String content, MessageType type, int lifetime) {
        super(id, timestamp, sender, receiver, content, type);
        this.lifetime = lifetime;
    }

    // create a PubSubMessage instance with explicit args
    // a message may be intended for more than one user
    public TransientPubSubMessage(UUID id, Timestamp timestamp,
                                  UUID sender, List<UUID> receiver, String content, MessageType type, int lifetime) {
        super(id, timestamp, sender, receiver, content, type);
        this.lifetime = lifetime;
    }

    // create a TransientPubSubMessage instance with implicit args
    // list of receivers
    public TransientPubSubMessage(UUID sender, List<UUID> receiver, String content, int lifetime) {
        this(
                UUID.randomUUID(),
                new Timestamp(System.currentTimeMillis()),
                sender, receiver,
                content,
                BasicMessageType.SIMPLEMSG,
                lifetime
        );
    }

    // create a TransientPubSubMessage instance with implicit args
    // single receiver
    public TransientPubSubMessage(UUID sender, UUID receiver, String content, int lifetime) {
        this(
                UUID.randomUUID(),
                new Timestamp(System.currentTimeMillis()),
                sender, receiver,
                content,
                BasicMessageType.SIMPLEMSG,
                lifetime
        );
    }

    public int getLifetime() {
        return lifetime;
    }

    @Override
    public boolean isTransient() {
        return isTransient;
    }
}
