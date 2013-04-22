package cz.tul.comm.job;

import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.messaging.Message;
import cz.tul.comm.messaging.MessageHeaders;
import cz.tul.comm.server.DataStorage;
import cz.tul.comm.socket.ListenerRegistrator;
import cz.tul.comm.socket.queue.Identifiable;
import cz.tul.comm.socket.queue.Listener;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Petr Ječmen
 */
public class ServerSideJob implements Job, Listener {

    private static final Logger log = Logger.getLogger(ServerSideJob.class.getName());
    private Communicator comm;
    private final ListenerRegistrator listenerRegistrator;
    private final DataStorage dataStorage;
    private final Object task;
    private final UUID jobId;
    private Object result;
    private JobStatus jobStatus;

    ServerSideJob(final Object task, final ListenerRegistrator listenerRegistrator, DataStorage dataStorage) {
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
        jobStatus = JobStatus.SENT;
        sendMessage(m);        
    }

    @Override
    public Object getTask() {
        return task;
    }

    @Override
    public Object getResult(final boolean blockingGet) {        
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

    public void setStatus(JobStatus jobStatus) {
        this.jobStatus = jobStatus;
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
    public void receiveData(Identifiable data) {
        if (data instanceof Message) {
            final Message m = (Message) data;
            switch (m.getHeader()) {
                case JobMessageHeaders.JOB_RESULT:
                    jobStatus = JobStatus.COMPUTED;
                    result = m.getData();                    
                    listenerRegistrator.removeIdListener(jobId, this);
                    log.log(Level.CONFIG, "Result for job with ID {0} has been received.", jobId);
                    synchronized (this) {
                        this.notify();
                    }
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
