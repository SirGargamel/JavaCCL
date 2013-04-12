package cz.tul.comm.history.sorting;

import cz.tul.comm.history.Record;
import static cz.tul.comm.history.sorting.HistorySorter.convertRecordToXML;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Sorts messages in two grops - one group is filled with inbound messages, the
 * rest is in the other group.
 *
 * @author Petr Ječmen
 */
public class InOutSorter extends HistorySorter {

    private static final Logger log = Logger.getLogger(InOutSorter.class.getName());        

    @Override
    public List<Element> sortHistory(final Collection<Record> records, final Document doc) {
        log.fine("Sorting nodes by direction (In | Out).");

        final List<Element> result = new ArrayList<>(2);
        final Element groupIn = doc.createElement("In");
        final Element groupOut = doc.createElement("Out");

        InetAddress localHost;
        try {
            localHost = InetAddress.getLocalHost();
        } catch (UnknownHostException ex) {
            log.log(Level.FINE, "Could not obtain local IP address, using loopback.", ex);
            localHost = InetAddress.getLoopbackAddress();
        }

        Element e;
        for (Record r : records) {
            e = convertRecordToXML(r, doc);
            if (r.getIpSource().equals(localHost)) {
                groupIn.appendChild(e);
            } else {
                groupOut.appendChild(e);
            }
        }

        result.add(groupIn);
        result.add(groupOut);

        return result;
    }
}
