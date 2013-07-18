package cz.tul.comm.server;

import cz.tul.comm.client.Client;
import cz.tul.comm.client.ClientImpl;
import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.communicator.CommunicatorInner;
import cz.tul.comm.exceptions.ConnectionException;
import java.net.InetAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 *
 * @author Petr Jecmen
 */
public class ClientDBTest {

    private static final int PORT_CLIENT_1 = 5253;
    private static final int PORT_CLIENT_2 = 5254;
    private static Client c1, c2;
    private static Server s;

    @Before
    public void setUp() {                
        s = ServerImpl.initNewServer();
        c1 = ClientImpl.initNewClient(PORT_CLIENT_1);
        c2 = ClientImpl.initNewClient(PORT_CLIENT_2);
    }

    @After
    public void tearDown() {
        c1.stopService();
        c2.stopService();
        s.stopService();
    }

    /**
     * Test of registerClient method, of class ClientDB.
     */
    @Test
    public void testRegisterClient() {
        System.out.println("registerClient");
        ClientManager instance = s.getClientManager();

        InetAddress address = InetAddress.getLoopbackAddress();

        try {
            Communicator result = instance.registerClient(address, PORT_CLIENT_1);

            assertNotNull(result);
            assertEquals(address, result.getAddress());
            assertEquals(PORT_CLIENT_1, result.getPort());
            assertEquals(1, instance.getClients().size());
        } catch (ConnectionException ex) {
            fail("Failed to register client on server - " + ex);
        }
    }

    /**
     * Test of deregisterClient method, of class ClientDB.
     */
    @Test
    public void testDeregisterClient() {
        System.out.println("deregisterClient");
        ClientManager instance = s.getClientManager();

        try {
            CommunicatorInner comm = (CommunicatorInner) instance.registerClient(InetAddress.getLoopbackAddress(), PORT_CLIENT_1);
            UUID id = UUID.randomUUID();
            comm.setTargetId(id);

            instance.registerClient(InetAddress.getLoopbackAddress(), PORT_CLIENT_2);

            assertEquals(2, instance.getClients().size());

            instance.deregisterClient(UUID.randomUUID());
            assertEquals(2, instance.getClients().size());

            instance.deregisterClient(id);
            assertEquals(1, instance.getClients().size());
        } catch (ConnectionException ex) {
            fail("Failed to register client on server - " + ex);
        }
    }

    /**
     * Test of getClient method, of class ClientDB.
     */
    @Test
    public void testGetClient_InetAddress_int() {
        System.out.println("getClient");
        InetAddress adress = InetAddress.getLoopbackAddress();
        ClientManager instance = s.getClientManager();

        try {
            final Communicator comm1 = instance.registerClient(adress, PORT_CLIENT_1);
            final Communicator comm2 = instance.registerClient(adress, PORT_CLIENT_2);

            assertEquals(comm2, instance.getClient(adress, PORT_CLIENT_2));

            assertNull(instance.getClient(adress, 0));
            assertNull(instance.getClient(null, PORT_CLIENT_1));
            assertNull(instance.getClient(null, 0));

            assertEquals(comm1, instance.getClient(adress, PORT_CLIENT_1));
        } catch (ConnectionException ex) {
            fail("Failed to register client on server - " + ex);
        }
    }

    /**
     * Test of getClient method, of class ClientDB.
     */
    @Test
    public void testGetClient_UUID() {
        System.out.println("getClient");
        ClientManager instance = s.getClientManager();

        try {
            CommunicatorInner comm = (CommunicatorInner) instance.registerClient(InetAddress.getLoopbackAddress(), PORT_CLIENT_1);

            instance.registerClient(InetAddress.getLoopbackAddress(), PORT_CLIENT_2);

            assertEquals(comm, instance.getClient(comm.getTargetId()));

            assertNull(instance.getClient(UUID.randomUUID()));
            assertNull(instance.getClient(null));
        } catch (ConnectionException ex) {
            fail("Failed to register client on server - " + ex);
        }
    }

    /**
     * Test of getClients method, of class ClientDB.
     */
    @Test
    public void testGetClients() {
        System.out.println("getClients");
        ClientManager instance = s.getClientManager();

        Communicator comm1 = null;
        Communicator comm2 = null;

        try {
            comm1 = instance.registerClient(InetAddress.getLoopbackAddress(), PORT_CLIENT_1);
            comm2 = instance.registerClient(InetAddress.getLoopbackAddress(), PORT_CLIENT_2);
        } catch (ConnectionException ex) {
            fail("Failed to register client on server - " + ex);
        }

        final Collection<Communicator> expResult = new HashSet(3);
        expResult.add(comm1);
        expResult.add(comm2);
        final Collection<Communicator> result = instance.getClients();
        assertEquals(expResult, result);
    }

    /**
     * Test of isIdAllowed method, of class ClientDB.
     */
    @Test
    public void testIsIdAllowed() {
        System.out.println("isIdAllowed");
        UUID id = UUID.randomUUID();
        ClientDB instance = (ClientDB) s.getClientManager();

        try {
            CommunicatorInner comm = (CommunicatorInner) instance.registerClient(InetAddress.getLoopbackAddress(), PORT_CLIENT_1);
            comm.setTargetId(id);
            assertTrue(instance.isIdAllowed(id));
            assertFalse(instance.isIdAllowed(UUID.randomUUID()));
        } catch (ConnectionException ex) {
            fail("Failed to register client on server - " + ex);
        }
    }
}