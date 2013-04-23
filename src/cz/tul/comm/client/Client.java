package cz.tul.comm.client;

import cz.tul.comm.IService;
import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.history.HistoryManager;
import cz.tul.comm.job.AssignmentListener;
import cz.tul.comm.socket.ListenerRegistrator;
import java.net.InetAddress;

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
    void assignAssignmentListener(AssignmentListener assignmentListener);

    /**
     * Deregister client from current server.
     */
    void deregisterFromServer();

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
     * @return Communicator object connected to server.
     */
    Communicator getServerComm();

    /**
     * @return true if server could be contacted and responds
     */
    boolean isServerUp();

    /**
     * Try to register to server at given address and port.
     *
     * @param address target IP address
     * @param port target port
     * @return true if registration has been successfull
     */
    boolean registerToServer(final InetAddress address, final int port);

    /**
     * Send data to server.
     *
     * @param data data for sending
     * @return true for successfull data sending
     */
    boolean sendDataToServer(final Object data);

    /**
     * Request an extra job from server.
     */
    void requestAssignment();
}
