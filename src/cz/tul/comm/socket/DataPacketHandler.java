package cz.tul.comm.socket;

import cz.tul.comm.communicator.DataPacket;

/**
 *
 * @author Petr Ječmen
 */
public interface DataPacketHandler {
    
    public Object handleDataPacket(final DataPacket dp);
    
}
