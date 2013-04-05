package cz.tul.comm.messaging.job;

import cz.tul.comm.IService;
import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.server.IClientManager;
import cz.tul.comm.server.IDataStorage;
import cz.tul.comm.socket.IListenerRegistrator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Petr Ječmen
 */
public class JobManager extends Thread implements IService {

    private static final Logger log = Logger.getLogger(JobManager.class.getName());
    private final IClientManager clientManager;
    private IDataStorage dataStorage;
    private final IListenerRegistrator listenerRegistrator;
    private final Queue<ServerSideJob> jobQueue;
    private final Map<Communicator, List<ServerSideJob>> jobAssignment;
    private boolean run;

    public JobManager(final IClientManager clientManager, final IListenerRegistrator listenerRegistrator) {
        this.clientManager = clientManager;
        this.listenerRegistrator = listenerRegistrator;
        jobAssignment = new ConcurrentHashMap<>();
        jobQueue = new ConcurrentLinkedQueue<>();

        run = true;
    }

    public void setDataStorage(IDataStorage dataStorage) {
        this.dataStorage = dataStorage;
    }

    public Job submitJob(final Object task) {
        final ServerSideJob result = new ServerSideJob(task, listenerRegistrator, dataStorage);
        jobQueue.add(result);

        log.log(Level.CONFIG, "Job with ID {0} submitted.", result.getId());
        synchronized (this) {
            this.notify();
        }

        return result;
    }

    @Override
    public void run() {
        log.fine("JobManager has been started.");
        while (run) {
            if (!jobQueue.isEmpty()) {
                processQueue();
                // TODO monitor client status and mark unaviable clients
            }
            synchronized (this) {
                try {
                    this.wait();
                } catch (InterruptedException ex) {
                    log.log(Level.WARNING, "Waiting of JobManager has been interrupted", ex);
                }
            }
        }
    }    

    @Override
    public void stopService() {
        run = false;
        synchronized (this) {
            this.notify();
        }
        for (List<ServerSideJob> l : jobAssignment.values()) {
            for (ServerSideJob ssj : l) {
                ssj.cancelJob();
            }
        }
        log.fine("JobManager has been stopped.");
    }

    private void processQueue() {
        Set<Communicator> clients;
        List<ServerSideJob> jobList;
        ServerSideJob job;
        clients = clientManager.getClients();
        for (Communicator comm : clients) {
            if (jobQueue.isEmpty()) {
                break;
            }
            jobList = jobAssignment.get(comm);
            if (jobList != null
                    && jobList.size() > 0) {
                Iterator<ServerSideJob> it = jobList.iterator();
                while (it.hasNext()) {
                    job = it.next();
                    if (job.isDone()) {
                        log.log(Level.CONFIG, "Job with ID {0} found as done.", new Object[]{job.getId(), comm.getId()});
                        it.remove();
                    }
                }
                if (!jobList.isEmpty()) {
                    continue;
                }
            }

            job = jobQueue.poll();
            assignJob(job, comm);
            log.log(Level.CONFIG, "Job with ID {0} assigned to client with ID {1}.", new Object[]{job.getId(), comm.getId()});
        }
    }
    
    private void assignJob(final ServerSideJob job, final Communicator comm) {
        job.submitJob(comm);
        List<ServerSideJob> jobs = jobAssignment.get(comm);
        if (jobs == null) {
            jobs = new CopyOnWriteArrayList<>();
        }
        jobs.add(job);
    }
}
