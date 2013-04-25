package cz.tul.comm.job;

import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.messaging.BasicConversator;
import cz.tul.comm.messaging.Message;
import cz.tul.comm.socket.ListenerRegistrator;
import cz.tul.comm.socket.queue.Identifiable;
import cz.tul.comm.socket.queue.Listener;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client side of job compuatation. Handles client operations.
 * @author Petr Ječmen
 */
public class ClientSideJob implements Assignment, Listener {

    private static final Logger log = Logger.getLogger(ClientSideJob.class.getName());
    private final Object task;
    private final ListenerRegistrator listenerRegistrator;
    private final AssignmentListener taskListener;
    private final Communicator comm;
    private final UUID jobId;
    private JobStatus jobStatus;
    private boolean isDone;

    /**
     * Create new instance.
     *
     * @param task task for computation
     * @param jobId jobs ID
     * @param comm server communicator
     * @param listenerRegistrator listener registrator
     * @param taskListener task listener for job cancelation
     */
    public ClientSideJob(final Object task, final UUID jobId, final Communicator comm, final ListenerRegistrator listenerRegistrator, final AssignmentListener taskListener) {
        this.task = task;
        this.comm = comm;
        this.jobId = jobId;
        this.listenerRegistrator = listenerRegistrator;
        this.taskListener = taskListener;

        final Message accept = new Message(jobId, JobMessageHeaders.JOB_ACCEPT, null);
        sendMessage(accept);
        
        isDone = false;
        jobStatus = JobStatus.ASSIGNED;
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
    public Object requestData(Object dataId) {
        final Message m = new Message(jobId, JobMessageHeaders.JOB_DATA_REQUEST, dataId);
        BasicConversator bc = new BasicConversator(comm, listenerRegistrator);
        final Object reply = bc.sendAndReceiveData(m);
        if (reply instanceof Message) {
            final Message r = (Message) reply;
            return r.getData();
        } else {
            throw new IllegalArgumentException("Illegal data received as a reply for data request.");
        }
    }

    @Override
    public boolean submitResult(Object result) {
        listenerRegistrator.removeIdListener(jobId, this);
        isDone = true;
        jobStatus = JobStatus.COMPUTED;
        final Message m = new Message(jobId, JobMessageHeaders.JOB_RESULT, result);
        return sendMessage(m);
    }

    @Override
    public Object getTask() {
        return task;
    }

    @Override
    public void cancel(final String reason) {
        jobStatus = JobStatus.CANCELED;
        final Message m = new Message(jobId, JobMessageHeaders.JOB_CANCEL, reason);
        sendMessage(m);
        listenerRegistrator.removeIdListener(jobId, this);
    }

    @Override
    public void receiveData(Identifiable data) {
        if (data instanceof Message) {
            final Message m = (Message) data;
            switch (m.getHeader()) {
                case JobMessageHeaders.JOB_CANCEL:
                    jobStatus = JobStatus.CANCELED;
                    listenerRegistrator.removeIdListener(jobId, this);
                    taskListener.cancelTask(this);
                    log.log(Level.CONFIG, "Job has been cancelled by server.");
                    break;
                // TODO
                case JobMessageHeaders.JOB_DATA_REQUEST:
                    // nothing, handled by conversator 
                    break;
                default:
                    log.log(Level.WARNING, "Data with invalid header received for job - {0}", m.toString());
                    break;
            }
        } else {
            log.log(Level.WARNING, "Illegal job data received - {0}", data.toString());
        }
    }

    @Override
    public UUID getId() {
        return jobId;
    }

    @Override
    public boolean isDone() {
        return isDone;
    }

    public JobStatus getStatus() {
        return jobStatus;
    }
}
