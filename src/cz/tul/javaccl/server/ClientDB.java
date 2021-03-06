package cz.tul.javaccl.server;

import static cz.tul.javaccl.CCLObservable.REGISTER;
import cz.tul.javaccl.GlobalConstants;
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
class ClientDB extends ClientManager implements Observer, IDFilter {

    private static final Logger LOG = Logger.getLogger(ClientDB.class.getName());
    private final Set<Communicator> clients;
    private Collection<UUID> allowedIDs;
    private HistoryManager hm;
    private final UUID localId;

    ClientDB(final UUID localId) {
        clients = new HashSet<Communicator>();
        allowedIDs = Collections.emptySet();
        allowedIDs = Collections.unmodifiableCollection(allowedIDs);
        this.localId = localId;
    }

    @Override
    public Communicator registerClient(final InetAddress address, final int port) throws IllegalArgumentException, ConnectionException {
        final CommunicatorInner cc = CommunicatorImpl.initNewCommunicator(address, port, localId);
        cc.registerHistory(hm);
        cc.addObserver(this);
        clients.add(cc);

        final Message m = new Message(GlobalConstants.ID_SYS_MSG, SystemMessageHeaders.LOGIN, localId);
        final Object id = cc.sendData(m);
        if (id instanceof UUID) {
            cc.setTargetId((UUID) id);

            prepareAllowedIDs();

            notifyChange(REGISTER, new Object[]{address, port, id});
            LOG.log(Level.INFO, "New client with IP " + address.getHostAddress() + " on port " + port + " registered with ID " + id);
        } else {
            LOG.warning("Illegal login response received - " + id);
        }

        return cc;
    }

    @Override
    public Communicator addClient(final InetAddress address, final int port, final UUID clientId) {
        final CommunicatorInner ccI = CommunicatorImpl.initNewCommunicator(address, port, localId);
        ccI.registerHistory(hm);
        ccI.addObserver(this);
        ccI.setTargetId(clientId);

        clients.add(ccI);
        prepareAllowedIDs();

        notifyChange(REGISTER, new Object[]{address, port, clientId});
        LOG.log(Level.INFO, "New client with IP " + address.getHostAddress() + " on port " + port + " with ID " + clientId + " registered");

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
        notifyChange(DEREGISTER, new Object[]{id});
        LOG.log(Level.INFO, "Client with ID " + id + " deregistered");
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
        return localId.equals(id);
    }

    @Override
    public UUID getLocalID() {
        return localId;
    }
}
