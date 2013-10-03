package cz.tul.javaccl.messaging;

import cz.tul.javaccl.Constants;
import cz.tul.javaccl.GenericResponses;
import cz.tul.javaccl.client.Client;
import cz.tul.javaccl.client.ClientImpl;
import cz.tul.javaccl.communicator.CommunicatorImpl;
import cz.tul.javaccl.communicator.CommunicatorInner;
import cz.tul.javaccl.communicator.DataPacket;
import cz.tul.javaccl.exceptions.ConnectionException;
import cz.tul.javaccl.exceptions.ConnectionExceptionCause;
import cz.tul.javaccl.server.Server;
import cz.tul.javaccl.server.ServerImpl;
import cz.tul.javaccl.socket.Listener;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Observable;
import java.util.Observer;
import java.util.Queue;
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
        try {
            c = ClientImpl.initNewClient(5253);
        } catch (IOException ex) {
            fail("Failed to initialize client.");
        }
    }

    @After
    public void tearDown() {
        c.stopService();
        s.stopService();
    }

    @Test
    public void testRegistration() {
        System.out.println("testRegistration");

        try {
            assertTrue(c.registerToServer(InetAddress.getByName(Constants.IP_LOOPBACK)));
        } catch (Exception ex) {
            fail("Registration from client to server failed - " + ex);
        }

        try {
            c.deregisterFromServer();
        } catch (ConnectionException ex) {
            fail("Client deregistration failed.");
        }

        try {
            assertNotNull(s.getClientManager().registerClient(InetAddress.getByName(Constants.IP_LOOPBACK), PORT_CLIENT));
        } catch (Exception ex) {
            fail("Registration from server to client failed - " + ex);
        }

        try {
            CommunicatorInner comm = (CommunicatorInner) CommunicatorImpl.initNewCommunicator(InetAddress.getByName(Constants.IP_LOOPBACK), 5252);
            comm.setTargetId(Constants.ID_SERVER);
            comm.sendData("data");
            fail("Should have failed, because this communicator is not registered.");
        } catch (ConnectionException ex) {
            assertEquals(ConnectionExceptionCause.UUID_NOT_ALLOWED, ex.getExceptionCause());
        } catch (Exception ex) {
            fail(ex.getLocalizedMessage());
        }
    }

    @Test
    public void testIdMessaging() {
        System.out.println("testIdMessaging");

        try {
            assertTrue(c.registerToServer(InetAddress.getByName(Constants.IP_LOOPBACK)));
        } catch (Exception ex) {
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
        });
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
        });

        try {
            assertNotNull(s.getClient(c.getLocalID()).sendData(new Message(id, msgHeader, msgToClient)));
            assertNotNull(c.getServerComm().sendData(new Message(id, msgHeader, msgToServer)));
            assertEquals(msgToClient.concat(msgToServer), sb.toString());
        } catch (ConnectionException ex) {
            Logger.getLogger(MessagingTest.class.getName()).log(Level.SEVERE, null, ex);
        }

        c.getListenerRegistrator().removeIdListener(id);
        try {
            assertEquals(GenericResponses.NOT_HANDLED, s.getClient(c.getLocalID()).sendData(new Message(id, msgHeader, msgToClient)));
        } catch (ConnectionException ex) {
            Logger.getLogger(MessagingTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Test
    public void testIdMessagingNoPush() {
        System.out.println("testIdMessagingNoPush");

        try {
            assertTrue(c.registerToServer(InetAddress.getByName(Constants.IP_LOOPBACK)));
        } catch (Exception ex) {
            fail("Registration from client to server failed - " + ex);
        }

        final UUID id = UUID.randomUUID();
        final String msgToClient = "testDataToClient";
        final String msgToServer = "testDataToServer";
        final String msgHeader = "header";        

        final Queue<Identifiable> clientQueue = c.getListenerRegistrator().createIdMessageQueue(id);
        final Queue<Identifiable> serverQueue = s.getListenerRegistrator().createIdMessageQueue(id);

        try {
            Message m = new Message(id, msgHeader, msgToClient);
            assertEquals(GenericResponses.NOT_HANDLED_DIRECTLY, s.getClient(c.getLocalID()).sendData(m));
            assertEquals(1, clientQueue.size());
            Identifiable data = clientQueue.poll();
            if (data instanceof DataPacket) {
                DataPacket dp = (DataPacket) data;                
                assertEquals(m, dp.getData());
            } else {
                fail("Illegal data inside data packet.");
            }

            m = new Message(id, msgHeader, msgToServer);
            assertEquals(GenericResponses.NOT_HANDLED_DIRECTLY, c.getServerComm().sendData(m));
            assertEquals(1, serverQueue.size());
            data = serverQueue.poll();
            if (data instanceof DataPacket) {
                DataPacket dp = (DataPacket) data;
                assertEquals(m, dp.getData());
            } else {
                fail("Illegal data inside data packet.");
            }
        } catch (ConnectionException ex) {
            fail("Communication failed.");
        }

        c.getListenerRegistrator().removeIdListener(id);
        try {
            assertEquals(GenericResponses.NOT_HANDLED, s.getClient(c.getLocalID()).sendData(new Message(id, msgHeader, msgToClient)));
        } catch (ConnectionException ex) {
            Logger.getLogger(MessagingTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Test
    public void testClientMessaging() {
        System.out.println("testClientMessaging");

        try {
            assertTrue(c.registerToServer(InetAddress.getByName(Constants.IP_LOOPBACK)));
        } catch (Exception ex) {
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
        });
        c.getListenerRegistrator().setClientListener(Constants.ID_SERVER, new Listener<DataPacket>() {
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
        });

        final String header = "abcdefg";
        final Message m = new Message(header, "data");

        final StringBuilder sb = new StringBuilder(header);
        final String reverseHeader = sb.reverse().toString();

        try {
            assertEquals(reverseHeader, c.sendDataToServer(m));
            assertEquals(GenericResponses.OK, c.sendDataToServer(header));

            assertEquals(reverseHeader, s.getClient(c.getLocalID()).sendData(m));
            assertEquals(GenericResponses.OK, s.getClient(c.getLocalID()).sendData(header));
        } catch (ConnectionException ex) {
            fail("Communication failed - " + ex);
        }

        c.getListenerRegistrator().removeClientListener(Constants.ID_SERVER);
        try {
            assertEquals(GenericResponses.NOT_HANDLED, s.getClient(c.getLocalID()).sendData(m));
        } catch (ConnectionException ex) {
            Logger.getLogger(MessagingTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Test
    public void testClientMessagingNoPush() {
        System.out.println("testClientMessagingNoPush");

        try {
            assertTrue(c.registerToServer(InetAddress.getByName(Constants.IP_LOOPBACK)));
        } catch (Exception ex) {
            fail("Registration from client to server failed - " + ex);
        }

        final Queue<DataPacket> serverQueue = s.getListenerRegistrator().createClientMessageQueue(c.getLocalID());
        final Queue<DataPacket> clientQueue = c.getListenerRegistrator().createClientMessageQueue(Constants.ID_SERVER);

        final String header = "abcdefg";
        final Message m = new Message(header, "data");

        try {
            assertEquals(GenericResponses.NOT_HANDLED_DIRECTLY, c.sendDataToServer(m));
            assertEquals(GenericResponses.NOT_HANDLED_DIRECTLY, c.sendDataToServer(header));
            assertEquals(2, serverQueue.size());
            DataPacket dp = serverQueue.poll();
            if (dp.getData() instanceof Message) {
                assertEquals(m, dp.getData());
            } else {
                fail("Illegal data inside data packet.");
            }
            dp = serverQueue.poll();
            assertEquals(header, dp.getData());

            assertEquals(GenericResponses.NOT_HANDLED_DIRECTLY, s.getClient(c.getLocalID()).sendData(m));
            assertEquals(GenericResponses.NOT_HANDLED_DIRECTLY, s.getClient(c.getLocalID()).sendData(header));
            assertEquals(2, clientQueue.size());
            dp = clientQueue.poll();
            if (dp.getData() instanceof Message) {
                assertEquals(m, dp.getData());
            } else {
                fail("Illegal data inside data packet.");
            }
            dp = clientQueue.poll();
            assertEquals(header, dp.getData());
        } catch (ConnectionException ex) {
            fail("Communication failed - " + ex);
        }

        c.getListenerRegistrator().removeClientListener(Constants.ID_SERVER);
        try {
            assertEquals(GenericResponses.NOT_HANDLED, s.getClient(c.getLocalID()).sendData(m));
        } catch (ConnectionException ex) {
            Logger.getLogger(MessagingTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Test
    public void testMessageObserver() {
        System.out.println("testMessageObserver");

        try {
            assertTrue(c.registerToServer(InetAddress.getByName(Constants.IP_LOOPBACK)));
        } catch (Exception ex) {
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
        final Observer o = new Observer() {
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
        };
        c.getListenerRegistrator().addMessageObserver(o);

        try {
            final String header = "abcdefg";
            final Message m = new Message(header, "data");
            final StringBuilder expected = new StringBuilder();

            assertEquals(GenericResponses.NOT_HANDLED_DIRECTLY, c.sendDataToServer(m,100000));
            assertEquals(GenericResponses.NOT_HANDLED_DIRECTLY, s.getClient(c.getLocalID()).sendData(m));
            expected.append(header);
            expected.reverse();

            synchronized (this) {
                this.wait(10);
            }

            assertEquals(expected.toString(), sbServer.toString());
            assertEquals(expected.toString(), sbClient.toString());

            assertEquals(GenericResponses.NOT_HANDLED_DIRECTLY, c.sendDataToServer(header));
            assertEquals(GenericResponses.NOT_HANDLED_DIRECTLY, s.getClient(c.getLocalID()).sendData(header));
            expected.append(GenericResponses.OK);

            synchronized (this) {
                this.wait(10);
            }

            assertEquals(expected.toString(), sbServer.toString());
            assertEquals(expected.toString(), sbClient.toString());

            c.getListenerRegistrator().removeMessageObserver(o);
            s.getClient(c.getLocalID()).sendData(m);
            assertEquals(expected.toString(), sbClient.toString());
        } catch (ConnectionException ex) {
            fail("Communication failed - " + ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(MessagingTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Test
    public void testMessageInvalidTarget() {
        System.out.println("testMessageInvalidTarget");

        try {
            assertTrue(c.registerToServer(InetAddress.getByName(Constants.IP_LOOPBACK)));
        } catch (Exception ex) {
            fail("Registration from client to server failed - " + ex);
        }

        try {
            CommunicatorInner comm = (CommunicatorInner) c.getServerComm();
            comm.setTargetId(UUID.randomUUID());
            comm.sendData("data");
            fail("Should have failed, because this communicator has illegal target ID.");
        } catch (ConnectionException ex) {
            assertEquals(ConnectionExceptionCause.WRONG_TARGET, ex.getExceptionCause());
        }
    }
}