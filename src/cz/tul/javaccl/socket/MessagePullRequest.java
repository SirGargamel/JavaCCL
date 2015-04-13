package cz.tul.javaccl.socket;

import java.io.Serializable;
import java.util.UUID;

/**
 *
 * @author Lenam s.r.o.
 */
public class MessagePullRequest implements Serializable {

    private final UUID clientId;

    public MessagePullRequest(final UUID clientId) {
        this.clientId = clientId;
    }

    public UUID getClientId() {
        return clientId;
    }
    
}
