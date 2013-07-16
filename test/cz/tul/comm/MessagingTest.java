package cz.tul.comm;

import cz.tul.comm.client.Client;
import cz.tul.comm.client.ClientImpl;
import cz.tul.comm.exceptions.ConnectionException;
import cz.tul.comm.messaging.Message;
import cz.tul.comm.server.Server;
import cz.tul.comm.server.ServerImpl;
import cz.tul.comm.socket.queue.Identifiable;
import cz.tul.comm.socket.queue.Listener;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Petr Jecmen
 */
public class MessagingTest {

    private static final int PORT_CLIENT = 5253;
    private Server s;
    private Client c;

    @Before
    public void setUp() {
        s = ServerImpl.initNewServer();
        c = ClientImpl.initNewClient(5253);
    }

    @After
    public void tearDown() {
        c.stopService();
        s.stopService();
    }

    @Test
    public void testRegistration() {
        try {
            assertTrue(c.registerToServer(InetAddress.getLoopbackAddress()));
        } catch (ConnectionException ex) {
            fail("Registration from client to server failed - " + ex);
        }

        try {
            c.deregisterFromServer();
        } catch (ConnectionException ex) {
            fail("Client deregistration failed.");
        }

        try {
            assertNotNull(s.getClientManager().registerClient(InetAddress.getLoopbackAddress(), PORT_CLIENT));
        } catch (ConnectionException ex) {
            fail("Registration from server to client failed - " + ex);
        }
    }

    @Test
    public void testMessaging() {
        try {
            assertTrue(c.registerToServer(InetAddress.getLoopbackAddress()));
        } catch (ConnectionException ex) {
            fail("Registration from client to server failed - " + ex);
        }

        final UUID id = UUID.randomUUID();
        final String msgToClient = "testDataToClient";
        final String msgToServer = "testDataToServer";
        final String msgHeader = "header";

        final StringBuilder sb = new StringBuilder();

        c.getListenerRegistrator().setIdListener(id, new Listener() {
            @Override
            public Object receiveData(Identifiable data) {
                Object result = null;
                if (data instanceof Message) {
                    final Message m = (Message) data;
                    if (m.getId().equals(id)
                            && m.getHeader().equals(msgHeader)) {
                        sb.append(m.getData());
                    }
                    result = GenericResponses.OK;
                }
                return result;
            }
        }, true);

        s.getListenerRegistrator().setIdListener(id, new Listener() {
            @Override
            public Object receiveData(Identifiable data) {
                Object result = null;
                if (data instanceof Message) {
                    final Message m = (Message) data;
                    if (m.getId().equals(id)
                            && m.getHeader().equals(msgHeader)) {
                        sb.append(m.getData());
                    }
                    result = GenericResponses.OK;
                }
                return result;
            }
        }, true);
        try {
            assertNotNull(s.getClient(c.getLocalID()).sendData(new Message(id, msgHeader, msgToClient)));
            assertNotNull(c.getServerComm().sendData(new Message(id, msgHeader, msgToServer)));
            assertEquals(msgToClient.concat(msgToServer), sb.toString());
        } catch (ConnectionException ex) {
            Logger.getLogger(MessagingTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Test
    public void testSendDataToClient() {
        final Client c2 = ClientImpl.initNewClient();

        try {
            assertTrue(c.registerToServer(InetAddress.getLoopbackAddress()));
            assertTrue(c2.registerToServer(InetAddress.getLoopbackAddress()));
        } catch (ConnectionException ex) {
            fail("Registration of any of the clients failed - " + ex);
        }

        final UUID id = UUID.randomUUID();
        final String testData = "testData";
        final Object msg = new Message(id, "header", testData);

        c2.getListenerRegistrator().setIdListener(id, new Listener() {
            @Override
            public Object receiveData(Identifiable data) {                
                assertTrue(data instanceof Message);

                Message m = (Message) data;
                
                assertEquals(testData, m.getData().toString());
                
                StringBuilder sb = new StringBuilder(m.getData().toString());
                sb.reverse();                

                return sb.toString();
            }
        }, true);

        try {
            final Object response = c.sendDataToClient(c2.getLocalID(), msg);
            final String expected = new StringBuffer(testData).reverse().toString();
            assertEquals(
                    expected,
                    response.toString());
        } catch (ConnectionException ex) {
            fail("Communication failed - " + ex);
        } catch (UnknownHostException ex) {
            fail("Wrong clint ID used - " + ex);
        }
    }
}