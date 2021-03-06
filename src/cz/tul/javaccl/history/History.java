package cz.tul.javaccl.history;

import cz.tul.javaccl.GlobalConstants;
import cz.tul.javaccl.history.export.ExportMessage;
import cz.tul.javaccl.history.export.ExportUnit;
import cz.tul.javaccl.history.export.Exporter;
import cz.tul.javaccl.history.sorting.HistorySorter;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
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
public class History implements HistoryManager {

    private static final Logger LOG = Logger.getLogger(History.class.getName());
    private boolean isEnabled;

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
    private final List<HistoryRecord> records;
    private final InetAddress localHost;
    private Document defaultDoc;

    /**
     * New instance of History conainer.
     */
    public History() {
        records = Collections.synchronizedList(new LinkedList<HistoryRecord>());

        InetAddress local;
        try {
            local = InetAddress.getLocalHost();
        } catch (UnknownHostException ex) {
            LOG.log(Level.FINE, "Local address not found, using loopback", ex);
            local = GlobalConstants.IP_LOOPBACK;
        }
        localHost = local;
        isEnabled = true;

        try {
            defaultDoc = prepareDocument();
        } catch (ParserConfigurationException ex) {
            LOG.log(Level.WARNING, "Failed to create DocumentBuilder, disabling history logging.");
            LOG.log(Level.FINE, "Failed to create DocumentBuilder.", ex);
            isEnabled = false;
        }
        
        Exporter.registerExporterUnit(new ExportMessage());
    }

    @Override
    public void logMessageSend(final InetAddress ipDestination, final UUID targetId, final Object data, final boolean accepted, final Object answer) {
        if (isEnabled) {
            final HistoryRecord r = new HistoryRecord(localHost, ipDestination, targetId, data, accepted, answer, defaultDoc, true);
            records.add(r);
            LOG.log(Level.FINE, "Sent message stored to history - " + r);
        }
    }

    @Override
    public void logMessageReceived(final InetAddress ipSource, final UUID sourceId, final Object data, final boolean accepted, final Object answer) {
        if (isEnabled) {
            final HistoryRecord r = new HistoryRecord(ipSource, localHost, sourceId, data, accepted, answer, defaultDoc, false);
            records.add(r);
            LOG.log(Level.FINE, "Received message stored to history - " + r);
        }
    }

    @Override
    public boolean export(final File target, HistorySorter sorter) {
        boolean result = false;

        if (sorter == null) {
            throw new IllegalArgumentException("Null sorter not allowed");
        }

        try {
            final Document doc = prepareDocument();
            final Element rootElement = doc.createElement("History");
            doc.appendChild(rootElement);
            final List<Element> data = sorter.sortHistory(records, doc);
            for (Element e : data) {
                rootElement.appendChild(e);
            }
            exportDocumentToXML(target, doc);
            result = true;

            LOG.log(Level.FINE, "History successfully exported to {0}, sorted using {1}", new Object[]{target.getAbsolutePath(), sorter.getClass().getCanonicalName()});
        } catch (TransformerConfigurationException ex) {
            LOG.log(Level.WARNING, "Failed to create Transformer, export failed.");
            LOG.log(Level.FINE, "Failed to create Transformer.", ex);
        } catch (TransformerException ex) {
            LOG.log(Level.WARNING, "Failed to transform history into XML, export failed.");
            LOG.log(Level.FINE, "Failed to transform history into XML.", ex);
        } catch (ParserConfigurationException ex) {
            LOG.log(Level.WARNING, "Failed to create DocumentBuilder, export failed.");
            LOG.log(Level.FINE, "Failed to create DocumentBuilder.", ex);
        }

        return result;
    }

    @Override
    public void registerExporter(final ExportUnit eu) {
        Exporter.registerExporterUnit(eu);
    }

    List<HistoryRecord> getRecords() {
        return records;
    }

    @Override
    public void enable(boolean enable) {
        isEnabled = enable;
    }

    @Override
    public void enableAutomticExportBeforeShutdown(final File target, final HistorySorter sorter) {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                export(target, sorter);
            }
        }));
    }
}
