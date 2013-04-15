package cz.tul.comm.server;

import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.history.IHistoryManager;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
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
class ClientDB implements IClientManager {

    private static final Logger log = Logger.getLogger(Communicator.class.getName());
    private final Set<Communicator> clients;
    private IHistoryManager hm;

    ClientDB() {
        clients = Collections.synchronizedSet(new HashSet<Communicator>());
    }

    @Override
    public Communicator registerClient(final InetAddress address, final int port) {
        Communicator cc = getClient(address, port);
        if (cc == null) {
            try {
                cc = Communicator.initNewCommunicator(address, port);
                if (cc != null) {
                    cc.registerHistory(hm);
                    clients.add(cc);
                    log.log(Level.CONFIG, "New client with IP {0} on port {1} registered", new Object[]{address.getHostAddress(), port});
                }
            } catch (IllegalArgumentException ex) {
                log.log(Level.WARNING, "Invalid Communicator parameters.", ex);
            }
        } else {
            log.log(Level.CONFIG, "Client with IP {0} on port {1} is already registered, no changes made", new Object[]{address.getHostAddress(), port});
        }

        return cc;
    }

    @Override
    public void deregisterClient(final UUID id) {
        final Iterator<Communicator> i = clients.iterator();
        Communicator cc;
        while (i.hasNext()) {
            cc = i.next();
            if (cc.getId().equals(id)) {
                i.remove();
                break;
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

        for (Communicator c : clients) {
            if (id.equals(c.getId())) {
                result = c;
                break;
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
    public void registerHistory(final IHistoryManager hm) {
        this.hm = hm;
        log.fine("History registered.");
    }
}
