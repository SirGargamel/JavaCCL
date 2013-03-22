package cz.tul.comm.server.daemons;

import cz.tul.comm.Constants;
import cz.tul.comm.IService;
import cz.tul.comm.server.IClientManager;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Petr Ječmen
 */
public class ClientDiscoveryDaemon extends Thread implements IService {

    private static final Logger log = Logger.getLogger(ClientDiscoveryDaemon.class.getName());
    private static final int DELAY = 60000;
    private final IClientManager cm;
    private final DatagramSocket s;
    private boolean run;

    public ClientDiscoveryDaemon(final IClientManager clientManager) throws SocketException {
        run = true;
        cm = clientManager;
        s = new DatagramSocket(Constants.DEFAULT_PORT_DISCOVERY);
        s.setBroadcast(true);
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

            try {
                listenForResponse();
            } catch (IOException ex) {
                log.log(Level.WARNING, "Error reading from socket", ex);
            }
        }
    }

    public void discoverClients() throws SocketException, IOException {
        // Find the server using UDP broadcast                                
        // TODO send valid msg
        byte[] sendData = Constants.DISCOVERY_QUESTION.getBytes();

        // Try the 255.255.255.255 first
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("255.255.255.255"), Constants.DEFAULT_PORT_DISCOVERY);
        s.send(sendPacket);
        log.log(Level.FINEST, "Discovery packet sent to: 255.255.255.255 (DEFAULT)");

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
                sendPacket = new DatagramPacket(sendData, sendData.length, broadcast, 8888);
                s.send(sendPacket);


                log.log(Level.FINEST, "Discovery packet sent to: {0}; Interface: {1}", new Object[]{broadcast.getHostAddress(), networkInterface.getDisplayName()});
            }
        }
        log.log(Level.FINER, "Done looping over all network interfaces.");
    }

    public void listenForResponse() throws IOException {
        final long endTime = System.currentTimeMillis() + DELAY;
        //Wait for a response
        byte[] recvBuf = new byte[15000];

        while (System.currentTimeMillis() < endTime) {
            s.setSoTimeout((int) (endTime - System.currentTimeMillis()));

            DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
            try {
                s.receive(receivePacket);

                //We have a response
                log.log(Level.FINER, "Broadcast response from client: {0}", receivePacket.getAddress().getHostAddress());

                //Check if the message is correct
                String message = new String(receivePacket.getData()).trim();
                if (message.equals(Constants.DISCOVERY_RESPONSE)) {
                    // TODO register new client
                }
            } catch (SocketTimeoutException ex) {
                // nothing bad happened
                // delay time reached
            }
        }
    }

    @Override
    public void stopService() {
        run = false;
    }
}
