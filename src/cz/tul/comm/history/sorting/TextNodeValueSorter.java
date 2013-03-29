package cz.tul.comm.history.sorting;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Sorter for XML data according to given Node identified by name. Data will be
 * sorted according to text value of given node.
 *
 * @author Petr Ječmen
 */
public class TextNodeValueSorter extends HistorySorter {

    private static final Logger log = Logger.getLogger(TextNodeValueSorter.class.getName());
    private final String nodeName;

    /**
     * @param nodeName name of the node which will be used for sorting.
     */
    public TextNodeValueSorter(final String nodeName) {
        this.nodeName = nodeName;
    }

    @Override
    public Element sortHistory(final Element rootElement, final Document doc) {
        log.log(Level.INFO, "Sorting nodes by node {0}.", nodeName);
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

        return storeMapToNode(doc, sortedNodes, nodeName);
    }

    private String extractValue(final Node element) {
        String result = "";

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
