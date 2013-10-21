package cz.tul.javaccl.socket;

import cz.tul.javaccl.communicator.DataPacket;

/**
 * Interface for handling received data packets.
 *
 * @author Petr Ječmen
 */
public interface DataPacketHandler {

    /**
     * Deliver packet to listener and return the answer.
     *
     * @param dp packet for handling
     * @return response to packet
     */
    public Object handleDataPacket(final DataPacket dp);
}
