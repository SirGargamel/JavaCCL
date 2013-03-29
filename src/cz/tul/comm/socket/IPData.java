package cz.tul.comm.socket;

import cz.tul.comm.socket.queue.IIdentifiable;
import java.net.InetAddress;

/**
 * Simple data container for received data with IP address from which the data
 * has been received.
 *
 * @author Gargamel
 */
public class IPData implements IIdentifiable {
    
    private final InetAddress ip;
    private final int port;
    private final Object data;

    /**
     * Store data to container.
     *
     * @param ip source IP
     * @param port source port
     * @param data received data
     */
    public IPData(final InetAddress ip, final int port, final Object data) {
        this.ip = ip;
        this.port = port;
        this.data = data;        
    }

    @Override
    public Object getId() {
        return getIp();
    }

    /**
     *
     * @return source IP
     */
    public InetAddress getIp() {
        return ip;
    }

    /**
     * @return port number, from which the data hse been received
     */
    public int getPort() {
        return port;
    }

    /**
     *
     * @return received data
     */
    public Object getData() {
        return data;
    }
}
