package cz.tul.comm;

import cz.tul.comm.persistence.SerializationUtils;
import java.io.File;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Petr Ječmen
 */
public class SerializationUtilsTest {

    private static final String TEST_FILE_NAME = "text.tst";
    private static File testFile;

    @BeforeClass
    public static void setUpClass() {
        testFile = new File(TEST_FILE_NAME);
        if (testFile.exists()) {
            testFile.delete();
        }
    }

    @AfterClass
    public static void tearDownClass() {
        if (testFile != null && testFile.exists()) {
            testFile.delete();
        }
    }

        public SerializationUtilsTest() {
    }

    /**
     * Test of saveItemToDisc method, of class SerializationUtils.
     */
    @Test
    public void testSaveAndLoadItemToDisc() throws Exception {
        final String data = "Serialization tester";
        if (SerializationUtils.saveItemToDisc(testFile, data)) {
            final Object result = SerializationUtils.loadItemFromDisc(testFile);
            assertEquals(result, data);
        } else {
            fail("Could not serialize data to disc");
        }
    }

    /**
     * Test of saveItemToDiscAsXML method, of class SerializationUtils.
     */
    @Test
    public void testSaveAndLoadItemToDiscAsXML() {
        final String data = "Serialization tester";
        if (SerializationUtils.saveItemToDiscAsXML(testFile, data)) {
            final Object result = SerializationUtils.loadXMLItemFromDisc(testFile);
            assertEquals(result, data);
        } else {
            fail("Could not XML serialize data to disc");
        }

    }
}