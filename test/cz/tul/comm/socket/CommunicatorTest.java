/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.tul.comm.socket;

import cz.tul.comm.Constants;
import cz.tul.comm.communicator.Communicator;
import java.io.IOException;
import java.net.InetAddress;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Petr Ječmen
 */
public class CommunicatorTest {
    
    private static final int TIMEOUT = 1000;
    private static ServerSocket ss;

    public CommunicatorTest() {
    }

    @BeforeClass
    public static void setUpClass() throws IOException {
        ss = ServerSocket.createServerSocket(Constants.DEFAULT_PORT);
    }

    @AfterClass
    public static void tearDownClass() {        
        ss.stopService();
    }

    /**
     * Test of getAddress method, of class Communicator.
     */
    @Test
    public void testCommunicator() {
        System.out.println("Communicator");

        final InetAddress ip = InetAddress.getLoopbackAddress();
        
        Communicator instance = Communicator.initNewCommunicator(ip, -1);
        assertNull(instance);
        
        instance = Communicator.initNewCommunicator(ip, Constants.DEFAULT_PORT);
        assertEquals(ip, instance.getAddress());               

        final boolean result = instance.sendData("test", TIMEOUT);
        assertTrue("Target could not receive data", result);
    }
}