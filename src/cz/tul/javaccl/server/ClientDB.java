package cz.tul.javaccl.server;

import cz.tul.javaccl.Constants;
import cz.tul.javaccl.communicator.Communicator;
import cz.tul.javaccl.communicator.CommunicatorImpl;
import cz.tul.javaccl.communicator.CommunicatorInner;
import cz.tul.javaccl.exceptions.ConnectionException;
import cz.tul.javaccl.history.HistoryManager;
import cz.tul.javaccl.messaging.Message;
import cz.tul.javaccl.messaging.SystemMessageHeaders;
import cz.tul.javaccl.socket.IDFilter;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Database for client handling. Basic registry with additional info about
 * clients.
 *
 * @author Petr Ječmen
 */
class ClientDB implements ClientManager, Observer, IDFilter {

    private static final Logger log = Logger.getLogger(ClientDB.class.getName());
    private final Set<Communicator> clients;
    private Collection<UUID> allowedIDs;
    private HistoryManager hm;

    ClientDB() {
        clients = Collections.synchronizedSet(new HashSet<Communicator>());        
        allowedIDs = Collections.emptySet();
        allowedIDs = Collections.unmodifiableCollection(allowedIDs);
    }

    @Override
    public Communicator registerClient(final InetAddress address, final int port) throws IllegalArgumentException, ConnectionException {
        CommunicatorInner cc = CommunicatorImpl.initNewCommunicator(address, port);
        if (cc != null) {
            cc.registerHistory(hm);
            cc.addObserver(this);
            cc.setSourceId(Constants.ID_SERVER);
            clients.add(cc);

            final UUID id = UUID.randomUUID();
            final Message m = new Message(Constants.ID_SYS_MSG, SystemMessageHeaders.LOGIN, id);
            cc.sendData(m);
            cc.setTargetId(id);

            prepareAllowedIDs();

            log.log(Level.CONFIG, "New client with IP {0} on port {1} registered", new Object[]{address.getHostAddress(), port});
        }

        return cc;
    }

    @Override
    public Communicator addClient(final InetAddress address, final int port, final UUID clientId) {
        CommunicatorInner ccI = CommunicatorImpl.initNewCommunicator(address, port);
        if (ccI != null) {
            ccI.registerHistory(hm);
            ccI.addObserver(this);
            ccI.setSourceId(Constants.ID_SERVER);
            ccI.setTargetId(clientId);

            clients.add(ccI);
            prepareAllowedIDs();

            log.log(Level.CONFIG, "New client with IP {0} on port {1} with ID {2} registered", new Object[]{address.getHostAddress(), port, clientId});
        }

        return ccI;
    }

    @Override
    public void deregisterClient(final UUID id) {
        final Iterator<Communicator> i = clients.iterator();
        Communicator cc;
        UUID ccId;
        synchronized (clients) {
            while (i.hasNext()) {
                cc = i.next();
                ccId = cc.getTargetId();
                if (ccId != null && ccId.equals(id)) {
                    i.remove();
                    prepareAllowedIDs();
                    break;
                }
            }
        }
        log.log(Level.CONFIG, "Client with ID {0} deregistered", new Object[]{id});
    }

    @Override
    public Communicator getClient(final InetAddress adress, final int port) {
        for (Communicator cc : clients) {
            if (cc.getAddress().equals(adress) && cc.getPort() == port) {
                return cc;
            }
        }
        return null;
    }

    @Override
    public Communicator getClient(final UUID id) {
        Communicator result = null;

        if (id != null) {
            for (Communicator c : clients) {
                if (id.equals(c.getTargetId())) {
                    result = c;
                    break;
                }
            }
        }

        return result;
    }

    @Override
    public Collection<Communicator> getClients() {
        return Collections.unmodifiableSet(clients);
    }

    /**
     * Register history manager that will store info about received messages.
     *
     * @param hm instance of history manager
     */
    public void registerHistory(final HistoryManager hm) {
        this.hm = hm;
        log.fine("History registered.");
    }

    @Override
    public void update(Observable o, Object arg) {
        if (o instanceof Communicator && arg instanceof UUID) {
            prepareAllowedIDs();
        }
    }

    private void prepareAllowedIDs() {
        Collection<UUID> ids = new HashSet<UUID>();
        UUID id;
        for (Communicator comm : clients) {
            id = comm.getTargetId();
            if (id != null) {
                ids.add(id);
            }
        }
        allowedIDs = Collections.unmodifiableCollection(ids);
    }

    @Override
    public boolean isIdAllowed(UUID id) {
        return allowedIDs.contains(id);
    }

    @Override
    public boolean isTargetIdValid(UUID id) {
        return Constants.ID_SERVER.equals(id);
    }

    @Override
    public UUID getLocalID() {
        return Constants.ID_SERVER;
    }
}
