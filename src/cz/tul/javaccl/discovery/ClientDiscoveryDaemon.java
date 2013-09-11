package cz.tul.javaccl.discovery;

import cz.tul.javaccl.Constants;
import cz.tul.javaccl.IService;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Daemon tryning to discover new clients on local network using UDP broadcast.
 *
 * @author Petr Ječmen
 */
public class ClientDiscoveryDaemon extends DiscoveryDaemon implements IService {

    private static final Logger log = Logger.getLogger(ClientDiscoveryDaemon.class.getName());    
    private final byte[] message;
    private boolean run;

    /**
     * @param serverSocketPort port on which the server runs
     * @throws SocketException thrown when daemon could not be created
     */
    public ClientDiscoveryDaemon(final int serverSocketPort) throws SocketException {
        super();        
        message = Constants.DISCOVERY_QUESTION.concat(Constants.DELIMITER).concat(String.valueOf(serverSocketPort)).getBytes();
        run = true;
    }

    @Override
    public void run() {
        while (run) {
            try {
                broadcastMessage(message);
            } catch (SocketException ex) {
                log.log(Level.WARNING, "Error while checking status of network interfaces", ex);
            } catch (IOException ex) {
                log.log(Level.WARNING, "Error writing to socket", ex);
            }

            synchronized (this) {
                try {
                    this.wait(DELAY);
                } catch (InterruptedException ex) {
                    log.log(Level.WARNING, "Waiting of ClientDiscoveryDaemon has been interrupted.", ex);
                }
            }
        }
    }    

    @Override
    public void stopService() {
        super.stopService();
        run = false;        
        log.fine("ClientDiscoveryDaemon has been stopped.");
    }
}
