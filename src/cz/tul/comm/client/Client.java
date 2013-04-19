package cz.tul.comm.client;

import cz.tul.comm.IService;
import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.communicator.Status;
import cz.tul.comm.history.IHistoryManager;
import cz.tul.comm.messaging.job.IAssignmentListener;
import cz.tul.comm.socket.IListenerRegistrator;
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
    void assignAssignmentListener(IAssignmentListener assignmentListener);

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
    IHistoryManager getHistory();

    /**
     * @return interface for listener registration
     */
    IListenerRegistrator getListenerRegistrator();

    Communicator getServerComm();

    boolean isServerUp();

    boolean registerToServer(final InetAddress address, final int port);

    /**
     * Send data to server.
     *
     * @param data data for sending
     * @return true for successfull data sending
     */
    boolean sendData(final Object data);
}
