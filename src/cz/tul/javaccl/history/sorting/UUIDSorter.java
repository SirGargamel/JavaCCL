package cz.tul.javaccl.history.sorting;

import cz.tul.javaccl.history.HistoryRecord;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.logging.Logger;
import static org.omg.IOP.IORHelper.id;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Extracts messages and groups them according to their IDs.
 *
 * @author Petr Ječmen
 */
public class UUIDSorter extends HistorySorter {

    private static final Logger LOG = Logger.getLogger(UUIDSorter.class.getName());
    private static final String FIELD_NAME_UUID = "UUID";

    @Override
    public List<Element> sortHistory(final Collection<HistoryRecord> records, final Document doc) {
        LOG.fine("Sotring messages by UUID.");

        // group Nodes by UUID        
        final SortedMap<UUID, List<HistoryRecord>> idGroups = new TreeMap<UUID, List<HistoryRecord>>();
        List<HistoryRecord> list;
        Element element;
        NodeList nodeList;
        Node uuidNode;
        UUID uuid;
        for (HistoryRecord r : records) {
            element = r.getData();
            nodeList = element.getElementsByTagName(FIELD_NAME_UUID);
            if (nodeList.getLength() > 0) {
                uuidNode = nodeList.item(0);
                uuid = UUID.fromString(uuidNode.getFirstChild().getTextContent());
                list = idGroups.get(uuid);
                if (list == null) {
                    list = new ArrayList<HistoryRecord>();
                    idGroups.put(uuid, list);
                }
                list.add(r);
            }
        }

        final List<Element> result = new ArrayList<Element>(records.size());
        Element group;
        for (Entry<UUID, List<HistoryRecord>> e : idGroups.entrySet()) {
            group = doc.createElement("Group");
            group.setAttribute("UUID", e.getKey().toString());
            
            for (HistoryRecord r : e.getValue()) {
                group.appendChild(convertRecordToXML(r, doc));
            }

            result.add(group);
        }

        return result;
    }
}
