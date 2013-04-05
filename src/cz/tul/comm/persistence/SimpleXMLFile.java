package cz.tul.comm.persistence;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
 *
 * @author Petr Ječmen
 */
public class SimpleXMLFile {

    private static final Logger log = Logger.getLogger(SimpleXMLFile.class.getName());
    private final List<Field> fields;

    public SimpleXMLFile() {
        this.fields = new ArrayList<>();
    }

    public void addField(final Field field) {
        fields.add(field);
    }

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
            Element rootElement = doc.createElement("settings");
            doc.appendChild(rootElement);
            // data elements
            Element el;
            for (Field f : fields) {
                el = doc.createElement(f.getName());
                el.appendChild(doc.createTextNode(f.getValue()));
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

    public static List<Field> loadSimpleXMLFile(final File source) throws IOException, SAXException {
        List<Field> fields = new ArrayList<>();
        
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(source);

            //optional, but recommended
            //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            doc.getDocumentElement().normalize();

            System.out.println("Root element :" + doc.getDocumentElement().getNodeName());

            NodeList nList = doc.getChildNodes();

            System.out.println("----------------------------");

            for (int temp = 0; temp < nList.getLength(); temp++) {

                Node nNode = nList.item(temp);

                System.out.println("\nCurrent Element :" + nNode.getNodeName());

                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    fields.add(new Field(eElement.getTagName(), eElement.getTextContent()));
                }
            }
        } catch (ParserConfigurationException ex) {
            log.log(Level.SEVERE, "Illegal parser config used.", ex);
        }

        return fields;
    }
}
