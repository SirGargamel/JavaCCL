package cz.tul.comm.client;

import cz.tul.comm.IService;
import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.exceptions.ConnectionException;
import cz.tul.comm.history.HistoryManager;
import cz.tul.comm.job.client.AssignmentListener;
import cz.tul.comm.socket.ListenerRegistrator;
import java.io.File;
import java.net.InetAddress;
import java.util.UUID;

/**
 * Interface for client side of library.
 *
 * @author Petr Ječmen
 */
public interface Client extends IService {

    /**
     * @return UUID of this client
     */
    UUID getLocalID();

    /**
     * Atach interface that will handle assignment computation.
     *
     * @param assignmentListener class hnadling assignment computation
     */
    void setAssignmentListener(AssignmentListener assignmentListener);

    /**
     * Deregister client from current server.
     *
     * @throws ConnectionException server could not be contacted
     */
    void deregisterFromServer() throws ConnectionException;

    /**
     * Export history as is to XML file.
     *
     * @return true for successfull export.
     */
    boolean exportHistory();

    /**
     * @return history manager for this client
     */
    HistoryManager getHistory();

    /**
     * @return interface for listener registration
     */
    ListenerRegistrator getListenerRegistrator();

    /**
     * @return CommunicatorImpl object connected to server.
     */
    Communicator getServerComm();

    /**
     * @return true if server could be contacted and responds
     */
    boolean isServerUp();

    /**
     * Register to server running on default port.
     *
     * @param address servers IP
     * @return true if the registration has been successfull
     * @throws ConnectionException server could not be contacted
     */
    boolean registerToServer(final InetAddress address) throws ConnectionException;

    /**
     * Try to register to server at given address and port.
     *
     * @param address target IP address
     * @param port target port
     * @return true if registration has been successfull
     * @throws ConnectionException server could not be contacted
     */
    boolean registerToServer(final InetAddress address, final int port) throws ConnectionException;

    /**
     * Send data to server.
     *
     * @param data data for sending
     * @return true for successfull data sending
     * @throws ConnectionException could not contact the server
     */
    Object sendDataToServer(final Object data) throws ConnectionException;

    /**
     * Send data to server.
     *
     * @param data data for sending
     * @param timeout maximal waiting time
     * @return true for successfull data sending
     * @throws ConnectionException could not contact the server
     */
    Object sendDataToServer(final Object data, final int timeout) throws ConnectionException;

    /**
     * Alter the maximal count of concurrent assignments (default is 1)
     *
     * @param assignmentCount maximal count of conccurent assignments
     * @return true for successfull change
     */
    boolean setMaxNumberOfConcurrentAssignments(final int assignmentCount);

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
