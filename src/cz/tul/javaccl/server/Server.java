package cz.tul.javaccl.server;

import cz.tul.javaccl.CCLEntity;
import cz.tul.javaccl.ComponentManager;
import cz.tul.javaccl.GlobalConstants;
import cz.tul.javaccl.IService;
import cz.tul.javaccl.Utils;
import cz.tul.javaccl.communicator.Communicator;
import cz.tul.javaccl.exceptions.ConnectionException;
import cz.tul.javaccl.history.HistoryManager;
import cz.tul.javaccl.job.server.Job;
import cz.tul.javaccl.job.server.ServerJobManager;
import cz.tul.javaccl.socket.ListenerRegistrator;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Interface of server part.
 *
 * @author Petr Ječmen
 */
public abstract class Server extends CCLEntity implements IService, ComponentManager {

    private static final Logger LOG = Logger.getLogger(Server.class.getName());

    static {
        Utils.initConsoleLogging();
    }    

    /**
     * @param dataStorage class handling data requests
     */
    public abstract void assignDataStorage(final DataStorage dataStorage);

    /**
     * Export history as is to XML file at library location.
     *
     * @return true for successfull export.
     */
    public abstract boolean exportHistory();

    /**
     * @param address clients IP
     * @return client at given IP
     */
    public abstract Communicator getClient(final InetAddress address);

    /**
     *
     * @param id clients ID
     * @return client with given ID
     */
    public abstract Communicator getClient(final UUID id);

    /**
     * @return interface for managing clients
     */
    public abstract ClientManager getClientManager();

    /**
     * @return history manager for this client
     */
    public abstract HistoryManager getHistory();

    /**
     * @return Interface for managing listeners.
     */
    public abstract ListenerRegistrator getListenerRegistrator();

    /**
     * @return interface for job management
     */
    public abstract ServerJobManager getJobManager();

    /**
     * Register new client communicationg on given IP and on default port.
     *
     * @param adress client IP
     * @return {@link Communicator} for communication
     * @throws ConnectionException client could not be contacted
     */
    public abstract Communicator registerClient(final InetAddress adress) throws ConnectionException;

    /**
     * Submit new job for computation
     *
     * @param task jobs task
     * @return interface for job control and result obtaining
     * @throws IllegalArgumentException jobs task could not be serialized
     */
    public abstract Job submitJob(final Object task) throws IllegalArgumentException;

    /**
     * Load settings from given file.
     *
     * @param settingsFile source file
     * @return true for succefull load
     */
    public abstract boolean loadSettings(final File settingsFile);

    /**
     * Save settings to given file.
     *
     * @param settingsFile target file
     * @return true for successfull save
     */
    public abstract boolean saveSettings(final File settingsFile);
    
    /**
     * Generate a settings file for a client to connect to this server.
     *
     * @param settingsFile target file
     * @return true for successfull save
     */
    public abstract boolean generateClientSettings(final File settingsFile);
    
    public abstract ComponentManager getComponentManager();
}
