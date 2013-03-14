package cz.tul.comm.history.export;

import cz.tul.comm.history.IXMLExporter;
import java.util.HashMap;
import java.util.Map;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Class for exporting history to XML file.
 *
 * @author Petr Ječmen
 */
public final class Exporter implements IXMLExporter {

    private final Map<Class<?>, IExportUnit> exporters;

    /**
     * Prepare new Exporter.
     */
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

    /**
     * Register new unit for data exporting.
     *
     * @param eu unit for registering
     */
    public void registerExporterUnit(final IExportUnit eu) {
        exporters.put(eu.getExportedClass(), eu);
    }
}
