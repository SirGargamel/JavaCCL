package cz.tul.comm.server;

import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.communicator.CommunicatorImpl;
import cz.tul.comm.communicator.DataPacket;
import cz.tul.comm.job.JobRequestManager;
import cz.tul.comm.messaging.Message;
import cz.tul.comm.messaging.MessageHeaders;
import java.util.Observable;
import java.util.Observer;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Observer handling system messages and its effects.
 *
 * @author Petr Ječmen
 */
public class SystemMessagesHandler implements Observer {
    
    private static final Logger log = Logger.getLogger(SystemMessagesHandler.class.getName());
    private final ClientManager clientManager;
    private final JobRequestManager jobRequestManager;

    /**
     * @param clientManager client manager
     * @param jobRequestManager interface for asking for extra jobs
     */
    public SystemMessagesHandler(ClientManager clientManager, JobRequestManager jobRequestManager) {
        if (clientManager != null) {
            this.clientManager = clientManager;
        } else {
            throw new NullPointerException("NULL client manager not allowed.");
        }
        if (jobRequestManager != null) {
            this.jobRequestManager = jobRequestManager;
        } else {
            throw new NullPointerException("NULL job request manager not allowed.");
        }
        
    }
    
    @Override
    public void update(Observable o, Object arg) {
        if (arg instanceof DataPacket) {
            final DataPacket ipData = (DataPacket) arg;
            final Object data = ipData.getData();
            if (data instanceof Message) {
                final Message m = (Message) data;
                final String header = m.getHeader();
                switch (header) {
                    case MessageHeaders.LOGIN:
                        int port;
                        try {
                            port = Integer.parseInt(m.getData().toString());
                            UUID clientId = UUID.randomUUID();
                            Communicator c = clientManager.registerClient(ipData.getSourceIP(), port);
                            if (c instanceof CommunicatorImpl) {
                                ((CommunicatorImpl) c).setId(clientId);
                            }
                            if (c != null) {
                                c.sendData(new Message(m.getId(), header, clientId));
                            } else {
                                log.warning("Error registering new client.");
                            }
                            log.log(Level.CONFIG, "LOGIN received from {0}, assigning id {1}", new Object[]{ipData.getSourceIP().getHostAddress(), clientId});
                        } catch (NumberFormatException ex) {
                            log.log(Level.WARNING, "Illegal login data received from {0} - {1}",
                                    new Object[]{ipData.getSourceIP().getHostAddress(), m.getData().toString()});
                        } catch (NullPointerException ex) {
                            log.log(Level.WARNING, "Null login data received from {0}",
                                    new Object[]{ipData.getSourceIP().getHostAddress()});
                        }
                        break;
                    case MessageHeaders.LOGOUT:
                        final Object id = m.getData();
                        if (id instanceof UUID) {
                            clientManager.deregisterClient((UUID) id);
                            log.log(Level.CONFIG, "Client with id {0} deregistered.", ipData.getClientID());
                        } else {
                            log.log(Level.WARNING, "Invalid client id received - {0}", id.toString());
                        }
                        break;
                    case MessageHeaders.JOB_REQUEST:
                        // TODO
                        final UUID clientId = ipData.getClientID();
                        final Communicator comm = clientManager.getClient(clientId);
                        if (comm != null) {
                            jobRequestManager.requestJob(comm);
                        } else {
                            log.warning("Client without id requested a job.");
                        }
                        
                        break;
                    default:
                        // nonsystem message, no action
                        break;
                }
            }
        } else {
            log.log(Level.WARNING, "Invalid data received", arg);
        }
    }
}
