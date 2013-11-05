package cz.tul.javaccl.history;

import cz.tul.javaccl.history.export.Exporter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Logger;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Data class for storing info about message history. Time is time of logging,
 * not sending (logging is ussualy done right after the message has been sent so
 * the difference should be minimal).
 *
 * @author Petr Ječmen
 */
public class HistoryRecord {

    private static final Logger log = Logger.getLogger(HistoryRecord.class.getName());
    private final InetAddress ipSource;
    private final InetAddress ipDestination;
    private final UUID id;
    private final Element data;
    private final Element answer;
    private final Date time;
    private final boolean accepted;
    private final boolean send;

    HistoryRecord(final InetAddress ipSource, final InetAddress ipDestination, final UUID sourceId, final Object data, final boolean accepted, final Object answer, final Document doc, final boolean send) {
        this.ipSource = ipSource;
        this.ipDestination = ipDestination;
        this.id = sourceId;
        this.data = Exporter.exportObject(data, doc);
        this.answer = Exporter.exportObject(answer, doc);
        this.accepted = accepted;
        this.send = send;

        time = new Date();
    }

    /**
     * @return ip of sender
     */
    public InetAddress getIpSource() {
        return ipSource;
    }

    /**
     * @return ip of target
     */
    public InetAddress getIpDestination() {
        return ipDestination;
    }

    /**
     *
     * @return iD of other side
     */
    public UUID getId() {
        return id;
    }

    /**
     * @return sent data
     */
    public Element getData() {
        return data;
    }

    /**
     * @return response to message
     */
    public Element getAnswer() {
        return answer;
    }

    /**
     * @return time of logging
     */
    public Date getTime() {
        return time;
    }

    /**
     * @return true if target confirmed data receive (read and conversion)
     */
    public boolean wasAccepted() {
        return accepted;
    }

    /**
     *
     * @return true if the message was sent to someone, if false, then the
     * message has been received
     */
    public boolean isSend() {
        return send;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("From ");
        if (ipSource != null) {
            sb.append(ipSource.getHostAddress());
            if (!send) {
                sb.append(", ");
                sb.append(id);
            }
        } else {
            sb.append("NULL");
        }
        sb.append(" to ");
        if (ipDestination != null) {
            sb.append(ipDestination.getHostAddress());
            if (send) {
                sb.append(", ");
                sb.append(id);
            }
        } else {
            sb.append("NULL");
        }
        sb.append(" on ");
        sb.append(time);
        sb.append(". Data - ");
        sb.append(nodeToString(data));
        if (accepted) {
            sb.append(". Was accepted with answer - ");
            sb.append(nodeToString(answer));
        } else {
            sb.append(". The message hasn't been accepted.");
        }

        return sb.toString();
    }

    private static String nodeToString(Node node) {
        String result;
        if (node != null) {
            StringWriter sw = new StringWriter();
            try {
                Transformer t = TransformerFactory.newInstance().newTransformer();
                t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                t.transform(new DOMSource(node), new StreamResult(sw));
            } catch (TransformerException te) {
                log.warning("nodeToString Transformer Exception");
            }
            result = sw.toString();
            result = result.replaceAll("<", "[").replaceAll(">", "]");
        } else {
            result = "null";
        }
        return result;
    }
}
