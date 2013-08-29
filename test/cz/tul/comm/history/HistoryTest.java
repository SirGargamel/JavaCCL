/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.tul.comm.history;

import cz.tul.comm.GenericResponses;
import cz.tul.comm.client.ClientImpl;
import cz.tul.comm.exceptions.ConnectionException;
import cz.tul.comm.history.export.ExportMessage;
import cz.tul.comm.history.sorting.DefaultSorter;
import cz.tul.comm.history.sorting.IPSorter;
import cz.tul.comm.history.sorting.InOutSorter;
import cz.tul.comm.history.sorting.UUIDSorter;
import cz.tul.comm.messaging.Message;
import cz.tul.comm.messaging.SystemMessageHeaders;
import cz.tul.comm.server.Server;
import cz.tul.comm.server.ServerImpl;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import org.junit.AfterClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;

/**
 *
 * @author Petr Jecmen
 */
public class HistoryTest {

    private static final String TEST_FILE_NAME = "export.xml";
    private static final String COMPARE_FILE_NAME = "exportCompare.xml";
    private static final String IP_SOURCE_TEMPLATE = "<IPSource></IPSource>";
    private static final String IP_DESTINATION_TEMPLATE = "<IPDestination></IPDestination>";
    private static File exportTarget, compareTarget;

