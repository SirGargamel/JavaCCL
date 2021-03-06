package cz.tul.javaccl.communicator;

import cz.tul.javaccl.socket.ServerSocket;
import java.net.InetAddress;
import java.util.UUID;

/**
 * Simple data holder used for sending data between {@link Communicator} and
 * {@link ServerSocket}.
 *
 * @author Petr Ječmen
 */
public class DataPacketImpl implements DataPacket {

    private final UUID sourceID;
    private final UUID targetID;
    private final Object data;
    private InetAddress sourceIP;

    /**
     * New instance.
     *
     * @param sourceID sender UUID
     * @param targetID target UUID
     * @param data tata for sending
     */
    public DataPacketImpl(UUID sourceID, UUID targetID, Object data) {
        this.sourceID = sourceID;
        this.targetID = targetID;
        this.data = data;
    }

    @Override
    public UUID getSourceId() {
        return sourceID;
    }

    @Override
    public UUID getTargetId() {
        return targetID;
    }

    /**
     * @return data for sending
     */
    @Override
    public Object getData() {
        return data;
    }

    @Override
    public Object getId() {
        return targetID;
    }

    /**
     * @return IP, from which the data packet has been sent
     */
    public InetAddress getSourceIP() {
        return sourceIP;
    }

    /**
     * @param sourceAddress set IP, from which the DataPacketImpl has been
     * received
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
