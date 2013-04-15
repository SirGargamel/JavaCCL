/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.tul.comm.server;

import cz.tul.comm.communicator.Communicator;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.UUID;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Petr Ječmen
 */
public class ClientDBTest {

    private static final int PORT = 5_253;

    public ClientDBTest() {
    }

    /**
     * Test of registerClient method, of class ClientDB.
     */
    @Test
    public void testRegisterClient() {
        final ClientDB instance = new ClientDB();

        InetAddress adress = null;
        int port = 50;
        Communicator result = instance.registerClient(adress, port);
        assertNull(result);

        adress = InetAddress.getLoopbackAddress();
        port = -50;
        result = instance.registerClient(adress, port);
        assertNull(result);

        adress = InetAddress.getLoopbackAddress();
        port = 50;
        result = instance.registerClient(adress, port);
        assertNotNull(result);
    }

    /**
     * Test of deregisterClient method, of class ClientDB.
     */
    @Test
    public void testDeregisterClient() {
        final ClientDB instance = new ClientDB();

        InetAddress adress = InetAddress.getLoopbackAddress();

        final Communicator c = instance.registerClient(adress, PORT);
        assertEquals(instance.getClients().size(), 1);
        instance.deregisterClient(UUID.randomUUID());
        assertEquals(instance.getClients().size(), 1);
        instance.deregisterClient(c.getId());
        assertEquals(instance.getClients().size(), 0);
    }

    /**
     * Test of getClient method, of class ClientDB.
     */
    @Test
    public void testGetClient() {
        final ClientDB instance = new ClientDB();

        InetAddress adress = InetAddress.getLoopbackAddress();

        Communicator expResult = instance.registerClient(adress, PORT);
        Communicator result = instance.getClient(adress, PORT);
        assertEquals(expResult, result);

    }

    /**
     * Test of getClients method, of class ClientDB.
     */
    @Test
    public void testGetClients() throws UnknownHostException {
        ClientDB instance = new ClientDB();
        Collection<Communicator> result = instance.getClients();
        assertEquals(0, result.size());
        try {

            final Communicator c1 = instance.registerClient(InetAddress.getByAddress(new byte[]{1, 1, 1, 1}), PORT);
            final Communicator c2 = instance.registerClient(InetAddress.getByAddress(new byte[]{0, 0, 0, 0}), PORT);
            result = instance.getClients();
            assertEquals(2, result.size());

            instance.deregisterClient(c1.getId());
            result = instance.getClients();
            assertEquals(1, result.size());

            instance.deregisterClient(c1.getId());
            result = instance.getClients();
            assertEquals(1, result.size());
        } catch (UnknownHostException ex) {
            fail(ex.getLocalizedMessage());
        }

    }
}