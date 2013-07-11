package cz.tul.comm.client;

import cz.tul.comm.IService;
import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.exceptions.ConnectionException;
import cz.tul.comm.history.HistoryManager;
import cz.tul.comm.job.client.AssignmentListener;
import cz.tul.comm.socket.ListenerRegistrator;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

/**
 * Interface for client side of library.
 *
 * @author Petr Ječmen
 */
public interface Client extends IService {

    /**
     * Atach interface that will handle assignment computation.
     *
     * @param assignmentListener class hnadling assignment computation
     */
    void setAssignmentListener(AssignmentListener assignmentListener);

    /**
     * Deregister client from current server.
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

    boolean registerToServer(final InetAddress address) throws ConnectionException;

    /**
     * Try to register to server at given address and port.
     *
     * @param address target IP address
     * @param port target port
     * @return true if registration has been successfull
     */
    boolean registerToServer(final InetAddress address, final int port) throws ConnectionException;

    /**
     * Send data to server.
     *
     * @param data data for sending
     * @return true for successfull data sending
     */
    Object sendDataToServer(final Object data) throws ConnectionException;

    Object sendDataToServer(final Object data, final int timeout) throws ConnectionException;

    Object sendDataToClient(final UUID clientId, final Object data) throws UnknownHostException, IllegalArgumentException, ConnectionException;

    Object sendDataToClient(final UUID clientId, final Object data, final int timeout) throws UnknownHostException, IllegalArgumentException, ConnectionException;

    /**
     * Request an extra job from server.
     */
    void requestAssignment() throws ConnectionException;
}
