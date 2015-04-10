package cz.tul.javaccl.history.export;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author Lenam s.r.o.
 */
public interface IExporter {
    
    Element exportObject(Object data, Document doc);
    
}
