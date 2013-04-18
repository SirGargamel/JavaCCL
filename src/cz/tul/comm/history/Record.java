package cz.tul.comm.history;

import java.net.InetAddress;
import java.util.Date;

/**
 * Data class for storing info about message history. Time is time of logging,
 * not sending (logging is ussualy done right after the message has been sent so
 * the difference should be minimal).
 *
 * @author Petr Ječmen
 */
public class Record {

    private final InetAddress ipSource;
    private final InetAddress ipDestination;
    private final Object data;
    private final Date time;
    private final boolean accepted;

    Record(final InetAddress ipSource, final InetAddress ipDestination, Object data, final boolean accepted) {
        this.ipSource = ipSource;
        this.ipDestination = ipDestination;
        this.data = data;
        this.accepted = accepted;

        time = new Date();
    }

    public InetAddress getIpSource() {
        return ipSource;
    }

    public InetAddress getIpDestination() {
        return ipDestination;
    }

    public Object getData() {
        return data;
    }

    public Date getTime() {
        return time;
    }

    public boolean wasAccepted() {
        return accepted;
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        
        sb.append("From ");
        sb.append(ipSource.getHostAddress());
        sb.append(" to ");
        sb.append(ipDestination.getHostAddress());
        sb.append(" on ");
        sb.append(time);
        sb.append(". Data - ");
        sb.append(data.toString());
        sb.append(". Was accepted - ");
        sb.append(accepted);
        
        return sb.toString();
    }
}
