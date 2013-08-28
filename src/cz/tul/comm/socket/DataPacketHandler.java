package cz.tul.comm.socket;

import cz.tul.comm.communicator.DataPacket;

/**
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
