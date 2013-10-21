package cz.tul.javaccl.communicator;

import cz.tul.javaccl.messaging.Identifiable;
import java.io.Serializable;
import java.util.UUID;

/**
 * Interface for transmitting data.
 *
 * @author Petr Jecmen
 */
public interface DataPacket extends Identifiable, Serializable {

    /**
     * @return data for sending
     */
    Object getData();

    @Override
    Object getId();

    /**
     * @return UUID of the server
     */
    UUID getSourceID();

    /**
     * @return UUID of the receiver
     */
    UUID getTargetID();
}
