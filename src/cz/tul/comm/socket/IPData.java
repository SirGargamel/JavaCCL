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
    private final Object data;

    /**
     * Store data to container.
     *
     * @param ip source IP
     * @param data received data
     */
    public IPData(final InetAddress ip, final Object data) {
        this.ip = ip;
        this.data = data;
    }

    @Override
    public Object getId() {
        return ip;
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
    public Object getData() {
        return data;
    }
}
