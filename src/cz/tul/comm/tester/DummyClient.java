package cz.tul.comm.tester;

import cz.tul.comm.socket.IMessageHandler;
import cz.tul.comm.Message;
import cz.tul.comm.client.Comm_Client;
import java.net.InetAddress;

/**
 * dummy client for testing.
 * @author Petr Ječmen
 */
public class DummyClient implements IMessageHandler {

    private Comm_Client com;
    private boolean state;

    public DummyClient() {
        state = true;
    }

    public void assignComm(final Comm_Client cc) {
        com = cc;
    }

    @Override
    public void handleMessage(final InetAddress adress, final Object msg) {
        if (msg != null && msg instanceof Message) {
            Message m = (Message) msg;
            switch (m.getData().toString()) {
                case "test":
                    if (state) {
                        Message mr = new Message(m.getId(), "header", "state");
                        com.sendMessage(mr);
                    } else {
                        Message mr = new Message(m.getId(), "header", "nostate");
                        com.sendMessage(mr);
                    }
                    break;
                case "config":
                    state = !state;
                    break;
            }
        } else {
            System.err.println("Invalid data from server.");
        }
    }
}
