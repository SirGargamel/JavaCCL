package cz.tul.comm.client;

import cz.tul.comm.Constants;
import cz.tul.comm.IService;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client side of DicoveryDaemon. Listens for server broadcast and responds to
 * them. Listens only when no server is aviable.
 *
 * @author Petr Ječmen
 */
public class ServerDiscoveryDaemon extends Thread implements IService {

    private static final Logger log = Logger.getLogger(ServerDiscoveryDaemon.class.getName());
    private final DatagramSocket s;
    private final IServerInterface sr;
    private boolean run;

    /**
     *
     * @param sr interface for settings new server settings
     * @throws SocketException thrown when client could not be created
     */
    public ServerDiscoveryDaemon(final IServerInterface sr) throws SocketException {
        this.sr = sr;
        s = new DatagramSocket(Constants.DEFAULT_PORT);
        s.setBroadcast(true);

        run = true;
    }

    @Override
    public void run() {
        while (run) {
            if (!sr.isServerUp()) {
                try {
                    //Receive a packet
                    byte[] recvBuf = new byte[15_000];
                    DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                    log.log(Level.FINE, "Starting listening for discovery packets");
                    s.receive(packet);

                    //Packet received
                    String message = new String(packet.getData()).trim();
                    log.log(Level.CONFIG, "Discovery packet received from {0} - {1}", new Object[]{packet.getAddress().getHostAddress(), message});

                    //See if the packet holds the right message                    
                    if (message.equals(Constants.DISCOVERY_QUESTION)) {
                        final StringBuilder response = new StringBuilder();
                        response.append(Constants.DISCOVERY_RESPONSE);
                        response.append(Constants.DISCOVERY_RESPONSE_DELIMITER);
                        response.append(sr.getServerComm().getPort());

                        byte[] sendData = response.toString().getBytes();

                        //Send a response
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), packet.getPort());
                        s.send(sendPacket);

                        sr.registerToServer(packet.getAddress(), Constants.DEFAULT_PORT);

                        log.log(Level.CONFIG, "Sent response meesage to {0}", sendPacket.getAddress().getHostAddress());
                    }

                } catch (SocketException ex) {
                    if (run == false) {
                        // everything is fine, wa wanted to interrupt socket receive method
                    } else {
                        log.log(Level.WARNING, "Error operating socket.", ex);
                    }
                } catch (IOException ex) {
                    log.log(Level.WARNING, "Error receiving or answering to client discovery packet", ex);
                }
            } else {
                try {
                    log.fine("ServerDiscoveryDaemon is falling asleep.");
                    synchronized (this) {
                        this.wait();
                    }
                } catch (InterruptedException ex) {
                    log.log(Level.WARNING, "ServerDiscoveryDaemon waiting has been interrupted.", ex);
                }
            }
        }
        s.close();
    }

    @Override
    public void stopService() {
        run = false;
        s.close();
        log.fine("ServerDiscoveryDaemon has been stopped.");
    }
}
