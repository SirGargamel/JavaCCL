package cz.tul.comm.socket;

import cz.tul.comm.communicator.DataPacket;
import java.net.InetAddress;

/**
 * Simple data container for received data with IP address from which the data
 * has been received.
 *
 * @author Gargamel
 */
public class IPData {
    
    private final InetAddress ip;    
    private final DataPacket dataPacket;

    /**
     * Store data to container.
     *
     * @param ip source IP
     * @param port source port
     * @param data received data
     */
    public IPData(final InetAddress ip, final DataPacket data) {
        this.ip = ip;        
        this.dataPacket = data;        
    }

    /**
     *
     * @return source IP
     */
    public InetAddress getIp() {
        return ip;
    }

    /**
     *
     * @return received data
     */
    public DataPacket getDataPacket() {
        return dataPacket;
    }
}
