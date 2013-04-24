package cz.tul.comm.server.daemons;

import cz.tul.comm.Constants;
import cz.tul.comm.IService;
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
public class ClientDiscoveryDaemon extends Thread implements IService {

    private static final Logger log = Logger.getLogger(ClientDiscoveryDaemon.class.getName());
    private static final int DELAY = 10_000;
    private final DatagramSocket s;    
    private final byte[] message;
    private final int messageLength;
    private boolean run;

    /**
     * @param clientManager client manager for new client registration
     * @throws SocketException thrown when daemon could not be created
     */
    public ClientDiscoveryDaemon(final int serverSocketPort) throws SocketException {                
        s = new DatagramSocket(Constants.DEFAULT_PORT);
        s.setBroadcast(true);
        message = Constants.DISCOVERY_QUESTION.concat(Constants.DISCOVERY_QUESTION_DELIMITER).concat(String.valueOf(serverSocketPort)).getBytes();
        messageLength = message.length;
        run = true;
    }

    @Override
    public void run() {
        while (run) {
            try {
                discoverClients();
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

    private void discoverClients() throws SocketException, IOException {
        // Find the clients using UDP broadcast                
        // Try the 255.255.255.255 first
        DatagramPacket sendPacket = new DatagramPacket(message, messageLength, InetAddress.getByName("255.255.255.255"), Constants.DEFAULT_PORT);
        s.send(sendPacket);
        log.log(Level.FINE, "Discovery packet sent to: 255.255.255.255 (DEFAULT)");

        // Broadcast the message over all the network interfaces
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();

            if (!networkInterface.isUp()) {
                continue;
            }

            for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                InetAddress broadcast = interfaceAddress.getBroadcast();
                if (broadcast == null) {
                    continue;
                }

                // Send the broadcast
                sendPacket = new DatagramPacket(message, messageLength, broadcast, Constants.DEFAULT_PORT);
                s.send(sendPacket);
                log.log(Level.FINE, "Discovery packet sent to: {0}; Interface: {1}", new Object[]{broadcast.getHostAddress(), networkInterface.getDisplayName()});
            }
        }
    }

    @Override
    public void stopService() {
        run = false;
        s.close();
        log.fine("ClientDiscoveryDaemon has been stopped.");
    }
}
