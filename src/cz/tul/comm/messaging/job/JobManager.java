package cz.tul.comm.messaging.job;

import cz.tul.comm.IService;
import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.communicator.Status;
import cz.tul.comm.server.IClientManager;
import cz.tul.comm.server.IDataStorage;
import cz.tul.comm.socket.IListenerRegistrator;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Petr Ječmen
 */
public class JobManager extends Thread implements IService {

    private static final Logger log = Logger.getLogger(JobManager.class.getName());
    private static final int WAIT_TIME = 1000;
    private static final int MAX_CLIENT_NA_TIME = 5000;
    private final IClientManager clientManager;
    private IDataStorage dataStorage;
    private final IListenerRegistrator listenerRegistrator;
    private final Queue<ServerSideJob> jobQueue;
    private final Map<Communicator, ServerSideJob> jobAssignment;
    private final Map<Communicator, Calendar> lastTimeOnline;
    private boolean run;

    public JobManager(final IClientManager clientManager, final IListenerRegistrator listenerRegistrator) {
        this.clientManager = clientManager;
        this.listenerRegistrator = listenerRegistrator;
        jobAssignment = new ConcurrentHashMap<>();
        lastTimeOnline = new HashMap<>();
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
                checkClients();
            }
            synchronized (this) {
                try {
                    this.wait(WAIT_TIME);
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

        for (ServerSideJob ssj : jobAssignment.values()) {
            ssj.cancelJob();
        }

        log.fine("JobManager has been stopped.");
    }

    private void processQueue() {
        Set<Communicator> clients;
        ServerSideJob job;
        clients = clientManager.getClients();
        for (Communicator comm : clients) {
            if (jobQueue.isEmpty() || !isClientOnline(comm)) {
                break;
            }
            job = jobAssignment.get(comm);
            if (job != null && job.isDone()) {
                // job cleanup
                log.log(Level.CONFIG, "Job with ID {0} found as done.", new Object[]{job.getId(), comm.getId()});
                jobAssignment.remove(comm);
                jobQueue.remove(job);
                job = null;
            }

            if (job == null) {
                // new assignment
                job = jobQueue.poll();
                assignJob(job, comm);
                lastTimeOnline.put(comm, Calendar.getInstance());
                log.log(Level.CONFIG, "Job with ID {0} assigned to client with ID {1}.", new Object[]{job.getId(), comm.getId()});
            }
        }
    }

    private void checkClients() {
        for (Communicator comm : jobAssignment.keySet()) {
            if (!isClientOnline(comm)) {
                final Calendar actualTime = Calendar.getInstance();
                final Calendar lastOnline = lastTimeOnline.get(comm);
                if (lastOnline != null) {
                    final long dif = actualTime.getTimeInMillis() - lastOnline.getTimeInMillis();
                    if (dif > MAX_CLIENT_NA_TIME) {
                        final ServerSideJob reassigned = jobAssignment.get(comm);
                        if (!jobQueue.contains(reassigned)) {
                            jobQueue.add(reassigned);
                        }
                    }
                } else {
                    log.warning("No online time recorder for joc communicator.");
                }
            } else {
                lastTimeOnline.put(comm, Calendar.getInstance());
            }
        }
    }

    private boolean isClientOnline(final Communicator comm) {
        final Status s = comm.getStatus();
        return (s.equals(Status.ONLINE) || s.equals(Status.REACHABLE) || s.equals(Status.BUSY));
    }

    private void assignJob(final ServerSideJob job, final Communicator comm) {
        job.submitJob(comm);
        jobAssignment.put(comm, job);
    }
}
