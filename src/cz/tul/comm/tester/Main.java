package cz.tul.comm.tester;

import cz.tul.comm.client.Comm_Client;
import cz.tul.comm.server.Comm_Server;
import cz.tul.comm.socket.Communicator;
import java.net.InetAddress;

/**
 * class for testing.
 * @author Petr Ječmen
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        final DummyClient dc = new DummyClient();
        final Comm_Client c = Comm_Client.initNewClient();
        dc.assignComm(c);
        c.addMessageHandler(dc);

        final Comm_Server s = Comm_Server.initNewServer();
        final Communicator cc = s.registerClient(InetAddress.getLoopbackAddress());

//        Message m = new Message(UUID.randomUUID(), "header", "test");
//        Object result = cc.sendData(m);
//        System.out.println(result.toString());
//        m = new Message(UUID.randomUUID(), "header", "config");
//        result = cc.sendData(m);
//        System.out.println(result.toString());
//        m = new Message(UUID.randomUUID(), "header", "test");
//        result = cc.sendData(m);
//        System.out.println(result.toString());

        c.stopService();
        s.stopService();
    }
}
