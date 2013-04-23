package cz.tul.comm.server;

import cz.tul.comm.IService;
import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.history.HistoryManager;
import cz.tul.comm.job.Job;
import cz.tul.comm.job.JobManager;
import cz.tul.comm.socket.ListenerRegistrator;
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
    JobManager getJobManager();

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
