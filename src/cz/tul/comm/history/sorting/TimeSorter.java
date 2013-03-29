package cz.tul.comm.history.sorting;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Sort XML data according to registration time in ascending order.
 *
 * @author Petr Ječmen
 */
public class TimeSorter extends HistorySorter {

    private static final Logger log = Logger.getLogger(TimeSorter.class.getName());
    private static final String NODE_NAME = "Time";

    @Override
    public Element sortHistory(final Element rootElement, final Document doc) {
        log.fine("Sorting nodes by time.");
        SortedMap<Object, List<Node>> sortedNodes = new TreeMap<>();

        final NodeList nl = rootElement.getChildNodes();
        Node nd;
        String key;
        for (int i = 0; i < nl.getLength(); i++) {
            nd = nl.item(i);
            key = extractValue(nd);
            if (key != null) {
                List<Node> l = sortedNodes.get(key);
                if (l == null) {
                    l = new ArrayList<>();
                    sortedNodes.put(key, l);
                }
                l.add(nd);
            }
        }

        return storeMapToNode(doc, sortedNodes, NODE_NAME);
    }

    private String extractValue(final Node element) {
        String result = "";

        if (element instanceof Element) {
            final Element e = (Element) element;
            NodeList nl = e.getElementsByTagName(NODE_NAME);
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
