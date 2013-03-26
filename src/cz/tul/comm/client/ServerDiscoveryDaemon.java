package cz.tul.comm.client;

import cz.tul.comm.Constants;
import cz.tul.comm.IService;
import cz.tul.comm.server.Comm_Server;
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
        s = new DatagramSocket(Constants.DEFAULT_PORT_DISCOVERY);
        s.setBroadcast(true);
        
        run = true;        
    }
    
    @Override
    public void run() {
        while (run) {
            if (!sr.isServerUp()) {
                try {
                    //Receive a packet
                    byte[] recvBuf = new byte[15000];
                    DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                    log.log(Level.FINER, "Starting listening for discovery packets");
                    s.receive(packet);

                    //Packet received
                    log.log(Level.FINER, "Discovery packet received from {0}", packet.getAddress().getHostAddress());

                    //See if the packet holds the right message
                    String message = new String(packet.getData()).trim();
                    if (message.equals(Constants.DISCOVERY_QUESTION)) {
                        final StringBuilder response = new StringBuilder();
                        response.append(Constants.DISCOVERY_RESPONSE);
                        response.append(Constants.DISCOVERY_RESPONSE_DELIMITER);
                        response.append(sr.getServerPort());
                        
                        byte[] sendData = response.toString().getBytes();

                        //Send a response
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), packet.getPort());
                        s.send(sendPacket);
                        
                        sr.registerServer(packet.getAddress(), Comm_Server.PORT);
                        
                        log.log(Level.FINER, "Sent response meesage to {0}", sendPacket.getAddress().getHostAddress());
                    }
                    
                } catch (SocketException ex) {
                    // everything is fine, wa wanted to interrupt socket receive method
                } catch (IOException ex) {
                    log.log(Level.WARNING, "Error answering client discovery packet", ex);
                }
            } else {
                try {
                    log.config("ServerDiscoveryDaemon is falling asleep.");
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
        log.config("ServerDiscoveryDaemon has been stopped.");
    }
}
