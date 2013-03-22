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
 *
 * @author Petr Ječmen
 */
public class ServerDiscoveryDaemon extends Thread implements IService {

    private static final Logger log = Logger.getLogger(ServerDiscoveryDaemon.class.getName());
    private final DatagramSocket s;
    private boolean run;

    public ServerDiscoveryDaemon() throws SocketException {
        s = new DatagramSocket(Constants.DEFAULT_PORT_DISCOVERY_C);
        s.setBroadcast(true);

        run = true;
    }

    @Override
    public void run() {
        while (run) {
            try {                
                //Receive a packet
                byte[] recvBuf = new byte[15000];
                DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                s.receive(packet);

                //Packet received
                log.log(Level.FINEST, ">>>Discovery packet received from: {0}", packet.getAddress().getHostAddress());                

                //See if the packet holds the right command (message)
                String message = new String(packet.getData()).trim();
                if (message.equals(Constants.DISCOVERY_QUESTION)) {
                    byte[] sendData = Constants.DISCOVERY_RESPONSE.getBytes();

                    //Send a response
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), packet.getPort());
                    s.send(sendPacket);

                    log.log(Level.FINEST, "Sent packet to: {0}", sendPacket.getAddress().getHostAddress());
                }

            } catch (IOException ex) {
                log.log(Level.WARNING, "Error answering client discovery packet", ex);
            }
        }
        s.close();
    }

    @Override
    public void stopService() {
        run = false;
        try {
            s.close();
        } catch (Exception ex) {
            // everything is fine, wa wanted to interrupt socket receive method
        }
    }
}
