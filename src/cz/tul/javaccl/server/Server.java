package cz.tul.javaccl.server;

import cz.tul.javaccl.Constants;
import cz.tul.javaccl.IService;
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
public abstract class Server implements IService {
    
    private static final Logger log = Logger.getLogger(Server.class.getName());
    
    /**
     * Create and initialize new instance of server.
     *
     * @param port server port (muse be valid port nuber between 0 and 65535)
     * @return new instance of ServerImpl
     * @throws IOException error opening socket on given port
     */
    public static Server initNewServer(final int port) throws IOException {
        final ServerImpl result = new ServerImpl(port);
        result.start();
        log.log(Level.INFO, "New server created on port " + port);

        return result;
    }

    /**
     * Create and initialize new instance of server on default port.
     *
     * @return new instance of ServerImpl
     */
    public static Server initNewServer() {
        Server s = null;
        int port = Constants.DEFAULT_PORT;

        while (s == null && port < 65535) {
            try {
                s = initNewServer(port++);
            } catch (IOException ex) {
                log.log(Level.WARNING, "Error initializing server on port " + (port - 1), ex);
            }
        }
        
        if (s == null) {
            log.log(Level.WARNING, "Error initializing server, no free port found");            
        }

        return s;
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
}
