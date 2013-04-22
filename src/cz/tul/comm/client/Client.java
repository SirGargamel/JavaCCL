package cz.tul.comm.client;

import cz.tul.comm.IService;
import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.history.HistoryManager;
import cz.tul.comm.messaging.job.Assignment;
import cz.tul.comm.messaging.job.AssignmentListener;
import cz.tul.comm.socket.ListenerRegistrator;
import java.net.InetAddress;

/**
 *
 * @author Petr Ječmen
 */
public interface Client extends IService {

    /**
     * Atach interface, that will handle assignment computation.
     *
     * @param assignmentListener class hnadling assignment computation
     */
    void assignAssignmentListener(AssignmentListener assignmentListener);

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

    Communicator getServerComm();

    boolean isServerUp();

    boolean registerToServer(final InetAddress address, final int port);

    /**
     * Send data to server.
     *
     * @param data data for sending
     * @return true for successfull data sending
     */
    boolean sendDataToServer(final Object data);
    
    void requestAssignment();
}
