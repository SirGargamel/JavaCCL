package cz.tul.javaccl.client;

import cz.tul.javaccl.CCLEntity;
import cz.tul.javaccl.ComponentManager;
import cz.tul.javaccl.IService;
import cz.tul.javaccl.Utils;
import cz.tul.javaccl.communicator.Communicator;
import cz.tul.javaccl.exceptions.ConnectionException;
import cz.tul.javaccl.history.HistoryManager;
import cz.tul.javaccl.job.client.AssignmentListener;
import cz.tul.javaccl.socket.ListenerRegistrator;
import java.io.File;
import java.net.InetAddress;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Interface for client side of library.
 *
 * @author Petr Ječmen
 */
public abstract class Client extends CCLEntity implements IService, ComponentManager {

    private static final Logger LOG = Logger.getLogger(Client.class.getName());

    static {
        Utils.initConsoleLogging();
    }    

    /**
     * @return UUID of this client
     */
    public abstract UUID getLocalID();

    /**
     * Atach interface that will handle assignment computation.
     *
     * @param assignmentListener class hnadling assignment computation
     */
    public abstract void setAssignmentListener(AssignmentListener assignmentListener);

    /**
     * Deregister client from current server.
     *
     * @throws ConnectionException server could not be contacted
     */
    public abstract void deregisterFromServer() throws ConnectionException;

    /**
     * Export history as is to XML file.
     *
     * @return true for successfull export.
     */
    public abstract boolean exportHistory();

    /**
     * @return history manager for this client
     */
    public abstract HistoryManager getHistory();

    /**
     * @return interface for listener registration
     */
    public abstract ListenerRegistrator getListenerRegistrator();

    /**
     * @return CommunicatorImpl object connected to server.
     */
    public abstract Communicator getServerComm();

    /**
     * @return true if server could be contacted and responds
     */
    public abstract boolean isServerUp();

    /**
     * Register to server running on default port.
     *
     * @param address servers IP
     * @return true if the registration has been successfull
     * @throws ConnectionException server could not be contacted
     */
    public abstract boolean registerToServer(final InetAddress address) throws ConnectionException;

    /**
     * Try to register to server at given address and port.
     *
     * @param address target IP address
     * @param port target port
     * @return true if registration has been successfull
     * @throws ConnectionException server could not be contacted
     */
    public abstract boolean registerToServer(final InetAddress address, final int port) throws ConnectionException;

    /**
     * Send data to server.
     *
     * @param data data for sending
     * @return true for successfull data sending
     * @throws ConnectionException could not contact the server
     */
    public abstract Object sendDataToServer(final Object data) throws ConnectionException;

    /**
     * Send data to server.
     *
     * @param data data for sending
     * @param timeout maximal waiting time
     * @return true for successfull data sending
     * @throws ConnectionException could not contact the server
     */
    public abstract Object sendDataToServer(final Object data, final int timeout) throws ConnectionException;

    /**
     * Alter the maximal count of concurrent assignments (default is 1)
     *
     * @param assignmentCount maximal count of conccurent assignments
     * @return true for successfull change
     */
    public abstract boolean setMaxNumberOfConcurrentAssignments(final int assignmentCount);

    /**
     * @param maxJobComplexity maximal complexity of jobs computable by this
     * client
     * @return true is server accepted the data
     */
    public abstract boolean setMaxJobComplexity(final int maxJobComplexity);

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
     * Enables / disables new assignment receiving.
     *
     * @param enable true if you want the client to receive assignments
     */
    public abstract void enableClient(final boolean enable);
    
    public abstract ComponentManager getComponentManager();
}
