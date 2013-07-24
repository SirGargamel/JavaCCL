/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.tul.comm.communicator;

import cz.tul.comm.exceptions.ConnectionException;
import java.awt.image.BufferedImage;
import java.net.InetAddress;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Petr Jecmen
 */
public class CommunicatorImplTest {
    
    @Test
    public void testInitNewCommunicator() {
        System.out.println("initNewCommunicator");
        Object result;        
        
        try {
            result = CommunicatorImpl.initNewCommunicator(InetAddress.getLoopbackAddress(), -1);
            fail("Should have failed because of illegal port");
        } catch (IllegalArgumentException ex) {
            // expected
        }
        
        try {
            result = CommunicatorImpl.initNewCommunicator(null, 1);
            fail("Should have failed because of illegal port");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }
    
    @Test
    public void testUnserializableDataSend() {
        System.out.println("testUnserializableDataSend");
        final Communicator comm = CommunicatorImpl.initNewCommunicator(InetAddress.getLoopbackAddress(), 0);
        
        try {
            comm.sendData(new BufferedImage(1, 1, 1));
            fail("Should have failed because of illegal port");
        } catch (IllegalArgumentException ex) {
            // expected
        } catch (ConnectionException ex) {
            fail("This point should not have been reached.");
        }
    }
}