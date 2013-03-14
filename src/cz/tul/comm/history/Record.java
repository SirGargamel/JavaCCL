/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.tul.comm.history;

import java.net.InetAddress;
import java.util.Date;

/**
 *
 * @author Petr Ječmen
 */
public class Record {
    
    private final InetAddress ipSource;
    private final InetAddress ipDestination;
    private final Object data;
    private final Date time;
    private final boolean accepted;

    Record(InetAddress ipSource, InetAddress ipDestination, Object data, final boolean accepted) {
        this.ipSource = ipSource;
        this.ipDestination = ipDestination;
        this.data = data;
        this.accepted = accepted;

        time = new Date();
    }    

    InetAddress getIpSource() {
        return ipSource;
    }

    InetAddress getIpDestination() {
        return ipDestination;
    }

    Object getData() {
        return data;
    }

    Date getTime() {
        return time;
    }

    boolean wasAccepted() {
        return accepted;
    }
}
