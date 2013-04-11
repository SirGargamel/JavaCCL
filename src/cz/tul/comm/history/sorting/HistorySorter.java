package cz.tul.comm.history.sorting;

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

    /**
     * Sort child {@link Element}s and sort them according to given parameter.
     *
     * @param rootElement element containing data for sorting
     * @param doc target XML document
     * @return New History element, in which all children are sorted.
     */
    public abstract Element sortHistory(final Element rootElement, final Document doc);

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
    
    protected String extractValue(final Node element, final String nodeName) {
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
