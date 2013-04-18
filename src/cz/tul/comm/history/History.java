package cz.tul.comm.history;

import cz.tul.comm.history.export.Exporter;
import cz.tul.comm.history.export.IExportUnit;
import cz.tul.comm.history.sorting.DefaultSorter;
import cz.tul.comm.history.sorting.HistorySorter;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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

/**
 * Class for storing information about sent and received messages.
 *
 * @author Gargamel
 */
public class History implements IHistoryManager {

    private static final Logger log = Logger.getLogger(History.class.getName());

    private static Document prepareDocument() throws ParserConfigurationException {
        final DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        return doc;
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
    private final Set<Record> records;
    private final InetAddress localHost;

    /**
     * New instance of History conainer.
     */
    public History() {
        records = Collections.synchronizedSet(new HashSet<Record>());

        InetAddress local;
        try {
            local = InetAddress.getLocalHost();
        } catch (UnknownHostException ex) {
            log.log(Level.FINE, "Local address not found, using loopback", ex);
            local = InetAddress.getLoopbackAddress();
        }
        localHost = local;
    }

    @Override
    public void logMessageSend(final InetAddress ipDestination, final Object data, final boolean accepted) {
        final Record r = new Record(localHost, ipDestination, data, accepted);
        records.add(r);
        log.log(Level.FINE, "Sent message stored to history - {0}", r);

    }

    @Override
    public void logMessageReceived(final InetAddress ipSource, final Object data, final boolean accepted) {
        final Record r = new Record(ipSource, localHost, data, accepted);
        records.add(r);
        log.log(Level.FINE, "Received message stored to history - {0}", r);
    }

    @Override
    public boolean export(final File target, HistorySorter sorter) {
        boolean result = false;

        if (sorter == null) {
            sorter = new DefaultSorter();
        }

        try {
            final Document doc = prepareDocument();
            final Element rootElement = doc.createElement("History");
            final List<Element> data = sorter.sortHistory(records, doc);
            for (Element e : data) {
                rootElement.appendChild(e);
            }
            exportDocumentToXML(target, doc);
            result = true;

            log.log(Level.CONFIG, "History successfully exported to {0}, sorted using {1}", new Object[]{target.getAbsolutePath(), sorter.getClass().getCanonicalName()});
        } catch (ParserConfigurationException ex) {
            log.log(Level.WARNING, "Failed to create DocumentBuilder.", ex);
        } catch (TransformerConfigurationException ex) {
            log.log(Level.WARNING, "Failed to create Transformer.", ex);
        } catch (TransformerException ex) {
            log.log(Level.WARNING, "Failed to transform history into XML.", ex);
        }

        return result;
    }

    @Override
    public void registerExporter(final IExportUnit eu) {
        Exporter.registerExporterUnit(eu);
        log.log(Level.FINE, "New export unit registered - {0}", eu.getClass().getCanonicalName());
    }
}
