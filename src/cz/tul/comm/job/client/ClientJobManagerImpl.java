package cz.tul.comm.job.client;

import cz.tul.comm.GenericResponses;
import cz.tul.comm.client.ServerInterface;
import cz.tul.comm.job.JobMessageHeaders;
import cz.tul.comm.job.JobTask;
import cz.tul.comm.socket.queue.Identifiable;
import cz.tul.comm.socket.queue.Listener;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Petr Ječmen
 */
public class ClientJobManagerImpl implements Listener, ClientJobManager {

    private static final Logger log = Logger.getLogger(ClientJobManagerImpl.class.getName());
    private static final int WAIT_TIME = 500;
    private AssignmentListener assignmentListener;
    private final ServerInterface server;
    private final Map<UUID, ClientSideJob> jobs;
    private final ExecutorService exec;

    public ClientJobManagerImpl(ServerInterface server) {
        this.server = server;
        jobs = new HashMap<>();
        exec = Executors.newCachedThreadPool();
    }

    public void setAssignmentListener(AssignmentListener assignmentListener) {
        this.assignmentListener = assignmentListener;
    }

    @Override
    public void submitResult(final UUID jobId, final Object result) {
        sendDataToServer(jobId, JobMessageHeaders.JOB_RESULT, result);
    }

    @Override
    public void cancelJob(final UUID jobId) {
        sendDataToServer(jobId, JobMessageHeaders.JOB_CANCEL, null);
    }

    @Override
    public void acceptJob(final UUID jobId) {
        sendDataToServer(jobId, JobMessageHeaders.JOB_ACCEPT, null);
    }

    @Override
    public Object requestData(final UUID jobId, final Object dataId) {
        return sendDataToServer(jobId, JobMessageHeaders.JOB_DATA_REQUEST, dataId);
    }

    public void requestAssignment() {
        sendDataToServer(null, JobMessageHeaders.JOB_REQUEST, server.getServerComm().getTargetId());
    }

    private Object sendDataToServer(final UUID jobId, final String header, final Object result) {
        waitForSever();
        final JobTask jt = new JobTask(jobId, header, result);
        return server.getServerComm().sendData(jt);
    }

    private void waitForSever() {
        while (!server.isServerUp()) {
            synchronized (this) {
                try {
                    this.wait(WAIT_TIME);
                } catch (InterruptedException ex) {
                    log.log(Level.WARNING, "Waiting for server being aviable for data request has been interrupted.", ex);
                }
            }
        }
    }

    @Override
    public Object receiveData(final Identifiable data) {
        if (data instanceof JobTask) {
            final JobTask jt = (JobTask) data;
            final UUID id = jt.getJobId();
            switch (jt.getTaskDescription()) {
                case JobMessageHeaders.JOB_TASK:
                    final ClientSideJob job = new ClientSideJob(jt.getTask(), id, this);
                    jobs.put(job.getId(), job);
                    exec.submit(new Runnable() {
                        @Override
                        public void run() {
                            assignmentListener.receiveTask(job);
                        }
                    });
                    return GenericResponses.OK;
                case JobMessageHeaders.JOB_CANCEL:
                    jobs.remove(id);
                    exec.submit(new Runnable() {
                        @Override
                        public void run() {
                            assignmentListener.cancelTask(jobs.get(id));
                        }
                    });
                    return GenericResponses.OK;
                default:
                    return GenericResponses.UNKNOWN_DATA;
            }
        } else {
            return GenericResponses.ILLEGAL_DATA;
        }
    }
}