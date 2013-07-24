/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.tul.comm;

import cz.tul.comm.messaging.Message;
import java.awt.image.BufferedImage;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Petr Jecmen
 */
public class UtilsTest {

    public UtilsTest() {
    }

    /**
     * Test of checkSerialization method, of class Utils.
     */
    @Test
    public void testCheckSerialization() {
        System.out.println("checkSerialization");
        
        Object data = "testString";
        assertTrue(Utils.checkSerialization(data));
        
        data = null;
        assertTrue(Utils.checkSerialization(data));

        data = new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR);
        assertFalse(Utils.checkSerialization(data));

        data = new Message("header", new byte[10][10]);
        assertTrue(Utils.checkSerialization(data));
        
        data = new Message("header", new Dummy());
        assertTrue(Utils.checkSerialization(data));

        try {
            data = new Message("header", new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR));
            fail("BufferedImage cannot be serialized, should have failed.");
        } catch (IllegalArgumentException ex) {
        }
        
        try {
            data = new Message("header", new Dummy2());
            fail("BufferedImage cannot be serialized, should have failed.");
        } catch (IllegalArgumentException ex) {
        }
    }
    
    private class Dummy implements Externalizable {

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            // do nothing
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        
    }
    
    private class Dummy2 implements Serializable {
        private final BufferedImage img = new BufferedImage(1, 1, 1);
    }
}