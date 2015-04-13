package cz.tul.javaccl.communicator;

import java.io.Serializable;
import java.util.UUID;

/**
 *
 * @author Lenam s.r.o.
 */
public class StatusMessage implements Serializable {
    
    private final UUID id;

    public StatusMessage(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
    
}