    public HistoryTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        exportTarget = new File(System.getProperty("user.home") + File.separator + TEST_FILE_NAME);
        compareTarget = new File(System.getProperty("user.home") + File.separator + COMPARE_FILE_NAME);
    }

    @AfterClass
    public static void tearDownClass() {
        exportTarget.delete();
        compareTarget.delete();
    }

    @Test
    public void testHistoryDirectLogging() {
        System.out.println("logHistoryDirectLoggin");

        InetAddress ipLocal = InetAddress.getLoopbackAddress();

        HistoryManager h = new History();


        synchronized (this) {
            try {
                h.logMessageSend(ipLocal, "dataOut", true, GenericResponses.OK);
                this.wait(100);
                h.logMessageReceived(ipLocal, GenericResponses.ILLEGAL_DATA, true, "0000-0000");
                this.wait(250);
                h.logMessageReceived(ipLocal, "dataIn2", false, "response");
            } catch (InterruptedException ex) {
                fail("Waiting interrupted");
            }
        }

        prepareFile(exportTarget);
        prepareFile(compareTarget);
        assertTrue(h.export(exportTarget, null));
        prepareLocalIp(new File(HistoryTest.class.getResource("testExportDefault.xml").getFile()), compareTarget);
        assertFiles(compareTarget, exportTarget);

        prepareFile(exportTarget);
        prepareFile(compareTarget);
        assertTrue(h.export(exportTarget, new DefaultSorter()));
        prepareLocalIp(new File(HistoryTest.class.getResource("testExportDefault.xml").getFile()), compareTarget);
        assertFiles(compareTarget, exportTarget);

        prepareFile(exportTarget);
        prepareFile(compareTarget);
        assertTrue(h.export(exportTarget, new IPSorter(true)));
        prepareLocalIp(new File(HistoryTest.class.getResource("testExportIpTrue.xml").getFile()), compareTarget);
        assertFiles(compareTarget, exportTarget);

        prepareFile(exportTarget);
        prepareFile(compareTarget);
        assertTrue(h.export(exportTarget, new IPSorter(false)));
        prepareLocalIp(new File(HistoryTest.class.getResource("testExportIpFalse.xml").getFile()), compareTarget);
        assertFiles(compareTarget, exportTarget);

        prepareFile(exportTarget);
        prepareFile(compareTarget);
        assertTrue(h.export(exportTarget, new InOutSorter()));
        prepareLocalIp(new File(HistoryTest.class.getResource("testExportInOut.xml").getFile()), compareTarget);
        assertFiles(compareTarget, exportTarget);
    }

    @Test
    public void testHistoryServerAndClient() {
        System.out.println("logHistoryServerAndClient");

        InetAddress ipLocal = InetAddress.getLoopbackAddress();
        UUID clientUuid = UUID.fromString("550e8400-e29b-41d4-a716-446655449999");
        UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID uuid2 = UUID.fromString("550e8400-e29b-41d4-a716-446655440111");

        final Server s = (ServerImpl) ServerImpl.initNewServer();
        final ClientImpl c;
        try {
            c = (ClientImpl) ClientImpl.initNewClient(5253);

            c.setServerInfo(ipLocal, 5252, clientUuid);
            s.getClientManager().addClient(ipLocal, 5253, clientUuid);

            synchronized (this) {
                try {
                    s.getClient(ipLocal).sendData(new Message(uuid, "Header", "data1"));
                    this.wait(10);
                    c.sendDataToServer(new Message(uuid, "Header", "dataBack"));
                    this.wait(25);
                    s.getClient(ipLocal).sendData(new Message(uuid2, "Header", "data2"));
                    this.wait(25);
                } catch (InterruptedException ex) {
                    fail("Waiting interrupted");
                } catch (ConnectionException ex) {
                    fail("Failed to deliver message - " + ex);
                }
            }

            s.getHistory().enable(false);
            
            c.stopService();
            s.stopService();

            History h = (History) s.getHistory();
            cleanUp(h.getRecords());
            h.registerExporter(new ExportMessage());

            prepareFile(exportTarget);
            prepareFile(compareTarget);
            assertTrue(s.getHistory().export(exportTarget, new UUIDSorter()));
            prepareLocalIp(new File(HistoryTest.class.getResource("testExportUUID.xml").getFile()), compareTarget);
            assertFiles(compareTarget, exportTarget);
        } catch (IOException ex) {
            fail("Failed to initialize clients.");
        }


    }

    private static void assertFiles(final File expected,
            final File actual) {
        String actualLine, expectedLine;
        try (BufferedReader expectedR = new BufferedReader(new FileReader(expected))) {
            try (BufferedReader actualR = new BufferedReader(new FileReader(actual))) {
                while ((expectedLine = expectedR.readLine()) != null) {
                    actualLine = actualR.readLine();
                    assertNotNull("Expected had more lines then the actual.", actualLine);

                    if (!expectedLine.trim().startsWith("<Time>")) {
                        assertEquals(expectedLine.trim(), actualLine.trim());
                    }
                }
                assertNull("Actual had more lines then the expected.", actualR.readLine());
            }
        } catch (IOException ex) {
            fail("Could not access tested files - " + ex.getLocalizedMessage());
        }
    }

    private static void prepareLocalIp(final File fileIn, final File fileOut) {
        // insert local IP at right spots
        try {
            final String ipSource = "<IPSource>" + InetAddress.getLocalHost().getHostAddress() + "</IPSource>";
            final String ipDest = "<IPDestination>" + InetAddress.getLocalHost().getHostAddress() + "</IPDestination>";

            String line;
            try (BufferedReader in = new BufferedReader(new FileReader(fileIn))) {
                try (BufferedWriter out = new BufferedWriter(new FileWriter(fileOut))) {
                    while (in.ready()) {
                        line = in.readLine().trim();
                        switch (line) {
                            case IP_SOURCE_TEMPLATE:
                                out.append(ipSource);
                                break;
                            case IP_DESTINATION_TEMPLATE:
                                out.append(ipDest);
                                break;
                            default:
                                out.append(line);
                                break;
                        }
                        out.newLine();
                    }
                }
            }
        } catch (UnknownHostException ex) {
            fail("Cannot obtain local IP.");
        } catch (IOException ex) {
            fail("Failed to prepare file " + fileIn + " for comparison.");
        }
    }

    private static void prepareFile(final File f) {
        // make sure that the file is accessible
        try {
            if (f.exists()) {
                f.delete();
            }
            f.createNewFile();
            if (!f.canWrite() || !f.canRead()) {
                fail("Test files are not accessible.");
            }
        } catch (IOException ex) {
            fail("Test file could not be created.");
        }
    }

    private static void cleanUp(List<HistoryRecord> records) {
        // remove status checks, logins etc.
        Iterator<HistoryRecord> it = records.iterator();

        HistoryRecord hr;
        Message m;
        Object data;
        while (it.hasNext()) {
            hr = it.next();

            data = hr.getData();
            if (data instanceof Message) {
                m = (Message) data;
                if (m.getHeader().equals(SystemMessageHeaders.STATUS_CHECK)
                        || m.getHeader().equals(SystemMessageHeaders.LOGIN)) {
                    it.remove();
                }
            }
        }
    }
}