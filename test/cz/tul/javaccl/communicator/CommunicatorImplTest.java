/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.tul.javaccl.communicator;

import cz.tul.javaccl.Constants;
import cz.tul.javaccl.exceptions.ConnectionException;
import java.awt.image.BufferedImage;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Petr Jecmen
 */
public class CommunicatorImplTest {
    
    @Test
    public void testInitNewCommunicator() throws UnknownHostException {
        System.out.println("initNewCommunicator");
        Object result;        
        
        try {
            result = CommunicatorImpl.initNewCommunicator(InetAddress.getByName(Constants.IP_LOOPBACK), -1);
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
    public void testUnserializableDataSend() throws UnknownHostException {
        System.out.println("testUnserializableDataSend");
        final Communicator comm = CommunicatorImpl.initNewCommunicator(InetAddress.getByName(Constants.IP_LOOPBACK), 0);
        
        try {
            comm.sendData(new BufferedImage(1, 1, 1));
            fail("Should have failed because of illegal port");        
        } catch (ConnectionException ex) {
            // expected
        }
    }
}