package cz.tul.comm.messaging.job;

import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.messaging.Message;
import cz.tul.comm.messaging.MessageHeaders;
import cz.tul.comm.server.IDataStorage;
import cz.tul.comm.socket.IListenerRegistrator;
import cz.tul.comm.socket.queue.IIdentifiable;
import cz.tul.comm.socket.queue.IListener;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Petr Ječmen
 */
public class ServerSideJob implements Job, IListener {

    private static final Logger log = Logger.getLogger(ServerSideJob.class.getName());
    private Communicator comm;
    private final IListenerRegistrator listenerRegistrator;
    private final IDataStorage dataStorage;
    private final Object task;
    private final UUID jobId;
    private Object result;
    private JobStatus jobStatus;

    ServerSideJob(final Object task, final IListenerRegistrator listenerRegistrator, IDataStorage dataStorage) {
        this.task = task;
        jobStatus = JobStatus.SUBMITTED;
        jobId = UUID.randomUUID();
        this.listenerRegistrator = listenerRegistrator;
        this.dataStorage = dataStorage;
    }

    /**
     * Assign job to some client and send task to him.
     * @param comm Client communicator
     */
    public void submitJob(final Communicator comm) {
        this.comm = comm;
        listenerRegistrator.addIdListener(jobId, this, true);

        final JobTask jt = new JobTask(jobId, task);
        final Message m = new Message(jobId, MessageHeaders.JOB, jt);
        sendMessage(m);

        jobStatus = JobStatus.SENT;
    }

    @Override
    public Object getTask() {
        return task;
    }

    @Override
    public Object getResult(final boolean blockingGet) {
        // TODO wait for result
        if (blockingGet) {
            while (result == null) {
                synchronized (this) {
                    try {
                        this.wait();
                    } catch (InterruptedException ex) {
                        log.log(Level.WARNING, "Job waiting for result has been interrupted", ex);
                    }
                }
            }
        }

        return result;
    }

    @Override
    public void cancelJob() {
        jobStatus = JobStatus.CANCELED;
        final Message m = new Message(jobId, JobMessageHeaders.JOB_CANCEL, null);
        sendMessage(m);
        listenerRegistrator.removeIdListener(jobId, this);
        log.log(Level.CONFIG, "Job with ID {0} has been cancelled by server.", jobId);
    }

    @Override
    public JobStatus getStatus() {
        return jobStatus;
    }

    @Override
    public boolean isDone() {
        return jobStatus.equals(JobStatus.COMPUTED) || isCanceled();
    }

    @Override
    public boolean isCanceled() {
        return jobStatus.equals(JobStatus.ERROR) || jobStatus.equals(JobStatus.CANCELED);
    }

    @Override
    public void receiveData(IIdentifiable data) {
        if (data instanceof Message) {
            final Message m = (Message) data;
            switch (m.getHeader()) {
                case JobMessageHeaders.JOB_RESULT:
                    result = m.getData();
                    synchronized (this) {
                        this.notify();
                    }
                    listenerRegistrator.removeIdListener(jobId, this);
                    log.log(Level.CONFIG, "Result for job with ID {0} has been received.", jobId);
                    break;
                case JobMessageHeaders.JOB_DATA_REQUEST:
                    final Object reqData;
                    if (dataStorage != null) {
                        final Object dataId = m.getData();
                        reqData = dataStorage.requestData(dataId);
                    } else {
                        reqData = null;
                        log.warning("Data storage is unset, null data sent to client.");
                    }
                    final Message response = new Message(jobId, JobMessageHeaders.JOB_DATA_REQUEST, reqData);
                    sendMessage(response);
                    log.log(Level.CONFIG, "Requested data for job with ID {0} has been sent.", jobId);
                    break;
                case JobMessageHeaders.JOB_CANCEL:
                    jobStatus = JobStatus.CANCELED;
                    listenerRegistrator.removeIdListener(jobId, this);
                    log.log(Level.CONFIG, "Job with ID {0} has been cancelled by client, reason was {1}.", new Object[]{jobId, m.getData()});
                    break;
                case JobMessageHeaders.JOB_ACCEPT:
                    jobStatus = JobStatus.ASSIGNED;
                    break;
                default:
                    log.log(Level.WARNING, "Data with invalid header received for job - {0}", m.toString());
            }
        } else {
            log.log(Level.WARNING, "Illegal job data received - {0}", data.toString());
        }
    }

    private boolean sendMessage(final Message m) {
        if (comm != null) {
            return comm.sendData(m);
        } else {
            log.warning("No Communicator set for Job.");
            return false;
        }
    }

    @Override
    public UUID getId() {
        return jobId;
    }
    
    Communicator getComm() {
        return comm;
    }
}
