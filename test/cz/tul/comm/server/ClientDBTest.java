/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.tul.comm.server;

import cz.tul.comm.communicator.Communicator;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Petr Ječmen
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
        System.out.println("deregisterClient");
        final ClientDB instance = new ClientDB();


        InetAddress adress = InetAddress.getLoopbackAddress();
        int port = 50;

        instance.registerClient(adress, port);
        assertEquals(instance.getClients().size(), 1);
        try {
            instance.deregisterClient(InetAddress.getByAddress(new byte[]{1, 1, 1, 1}));
            assertEquals(instance.getClients().size(), 1);
        } catch (UnknownHostException ex) {
            System.err.println("Error creating address from IP, deregistering not fully tested.\n" + ex.getLocalizedMessage());
        }

        instance.deregisterClient(adress);
        assertEquals(instance.getClients().size(), 0);
    }

    /**
     * Test of getClient method, of class ClientDB.
     */
    @Test
    public void testGetClient() {
        System.out.println("getClient");
        final ClientDB instance = new ClientDB();

        InetAddress adress = InetAddress.getLoopbackAddress();
        int port = 50;

        Communicator expResult = instance.registerClient(adress, port);
        Communicator result = instance.getClient(adress);
        assertEquals(expResult, result);

    }

    /**
     * Test of getClients method, of class ClientDB.
     */
    @Test
    public void testGetClients() throws UnknownHostException {
        System.out.println("getClients");
        ClientDB instance = new ClientDB();
        Set result = instance.getClients();
        assertEquals(0, result.size());
        try {
            instance.registerClient(InetAddress.getByAddress(new byte[]{1, 1, 1, 1}), 50);
            instance.registerClient(InetAddress.getByAddress(new byte[]{0, 0, 0, 0}), 50);
            result = instance.getClients();
            assertEquals(2, result.size());

            instance.deregisterClient(InetAddress.getByAddress(new byte[]{1, 1, 1, 1}));
            result = instance.getClients();
            assertEquals(1, result.size());

            instance.deregisterClient(InetAddress.getByAddress(new byte[]{1, 1, 1, 1}));
            result = instance.getClients();
            assertEquals(1, result.size());
        } catch (UnknownHostException ex) {
            fail(ex.getLocalizedMessage());
        }

    }
}