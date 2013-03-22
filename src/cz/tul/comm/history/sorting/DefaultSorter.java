package cz.tul.comm.history.sorting;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * No sorting done to elements.
 *
 * @author Petr Ječmen
 */
public class DefaultSorter extends HistorySorter {

    @Override
    public Element sortHistory(final Element rootElement, final Document doc) {
        return rootElement;
    }
}
