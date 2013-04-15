package cz.tul.comm.server;

import cz.tul.comm.IService;
import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.history.IHistoryManager;
import cz.tul.comm.messaging.job.Job;
import cz.tul.comm.socket.IListenerRegistrator;
import java.net.InetAddress;
import java.util.UUID;

/**
 *
 * @author Petr Ječmen
 */
public interface Server extends IService {

    /**
     * @param dataStorage class hnadling data requests
     */
    void assignDataStorage(final IDataStorage dataStorage);

    /**
     * Export history as is to XML file.
     *
     * @return true for successfull export.
     */
    boolean exportHistory();

    /**
     *
     * @param address
     * @return
     */
    Communicator getClient(final InetAddress address);
    
    Communicator getClient(final UUID id);

    /**
     *
     * @return
     */
    IClientManager getClientManager();

    /**
     * @return history manager for this client
     */
    IHistoryManager getHistory();

    /**
     * Interface for registering new data listeners.
     *
     * @return
     */
    IListenerRegistrator getListenerRegistrator();

    /**
     * Register new client communicationg on given IP and on default port.
     *
     * @param adress client IP
     * @return
     */
    Communicator registerClient(final InetAddress adress);

    /**
     * Submit new job for computation
     *
     * @param task jobs task
     * @return interface for job control and result obtaining
     */
    Job submitJob(final Object task);
    
}
