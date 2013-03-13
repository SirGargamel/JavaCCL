package cz.tul.comm.history.export;

import cz.tul.comm.history.IXMLExporter;
import java.util.HashMap;
import java.util.Map;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author Petr Ječmen
 */
public class Exporter implements IXMLExporter {
    
    private final Map<Class<?>, IExportUnit> exporters;
    
    public Exporter() {
        exporters = new HashMap<>();                
    }

    @Override
    public Element exportObject(Object data, Document doc) {
        Element result;
        
        Class<?> c = data.getClass();
        if (exporters.containsKey(c)) {
            result = exporters.get(c).exportData(doc, data, this);
        } else {
            result = doc.createElement(data.getClass().getSimpleName());
            result.appendChild(doc.createTextNode(data.toString()));
        }
        
        return result;
    }
    
    public void registerExporter(final IExportUnit eu) {
        exporters.put(eu.getExportedClass(), eu);
    }
}
