package cz.tul.comm.server.daemons;

import cz.tul.comm.IService;
import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.communicator.Status;
import cz.tul.comm.gui.UserLogging;
import cz.tul.comm.messaging.Message;
import cz.tul.comm.messaging.MessageHeaders;
import cz.tul.comm.server.IClientManager;
import cz.tul.comm.socket.IListenerRegistrator;
import cz.tul.comm.socket.queue.IIdentifiable;
import cz.tul.comm.socket.queue.IListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Daemon asking clients what is their status.
 *
 * @author Petr Ječmen
 */
public class ClientStatusDaemon extends Thread implements IService, IListener {

    private static final Logger log = Logger.getLogger(ClientStatusDaemon.class.getName());
    private static final int DELAY = 5000;
    private static final int TIMEOUT = 500;
    private boolean run;
    private final IClientManager clientManager;
    private final IListenerRegistrator listenerRegistrator;
    private final Map<UUID, Communicator> responses;

    /**
     *
     * @param clientManager client manager
     * @param listenerRegistrator listener registrator for easy response
     * obtaining
     */
    public ClientStatusDaemon(final IClientManager clientManager, final IListenerRegistrator listenerRegistrator) {
        if (clientManager == null || listenerRegistrator == null) {
            throw new IllegalArgumentException("NULL arguments not allowed");
        }

        this.clientManager = clientManager;
        this.listenerRegistrator = listenerRegistrator;

        responses = new HashMap<>();
        run = true;
    }

    @Override
    public void run() {
        Set<Communicator> clients;
        Message m;
        UUID id;
        while (run) {
            clients = clientManager.getClients();
            for (Communicator c : clients) {
                final Status currentStatus = c.checkStatus();
                if (currentStatus == Status.OFFLINE || currentStatus == Status.NA) {
                    continue;
                }

                id = UUID.randomUUID();
                m = new Message(id, MessageHeaders.STATUS, null);

                listenerRegistrator.addIdListener(id, this, true);
                responses.put(id, c);

                if (!c.sendData(m, TIMEOUT)) {
                    c.setStatus(Status.OFFLINE);                    
                    log.log(Level.INFO, "Client {0}:{1} could not be contacted.", new Object[]{c.getAddress().getHostAddress(), c.getPort()});
                } else {
                    c.setStatus(Status.NOT_RESPONDING);
                    log.log(Level.INFO, "Status request to {0}:{1} sent successfully.", new Object[]{c.getAddress().getHostAddress(), c.getPort()});
                }
            }

            synchronized (this) {
                try {
                    this.wait(DELAY);
                } catch (InterruptedException ex) {
                    log.log(Level.WARNING, "Waiting of ClientStatus thread interrupted.", ex);
                }
            }
        }
    }

    @Override
    public void stopService() {
        run = false;
        log.config("ClientStatusDaemon has been stopped.");
    }

    @Override
    public void receiveData(final IIdentifiable data) {
        if (data instanceof Message) {
            final Message m = (Message) data;

            if (m.getHeader().equals(MessageHeaders.STATUS)
                    && m.getData() instanceof Status) {
                final UUID id = m.getId();
                final Communicator c = responses.get(id);
                final Status status = (Status) m.getData();

                try {
                    c.setStatus(status);
                    log.log(Level.INFO, "{0}:{1} status report - {2}.", new Object[]{c.getAddress().getHostAddress(), c.getPort(), status});
                } catch (IllegalArgumentException ex) {
                    log.log(Level.WARNING, "Illegal data received as status.", ex);
                }

                listenerRegistrator.removeIdListener(id, this);
                responses.remove(id);
            }
        }
    }
}
