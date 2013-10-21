package cz.tul.javaccl.persistence;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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

    private static final Logger log = Logger.getLogger(SimpleXMLSettingsFile.class.getName());
    private static final String SETTINGS_NODE_NAME = "JavaCCL";

    /**
     * Load contents of a simple XML file.
     *
     * @param source file for reading
     * @return map filled with loaded pairs of values
     * @throws IOException error accessing source file
     * @throws SAXException error parsing XML file
     */
    public static Map<String, String> loadSimpleXMLFile(final File source) throws IOException, SAXException {
        Map<String, String> fields = new HashMap<String, String>();

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(source);

            //optional, but recommended
            //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            doc.getDocumentElement().normalize();

            Queue<Node> nodesForParsing = new ConcurrentLinkedQueue<Node>();
            addAllChildNodes(nodesForParsing, doc.getElementsByTagName(SETTINGS_NODE_NAME));

            Node node;
            while (!nodesForParsing.isEmpty()) {
                node = nodesForParsing.poll();

                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) node;
                    fields.put(eElement.getTagName(), eElement.getTextContent());
                }

                addAllChildNodes(nodesForParsing, node.getChildNodes());
            }
        } catch (ParserConfigurationException ex) {
            log.log(Level.SEVERE, "Illegal parser config used.", ex);
        }

        return fields;
    }

    private static void addAllChildNodes(final Queue<Node> processingQueue, final NodeList nodes) {
        for (int i = 0; i < nodes.getLength(); i++) {
            processingQueue.add(nodes.item(i));
        }
    }
    private final Map<String, String> fields;

    /**
     * Init fresh instance of XML file.
     */
    public SimpleXMLSettingsFile() {
        this.fields = new HashMap<String, String>();
    }

    /**
     * Store new value pair
     *
     * @param fieldName name of the field
     * @param fieldValue value of the field
     */
    public void addField(final String fieldName, final String fieldValue) {
        fields.put(fieldName, fieldValue);
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
            if (!target.exists()) {
                target.createNewFile();
            }

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // root element
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement(SETTINGS_NODE_NAME);
            doc.appendChild(rootElement);
            // data elements
            Element el;
            for (Entry<String, String> e : fields.entrySet()) {
                el = doc.createElement(e.getKey());
                el.appendChild(doc.createTextNode(e.getValue()));
                rootElement.appendChild(el);
            }
            // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            DOMSource source = new DOMSource(doc);
            StreamResult out = new StreamResult(target);
            transformer.transform(source, out);

            result = true;
        } catch (ParserConfigurationException ex) {
            log.log(Level.SEVERE, "Wrong parser configuration used.", ex);
        } catch (TransformerException ex) {
            log.log(Level.SEVERE, "Transformer exception occured.", ex);
        }

        return result;
    }
}
