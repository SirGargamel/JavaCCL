package cz.tul.comm.socket;

import cz.tul.comm.socket.queue.IIdentifiable;
import java.net.InetAddress;

/**
 *
 * @author Gargamel
 */
public class IPData implements IIdentifiable {
    
    private final InetAddress ip;
    private final Object data;

    public IPData(final InetAddress ip, final Object data) {
        this.ip = ip;
        this.data = data;
    }

    @Override
    public Object getId() {
        return ip;
    }

    public InetAddress getIp() {
        return ip;
    }

    public Object getData() {
        return data;
    }
    
}
