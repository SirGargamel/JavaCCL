package cz.tul.javaccl.history.export;

import cz.tul.javaccl.messaging.Message;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Export unit for class {@link Message}. Stores 3 nodes - UUID, header and
 * data. Data are exported using {@link Exporter} and its methods, so if you
 * want to have nice export for sent data, create and register export unit for
 * given data.
 *
 * @author Petr Ječmen
 */
public class ExportMessage extends ExportUnit {

    private static void appendStringDataToNode(final Node node, final Document doc, final String name, final String data) {
        final Element e = doc.createElement(name);
        e.appendChild(doc.createTextNode(data));
        node.appendChild(e);
    }

    @Override
    public Element exportData(final Document doc, final Object data) {
        Element result;
        if (data instanceof Message) {
            final Message msg = (Message) data;
            result = doc.createElement("Message");

            appendStringDataToNode(result, doc, "UUID", msg.getId().toString());
            appendStringDataToNode(result, doc, "Header", msg.getHeader());
            if (msg.getData() != null) {
                result.appendChild(new Exporter().exportData(doc, msg.getData()));
            }
        } else {
            result = doc.createElement("null");
        }

        return result;
    }

    @Override
    public Class<?> getExportedClass() {
        return Message.class;
    }
}
