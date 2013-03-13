package cz.tul.comm.history;

import cz.tul.comm.history.export.ExportMessage;
import cz.tul.comm.history.export.Exporter;
import cz.tul.comm.history.export.IExportUnit;
import cz.tul.comm.history.sorting.HistorySorter;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 *
 * @author Gargamel
 */
public class History {

    private static final Logger log = Logger.getLogger(History.class.getName());
    private static final String TIME_PATTERN = "yyyy-MM-dd H:m:s:S z";
    private static final DateFormat df = new SimpleDateFormat(TIME_PATTERN);
    private static final Set<Record> records;
    private static final InetAddress localHost;
    private static final Exporter exporter;

    static {
        records = Collections.synchronizedSet(new HashSet<Record>());

        InetAddress local;
        try {
            local = InetAddress.getLocalHost();
        } catch (UnknownHostException ex) {
            log.log(Level.CONFIG, "Local address not found, using loopback", ex);
            local = InetAddress.getLoopbackAddress();
        }
        localHost = local;

        exporter = new Exporter();
        exporter.registerExporter(new ExportMessage());
    }

    public static void logMessageSend(final InetAddress ipDestination, final Object data, final boolean accepted) {
        records.add(new Record(localHost, ipDestination, data, accepted));
    }

    public static void logMessageReceived(final InetAddress ipSource, final Object data, final boolean accepted) {
        records.add(new Record(ipSource, localHost, data, accepted));
    }

    public static boolean export(final File target, final HistorySorter sorter) {
        boolean result = false;

        try {
            final Document doc = prepareDocument();          
            final Element rootElement = createRootElement(doc);

            if (sorter != null) {
                final Element newRootElement = sorter.sortHistory(rootElement, doc);
                doc.replaceChild(newRootElement, rootElement);
            }

            exportDocumentToXML(target, doc);
            result = true;
        } catch (ParserConfigurationException ex) {
            log.log(Level.WARNING, "Failed to create DocumentBuilder", ex);
        } catch (TransformerException ex) {
            log.log(Level.WARNING, "Failed to create Transformer", ex);
        }

        return result;
    }

    private static Document prepareDocument() throws ParserConfigurationException {
        final DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        return doc;
    }

    private static Element createRootElement(final Document doc) {
        final Element rootElement = doc.createElement("History");
        doc.appendChild(rootElement);

        for (Record r : records) {
            rootElement.appendChild(convertRecordToXML(r, doc));
        }

        return rootElement;
    }

    private static void exportDocumentToXML(final File target, final Document doc) throws TransformerConfigurationException, TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        DOMSource source = new DOMSource(doc);
        StreamResult output = new StreamResult(target);

        transformer.transform(source, output);
    }

    public static boolean export(final File target) {
        return export(target, null);
    }

    private static Node convertRecordToXML(final Record r, final Document doc) {
        final Node result = doc.createElement("Record");

        // TODO convert Record
        appendStringDataToNode(result, doc, "IPSource", r.getIpSource().getHostAddress());
        appendStringDataToNode(result, doc, "IPDestination", r.getIpDestination().getHostAddress());
        appendStringDataToNode(result, doc, "Time", df.format(r.getTime()));
        appendStringDataToNode(result, doc, "Accepted", String.valueOf(r.wasAccepted()));

        result.appendChild(exporter.exportObject(r.getData(), doc));

        return result;
    }

    private static void appendStringDataToNode(final Node n, final Document d, final String name, final String data) {
        final Element e = d.createElement(name);
        e.appendChild(d.createTextNode(data));
        n.appendChild(e);
    }

    public static void registerExporter(final IExportUnit eu) {
        exporter.registerExporter(eu);
    }
}
