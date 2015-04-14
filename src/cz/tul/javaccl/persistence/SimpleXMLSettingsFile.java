package cz.tul.javaccl.persistence;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Simple XML file for storing apirs of values.
 *
 * @author Petr Ječmen
 */
public class SimpleXMLSettingsFile {

    private static final Logger LOG = Logger.getLogger(SimpleXMLSettingsFile.class.getName());

    /**
     * Load contents of a simple XML file.
     *
     * @param source file for reading
     * @return map filled with loaded pairs of values
     * @throws IOException error accessing source file
     * @throws SAXException error parsing XML file
     */
    public static List<Entry<String, String>> loadSimpleXMLFile(final File source) throws IOException, SAXException {
        final List<Entry<String, String>> fields = new LinkedList<Entry<String, String>>();

        try {
            final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            final Document doc = dBuilder.parse(source);

            //optional, but recommended
            //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            doc.getDocumentElement().normalize();

            final Queue<Node> nodesForParsing = new ConcurrentLinkedQueue<Node>();
            addAllChildNodes(nodesForParsing, doc.getElementsByTagName(XmlNodes.ROOT.toString()));

            Node node;
            Element eElement;
            while (!nodesForParsing.isEmpty()) {
                node = nodesForParsing.poll();

                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    eElement = (Element) node;
                    fields.add(new AbstractMap.SimpleImmutableEntry<String, String>(eElement.getTagName(), eElement.getTextContent()));
                }

                addAllChildNodes(nodesForParsing, node.getChildNodes());
            }
        } catch (ParserConfigurationException ex) {
            LOG.log(Level.SEVERE, "Illegal parser config used.", ex);
        }

        return fields;
    }

    private static void addAllChildNodes(final Queue<Node> processingQueue, final NodeList nodes) {
        for (int i = 0; i < nodes.getLength(); i++) {
            processingQueue.add(nodes.item(i));
        }
    }
    private final List<Entry<String, String>> fields;

    /**
     * Init fresh instance of XML file.
     */
    public SimpleXMLSettingsFile() {
        this.fields = new LinkedList<Entry<String, String>>();
    }

    /**
     * Store new value pair
     *
     * @param fieldName name of the field
     * @param fieldValue value of the field
     */
    public void addField(final String fieldName, final String fieldValue) {
        fields.add(new AbstractMap.SimpleImmutableEntry<String, String>(fieldName, fieldValue));
    }

    /**
     * Save contents of this instance to a file.
     *
     * @param target target file
     * @return true for successfull saving
     * @throws IOException error handling target file
     */
    public boolean storeXML(final File target) throws IOException {
        boolean result = false;

        try {
            if (!target.exists() && !target.createNewFile()) {
                throw new IOException("Could not create target file " + target.getAbsolutePath());
            }

            final DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // root element
            final Document doc = docBuilder.newDocument();
            final Element rootElement = doc.createElement(XmlNodes.ROOT.toString());
            doc.appendChild(rootElement);
            // data elements
            Element el;
            for (Entry<String, String> e : fields) {
                el = doc.createElement(e.getKey());
                el.appendChild(doc.createTextNode(e.getValue()));
                rootElement.appendChild(el);
            }
            // write the content into xml file
            final TransformerFactory transformerFactory = TransformerFactory.newInstance();
            final Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            final DOMSource source = new DOMSource(doc);
            final StreamResult out = new StreamResult(target);
            transformer.transform(source, out);

            result = true;
        } catch (ParserConfigurationException ex) {
            LOG.log(Level.SEVERE, "Wrong parser configuration used.", ex);
        } catch (TransformerException ex) {
            LOG.log(Level.SEVERE, "Transformer exception occured.", ex);
        }

        return result;
    }
}
