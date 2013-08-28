package cz.tul.comm.server;

import cz.tul.comm.IService;
import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.exceptions.ConnectionException;
import cz.tul.comm.history.HistoryManager;
import cz.tul.comm.job.server.Job;
import cz.tul.comm.job.server.ServerJobManager;
import cz.tul.comm.socket.ListenerRegistrator;
import java.io.File;
import java.net.InetAddress;
import java.util.UUID;

/**
 * Interface of server part.
 *
 * @author Petr Ječmen
 */
public interface Server extends IService {

    /**
     * @param dataStorage class handling data requests
     */
    void assignDataStorage(final DataStorage dataStorage);

    /**
     * Export history as is to XML file at library location.
     *
     * @return true for successfull export.
     */
    boolean exportHistory();

    /**
     * @param address clients IP
     * @return client at given IP
     */
    Communicator getClient(final InetAddress address);

    /**
     *
     * @param id clients ID
     * @return client with given ID
     */
    Communicator getClient(final UUID id);

    /**
     * @return interface for managing clients
     */
    ClientManager getClientManager();

    /**
     * @return history manager for this client
     */
    HistoryManager getHistory();

    /**
     * @return Interface for managing listeners.
     */
    ListenerRegistrator getListenerRegistrator();

    /**
     * @return interface for job management
     */
    ServerJobManager getJobManager();

    /**
     * Register new client communicationg on given IP and on default port.
     *
     * @param adress client IP
     * @return {@link Communicator} for communication
     * @throws ConnectionException client could not be contacted
     */
    Communicator registerClient(final InetAddress adress) throws ConnectionException;

    /**
     * Submit new job for computation
     *
     * @param task jobs task
     * @return interface for job control and result obtaining
     * @throws IllegalArgumentException jobs task could not be serialized
     */
    Job submitJob(final Object task) throws IllegalArgumentException;

    /**
     * Load settings from given file.
     *
     * @param settingsFile source file
     * @return true for succefull load
     */
    boolean loadSettings(final File settingsFile);

    /**
     * Save settings to given file.
     *
     * @param settingsFile target file
     * @return true for successfull save
     */
    boolean saveSettings(final File settingsFile);
}
