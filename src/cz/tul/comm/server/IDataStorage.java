package cz.tul.comm.server;

/**
 * Interface for obtaining data requested by client.
 *
 * @author Petr Ječmen
 */
public interface IDataStorage {

    /**
     * Retrieve data with given identificator from some data storage.
     *
     * @param dataId data indentificator
     * @return instance of requsted data
     */
    Object requestData(final Object dataId);
}
