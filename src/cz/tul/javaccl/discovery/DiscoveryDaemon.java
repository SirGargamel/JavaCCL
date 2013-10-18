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

/**
 *
 * @author Petr Jecmen
 */
abstract class DiscoveryDaemon extends Thread implements IService {
    
    protected static final int DELAY = 10000;
    protected final DatagramSocket s;

    public DiscoveryDaemon() throws SocketException {
        s = new DatagramSocket(Constants.DEFAULT_PORT);
        s.setBroadcast(true);
        s.setSoTimeout(DELAY);
    }

    protected void broadcastMessage(final byte[] msg) throws SocketException, IOException {
        // Find the clients using UDP broadcast                
        // Try the 255.255.255.255 first
        final int messageLength = msg.length;
        DatagramPacket sendPacket = new DatagramPacket(msg, messageLength, InetAddress.getByName("255.255.255.255"), Constants.DEFAULT_PORT);
        s.send(sendPacket);        

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
                sendPacket = new DatagramPacket(msg, messageLength, broadcast, Constants.DEFAULT_PORT);
                s.send(sendPacket);                
            }
        }
    }

    @Override
    public void stopService() {
        s.close();
    }
}
