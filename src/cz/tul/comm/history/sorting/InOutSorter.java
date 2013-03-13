package cz.tul.comm.history.sorting;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Petr Ječmen
 */
public class InOutSorter extends HistorySorter {

    private static final String NODE_NAME = "IPSource";
    private static final String IP_DELIMITER = ".";
    private static final String IP_DELIMITER_REGEX = "[" + IP_DELIMITER + "]";
    private static final String IP_ZERO = "0";
    private static final int IP_PART_COUNT = 4;
    private static final int IP_PART_SIZE = 3;

    @Override
    public Element sortHistory(final Element rootElement, final Document doc) {
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


        final Element result = doc.createElement("History");
        final Element groupIn = doc.createElement("In");
        final Element groupOut = doc.createElement("Out");
        String normalizedLocalHost;
        try {
            normalizedLocalHost = normalizeIP(InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException ex) {
            normalizedLocalHost = normalizeIP(InetAddress.getLoopbackAddress().getHostAddress());
        }
        
        Element group;
        for (Map.Entry<Object, List<Node>> e : sortedNodes.entrySet()) {
            boolean out = e.getKey().equals(normalizedLocalHost);
            if (out) {
                group = groupOut;
            } else {
                group = groupIn;
            }
            for (Node n : e.getValue()) {
                group.appendChild(n);
            }
        }
        result.appendChild(groupIn);
        result.appendChild(groupOut);

        return result;
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
                    String ip = eInner.getTextContent();
                    result = normalizeIP(ip);
                }
            }
        }

        return result;
    }

    private static String normalizeIP(final String ip) {
        StringBuilder sb = new StringBuilder();

        String[] parts = ip.split(IP_DELIMITER_REGEX);
        if (parts.length == IP_PART_COUNT) {
            for (int i = 0; i < IP_PART_COUNT; i++) {
                for (int j = 0; j < IP_PART_SIZE - parts[i].length(); j++) {
                    sb.append(IP_ZERO);
                }
                sb.append(parts[i]);
                sb.append(IP_DELIMITER);
            }
            sb.setLength(sb.length() - 1);
        } else {
            sb.append(ip);
        }


        return sb.toString();
    }
}
