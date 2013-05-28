package cz.tul.comm.server;

import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.communicator.CommunicatorInner;
import java.net.InetAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Petr Jecmen
 */
public class ClientDBTest {

    public ClientDBTest() {
    }

    /**
     * Test of registerClient method, of class ClientDB.
     */
    @Test
    public void testRegisterClient() {
        System.out.println("registerClient");
        ClientDB instance = new ClientDB();

        InetAddress address = InetAddress.getLoopbackAddress();
        int port = 5000;
        Communicator result = instance.registerClient(address, port);

        assertEquals(address, result.getAddress());
        assertEquals(port, result.getPort());
        assertEquals(1, instance.getClients().size());
    }

    /**
     * Test of deregisterClient method, of class ClientDB.
     */
    @Test
    public void testDeregisterClient() {
        System.out.println("deregisterClient");
        ClientDB instance = new ClientDB();

        CommunicatorInner comm = (CommunicatorInner) instance.registerClient(InetAddress.getLoopbackAddress(), 5000);
        UUID id = UUID.randomUUID();
        comm.setTargetId(id);

        instance.registerClient(InetAddress.getLoopbackAddress(), 5001);

        assertEquals(2, instance.getClients().size());

        instance.deregisterClient(UUID.randomUUID());
        assertEquals(2, instance.getClients().size());

        instance.deregisterClient(id);
        assertEquals(1, instance.getClients().size());
    }

    /**
     * Test of getClient method, of class ClientDB.
     */
    @Test
    public void testGetClient_InetAddress_int() {
        System.out.println("getClient");
        InetAddress adress = InetAddress.getLoopbackAddress();
        int port = 5000;
        ClientDB instance = new ClientDB();

        Communicator c1 = instance.registerClient(adress, port);

        port++;
        Communicator c2 = instance.registerClient(adress, port);

        assertEquals(c2, instance.getClient(adress, port));

        assertNull(instance.getClient(adress, 0));
        assertNull(instance.getClient(null, port));
        assertNull(instance.getClient(null, 0));

        port--;
        assertEquals(c1, instance.getClient(adress, port));
    }

    /**
     * Test of getClient method, of class ClientDB.
     */
    @Test
    public void testGetClient_UUID() {
        System.out.println("getClient");
        UUID id = UUID.randomUUID();
        ClientDB instance = new ClientDB();

        CommunicatorInner c1 = (CommunicatorInner) instance.registerClient(InetAddress.getLoopbackAddress(), 5000);
        c1.setTargetId(id);
        Communicator c2 = instance.registerClient(InetAddress.getLoopbackAddress(), 5001);

        assertEquals(c1, instance.getClient(id));        

        assertNull(instance.getClient(UUID.randomUUID()));
        assertNull(instance.getClient(null));
    }

    /**
     * Test of getClients method, of class ClientDB.
     */
    @Test
    public void testGetClients() {
        System.out.println("getClients");
        ClientDB instance = new ClientDB();
        
        Communicator c1 = instance.registerClient(InetAddress.getLoopbackAddress(), 5000);
        Communicator c2 = instance.registerClient(InetAddress.getLoopbackAddress(), 5000);
        Communicator c3 = instance.registerClient(InetAddress.getLoopbackAddress(), 5001);
        
        Collection<Communicator> expResult = new HashSet(2);
        expResult.add(c1);
        expResult.add(c3);
        Collection<Communicator> result = instance.getClients();
        assertEquals(expResult, result);
    }

    /**
     * Test of isIdAllowed method, of class ClientDB.
     */
    @Test
    public void testIsIdAllowed() {
        System.out.println("isIdAllowed");
        UUID id = UUID.randomUUID();
        ClientDB instance = new ClientDB();
        CommunicatorInner comm = (CommunicatorInner) instance.registerClient(InetAddress.getLoopbackAddress(), 5000);
        comm.setTargetId(id);
        assertTrue(instance.isIdAllowed(id));
        assertFalse(instance.isIdAllowed(UUID.randomUUID()));
    }
}