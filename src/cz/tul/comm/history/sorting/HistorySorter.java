package cz.tul.comm.history.sorting;

import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 *
 * @author Petr Ječmen
 */
public abstract class HistorySorter {

    public abstract Element sortHistory(final Element rootElement, final Document doc);

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
}
