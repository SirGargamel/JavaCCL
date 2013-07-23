package cz.tul.comm.messaging;

import cz.tul.comm.GenericResponses;
import cz.tul.comm.client.Client;
import cz.tul.comm.client.ClientImpl;
import cz.tul.comm.communicator.DataPacket;
import cz.tul.comm.exceptions.ConnectionException;
import cz.tul.comm.server.Server;
import cz.tul.comm.server.ServerImpl;
import cz.tul.comm.socket.queue.Identifiable;
import cz.tul.comm.socket.queue.Listener;
import java.net.InetAddress;
import java.util.Observable;
import java.util.Observer;
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
    public void testIdMessaging() {
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

        c.getListenerRegistrator().setIdListener(id, new Listener<Identifiable>() {
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
        s.getListenerRegistrator().setIdListener(id, new Listener<Identifiable>() {
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

        c.getListenerRegistrator().removeIdListener(id);
        try {
            assertEquals(GenericResponses.NOT_HANDLED_DIRECTLY, s.getClient(c.getLocalID()).sendData(new Message(id, msgHeader, msgToClient)));
        } catch (ConnectionException ex) {
            Logger.getLogger(MessagingTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Test
    public void testClientMessaging() {
        try {
            assertTrue(c.registerToServer(InetAddress.getLoopbackAddress()));
        } catch (ConnectionException ex) {
            fail("Registration from client to server failed - " + ex);
        }

        s.getListenerRegistrator().setClientListener(c.getLocalID(), new Listener<DataPacket>() {
            @Override
            public Object receiveData(DataPacket data) {
                if (data.getData() instanceof Message) {
                    final Message m = (Message) data.getData();
                    StringBuilder sb = new StringBuilder(m.getHeader());
                    return sb.reverse().toString();
                } else {
                    return GenericResponses.OK;
                }
            }
        }, true);

        final String header = "abcdefg";
        final Message m = new Message(header, "data");

        final StringBuilder sb = new StringBuilder(header);
        final String reverseHeader = sb.reverse().toString();

        try {
            assertEquals(reverseHeader, c.sendDataToServer(m));
            assertEquals(GenericResponses.OK, c.sendDataToServer(header));
        } catch (ConnectionException ex) {
            fail("Communication failed - " + ex);
        }
    }

    @Test
    public void testMessageObserver() {
        try {
            assertTrue(c.registerToServer(InetAddress.getLoopbackAddress()));
        } catch (ConnectionException ex) {
            fail("Registration from client to server failed - " + ex);
        }

        final StringBuilder sbServer = new StringBuilder();
        final StringBuilder sbClient = new StringBuilder();

        s.getListenerRegistrator().addMessageObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                if (arg instanceof DataPacket) {
                    DataPacket dp = (DataPacket) arg;

                    if (dp.getData() instanceof Message) {
                        final Message m = (Message) dp.getData();
                        sbServer.append(m.getHeader());
                        sbServer.reverse();
                    } else {
                        sbServer.append(GenericResponses.OK);
                    }
                }
            }
        });
        c.getListenerRegistrator().addMessageObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                if (arg instanceof DataPacket) {
                    DataPacket dp = (DataPacket) arg;

                    if (dp.getData() instanceof Message) {
                        final Message m = (Message) dp.getData();
                        sbClient.append(m.getHeader());
                        sbClient.reverse();
                    } else {
                        sbClient.append(GenericResponses.OK);
                    }
                }
            }
        });

        try {
            final String header = "abcdefg";
            final Message m = new Message(header, "data");
            final StringBuilder expected = new StringBuilder();

            c.sendDataToServer(m);
            s.getClient(c.getLocalID()).sendData(m);
            expected.append(header);
            expected.reverse();
            assertEquals(expected.toString(), sbServer.toString());
            assertEquals(expected.toString(), sbClient.toString());

            c.sendDataToServer(header);
            s.getClient(c.getLocalID()).sendData(header);
            expected.append(GenericResponses.OK);
            assertEquals(expected.toString(), sbServer.toString());
            assertEquals(expected.toString(), sbClient.toString());
        } catch (ConnectionException ex) {
            fail("Communication failed - " + ex);
        }
    }
}