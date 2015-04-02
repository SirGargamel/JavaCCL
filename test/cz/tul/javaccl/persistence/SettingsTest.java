package cz.tul.javaccl.persistence;

import cz.tul.javaccl.GlobalConstants;
import cz.tul.javaccl.client.Client;
import cz.tul.javaccl.client.ClientImpl;
import cz.tul.javaccl.client.ServerInterface;
import cz.tul.javaccl.exceptions.ConnectionException;
import cz.tul.javaccl.server.Server;
import cz.tul.javaccl.server.ServerImpl;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Petr Jecmen
 */
public class SettingsTest {

    private static final String TEST_FILE_NAME = "test.xml";
    private static final int CLIENT_PORT = 5253;
    private static Server s;
    private static Client c;
    private static File serializationTarget;    

    @BeforeClass
    public static void setUpClass() {
        serializationTarget = new File(System.getProperty("user.home") + File.separator + TEST_FILE_NAME);

        try {
            if (!serializationTarget.exists()) {
                serializationTarget.createNewFile();
            }
            if (!serializationTarget.canWrite() || !serializationTarget.canRead()) {
                fail("Test file is not accessible.");
            }
        } catch (IOException ex) {
            fail("Test file could not be created.");
        }

    }

    @AfterClass
    public static void tearDownClass() {
        serializationTarget.delete();
    }

    @Before
    public void setUp() {        
        try {            
            c = ClientImpl.initNewClient(CLIENT_PORT);
            s = ServerImpl.initNewServer();
        } catch (IOException ex) {
            fail("Initialization failed.");
        }
    }

    @After
    public void tearDown() {
        c.stopService();
        s.stopService();
    }

    @Test
    public void testDeserializeServer() {
        System.out.println("deserializeServer");
        boolean result = ServerSettings.deserialize(new File(SettingsTest.class.getResource("testSettingsServer.xml").getFile()), s.getClientManager());
        assertEquals(true, result);

        result = ServerSettings.deserialize(new File(SettingsTest.class.getResource("testSettingsEmpty.xml").getFile()), s.getClientManager());
        assertEquals(false, result);

        result = ServerSettings.deserialize(new File(SettingsTest.class.getResource("testSettingsServerFail.xml").getFile()), s.getClientManager());
        assertEquals(false, result);
    }

    @Test
    public void testSerializeServer() {
        System.out.println("serializeServer");
        s.stopService();
        s = ServerImpl.initNewServer();
        try {
            s.getClientManager().registerClient(GlobalConstants.IP_LOOPBACK, CLIENT_PORT);
        } catch (IllegalArgumentException ex) {
            fail("Illegal arguments used");
        } catch (ConnectionException ex) {
            fail("Could not connect to client");
        }

        boolean result = ServerSettings.serialize(serializationTarget, s.getClientManager());
        assertEquals(true, result);

        assertFiles(new File(SettingsTest.class.getResource("testSettingsServer.xml").getFile()), serializationTarget);
    }

    @Test
    public void testDeserializeClient() {
        System.out.println("deserializeClient");
        boolean result = ClientSettings.deserialize(new File(SettingsTest.class.getResource("testSettingsClient.xml").getFile()), (ServerInterface) c);
        assertEquals(true, result);

        result = ClientSettings.deserialize(new File(SettingsTest.class.getResource("testSettingsClientIpOnly.xml").getFile()), (ServerInterface) c);
        assertEquals(true, result);

        result = ClientSettings.deserialize(new File(SettingsTest.class.getResource("testSettingsEmpty.xml").getFile()), (ServerInterface) c);
        assertEquals(false, result);

        result = ClientSettings.deserialize(new File(SettingsTest.class.getResource("testSettingsClientFail.xml").getFile()), (ServerInterface) c);
        assertEquals(false, result);
        
        result = ClientSettings.deserialize(new File(SettingsTest.class.getResource("testSettingsClientMultiIP.xml").getFile()), (ServerInterface) c);
        assertEquals(true, result);
    }

    @Test
    public void testSerializeClient() {
        System.out.println("serializeClient");
        c.stopService();
        try {
            c = ClientImpl.initNewClient(CLIENT_PORT);
            c.registerToServer(GlobalConstants.IP_LOOPBACK);
        } catch (IllegalArgumentException ex) {
            fail("Illegal arguments used");
        } catch (ConnectionException ex) {
            fail("Could not connect to client");
        } catch (Exception ex) {
            fail("Failed to initialize client.");
        }

        boolean result = ClientSettings.serialize(serializationTarget, c.getServerComm());
        assertEquals(true, result);

        assertFiles(new File(SettingsTest.class.getResource("testSettingsClient.xml").getFile()), serializationTarget);
    }

    private static void assertFiles(final File expected,
            final File actual) {
        String actualLine, expectedLine;
        BufferedReader expectedR = null, actualR = null;
        try {
            expectedR = new BufferedReader(new FileReader(expected));
            actualR = new BufferedReader(new FileReader(actual));
            while ((expectedLine = expectedR.readLine()) != null) {
                actualLine = actualR.readLine();
                assertNotNull("Expected had more lines then the actual.", actualLine);
                assertEquals(expectedLine.trim(), actualLine.trim());
            }
            assertNull("Actual had more lines then the expected.", actualR.readLine());
        } catch (IOException ex) {
            fail("Could not access tested files - " + ex.getLocalizedMessage());
        } finally {
            if (expectedR != null) {
                try {
                    expectedR.close();
                } catch (IOException ex) {
                    Logger.getLogger(SettingsTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (actualR != null) {
                try {
                    actualR.close();
                } catch (IOException ex) {
                    Logger.getLogger(SettingsTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}