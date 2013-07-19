package cz.tul.comm.communicator;

import cz.tul.comm.socket.ServerSocket;
import cz.tul.comm.socket.queue.Identifiable;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.UUID;

/**
 * Simple data holder used for sending data between {@link Communicator} and
 * {@link ServerSocket}.
 *
 * @author Petr Ječmen
 */
public class DataPacket implements Serializable, Identifiable {

    private final UUID sourceID;
    private final UUID targetID;
    private final Object data;
    private InetAddress sourceIP;

    public DataPacket(UUID sourceID, UUID targetID, Object data) {
        this.sourceID = sourceID;
        this.targetID = targetID;
        this.data = data;
    }    

    public UUID getSourceID() {
        return sourceID;
    }

    public UUID getTargetID() {
        return targetID;
    }    

    /**
     * @return data for sending
     */
    public Object getData() {
        return data;
    }

    @Override
    public Object getId() {
        return getTargetID();
    }

    /**
     * @return IP, from which the data packet has been sent
     */
    public InetAddress getSourceIP() {
        return sourceIP;
    }

    /**
     * @param sourceAddress set IP, from which the DataPacket has been received
     */
    public void setSourceIP(InetAddress sourceAddress) {
        this.sourceIP = sourceAddress;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();        
        if (sourceID != null) {            
            sb.append(sourceID.toString());
        }        
        if (targetID != null) {
            sb.append(" - ");
            sb.append(targetID.toString());
        }
        sb.append(" - [");
        if (data != null) {            
            sb.append(data.toString());
        }
        sb.append("]");
        return sb.toString();
    }
}
