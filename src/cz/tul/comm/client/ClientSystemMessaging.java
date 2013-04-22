package cz.tul.comm.client;

import cz.tul.comm.communicator.DataPacket;
import cz.tul.comm.messaging.Message;
import cz.tul.comm.messaging.MessageHeaders;
import cz.tul.comm.job.ClientSideJob;
import cz.tul.comm.job.JobTask;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class handling system messages for client side.
 *
 * @author Petr Ječmen
 */
class ClientSystemMessaging implements Observer {

    private static final Logger log = Logger.getLogger(ClientSystemMessaging.class.getName());
    private final Comm_Client parent;

    ClientSystemMessaging(Comm_Client parent) {
        this.parent = parent;
    }

    @Override
    public void update(Observable o, Object arg) {
        if (arg instanceof DataPacket) {
            final DataPacket data = (DataPacket) arg;
            if (data.getData() instanceof Message) {
                final Message m = (Message) data.getData();
                switch (m.getHeader()) {
                    case MessageHeaders.JOB:
                        if (parent.getAssignmentListener() != null) {
                            final Object task = m.getData();
                            if (task != null && task instanceof JobTask) {
                                final JobTask jt = (JobTask) task;
                                final ClientSideJob job = new ClientSideJob(jt.getTask(), jt.getJobId(), parent.getServerComm(), parent.getListenerRegistrator(), parent.getAssignmentListener());
                                parent.getListenerRegistrator().addIdListener(job.getId(), job, true);
                                parent.getAssignmentListener().receiveTask(job);
                            } else {
                                log.warning("NULL job task received");
                            }
                        } else {
                            log.warning("No assignment listener set, job cannot be computed.");
                        }
                        break;
                    default:
                        // nonsystem msg, nothing to do
                        break;
                }
            } else {
                log.log(Level.WARNING, "Non message data received.", arg);
            }
        }
    }
}
