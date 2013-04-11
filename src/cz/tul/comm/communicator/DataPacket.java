package cz.tul.comm.communicator;

import cz.tul.comm.socket.ServerSocket;
import cz.tul.comm.socket.queue.IIdentifiable;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.UUID;

/**
 * Simple data holder used for sending data between {@link Communicator} and
 * {@link ServerSocket}.
 *
 * @author Petr Ječmen
 */
public class DataPacket implements Serializable, IIdentifiable {

    private final UUID clientID;
    private final Object data;
    private InetAddress sourceIP;

    /**
     * Init new data packet.
     *
     * @param clientID clients ID
     * @param data data for sending
     */
    public DataPacket(UUID clientID, Object data) {
        this.clientID = clientID;
        this.data = data;
    }

    /**
     * @return clients ID
     */
    public UUID getClientID() {
        return clientID;
    }

    /**
     * @return data for sending
     */
    public Object getData() {
        return data;
    }

    @Override
    public Object getId() {
        return getClientID();
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
        sb.append("[");
        if (clientID != null) {
            sb.append(clientID.toString());
        } else {
            sb.append("No ID");
        }
        if (data != null) {
            sb.append(" - ");
            sb.append(data.toString());
        } else {
            sb.append(", no data");
        }
        sb.append("]");
        return sb.toString();
    }
}