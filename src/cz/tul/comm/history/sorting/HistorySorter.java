package cz.tul.comm.history.sorting;

import cz.tul.comm.history.HistoryRecord;
import cz.tul.comm.history.export.Exporter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Abstract class defining interface for history sorters, which will sort XML
 * nodes according to given parameter.
 *
 * @author Petr Ječmen
 */
public abstract class HistorySorter {

    private static final String TIME_PATTERN = "yyyy-MM-dd H:m:s:S z";
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat(TIME_PATTERN);

    /**
     * Convert history record to a XML representation, data are exported using
     * aviable export units.
     *
     * @param r record for export
     * @param doc target XML document
     * @return XML representation as element
     */
    protected static Element convertRecordToXML(final HistoryRecord r, final Document doc) {
        final Element result = doc.createElement("Record");

        appendStringDataToNode(result, doc, "IPSource", r.getIpSource().getHostAddress());
        appendStringDataToNode(result, doc, "IPDestination", r.getIpDestination().getHostAddress());
        appendStringDataToNode(result, doc, "Time", DATE_FORMAT.format(r.getTime()));
        appendStringDataToNode(result, doc, "Accepted", String.valueOf(r.wasAccepted()));

        result.appendChild(Exporter.exportObject(r.getData(), doc));
        result.appendChild(Exporter.exportObject(r.getAnswer(), doc));

        return result;
    }

    private static void appendStringDataToNode(final Node n, final Document d, final String name, final String data) {
        final Element e = d.createElement(name);
        e.appendChild(d.createTextNode(data));
        n.appendChild(e);
    }

    /**
     * Sort child {@link Element}s and sort them according to given parameter.
     *
     * @param records
     * @param doc target XML document
     * @return New History element, in which all children are sorted.
     */
    public abstract List<Element> sortHistory(final Collection<HistoryRecord> records, final Document doc);

    /**
     * Take {@link SortedMap} filled with data and export it to XML document.
     *
     * @param doc target XML document
     * @param data {@link SortedMap} filled with data
     * @param idName name of the ID used for sorting.
     * @return Element caled "History" filled with sorted data.
     */
    protected Element storeMapToNode(final Document doc, SortedMap<Object, List<Node>> data, final String idName) {
        final Element result = doc.createElement("History");

        Element group;
        List<Node> ln;
        for (Entry<Object, List<Node>> e : data.entrySet()) {
            ln = e.getValue();
            if (ln.size() == 1) {
                result.appendChild(ln.get(0));
            } else {
                group = doc.createElement("Group");
                group.setAttribute(idName, e.getKey().toString());

                for (Node n : ln) {
                    group.appendChild(n);
                }

                result.appendChild(group);
            }
        }
        return result;
    }

    /**
     * Extra value of subnode with given nade
     *
     * @param element source lement
     * @param nodeName target node name
     * @return extracted value (or null if node with given name does not exist)
     */
    protected String extractValue(final Node element, final String nodeName) {
        String result = null;

        if (element instanceof Element) {
            final Element e = (Element) element;
            NodeList nl = e.getElementsByTagName(nodeName);
            if (nl.getLength() > 0) {
                Node n = nl.item(0);
                if (n instanceof Element) {
                    final Element eInner = (Element) n;
                    result = eInner.getTextContent();
                }
            }
        }

        return result;
    }
}
