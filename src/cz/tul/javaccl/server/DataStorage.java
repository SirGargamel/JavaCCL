package cz.tul.javaccl.server;

/**
 * Interface for obtaining data requested by client.
 *
 * @author Petr Ječmen
 */
public interface DataStorage {

    /**
     * Retrieve data with given identificator from some data storage.
     *
     * @param dataId data indentificator
     * @return instance of requsted data
     */
    Object requestData(final Object dataId);
}
