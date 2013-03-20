package cz.tul.comm.server.daemons;

import cz.tul.comm.IService;
import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.communicator.Status;
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
                id = UUID.randomUUID();
                m = new Message(id, MessageHeaders.STATUS, null);

                listenerRegistrator.addIdListener(id, this, true);
                responses.put(id, c);

                c.sendData(m, TIMEOUT);
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
                } catch (IllegalArgumentException ex) {
                    log.log(Level.WARNING, "Illegal data received as status.", ex);
                }

                listenerRegistrator.removeIdListener(id, this);
                responses.remove(id);
            }
        }
    }
}
