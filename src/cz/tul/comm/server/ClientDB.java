package cz.tul.comm.server;

import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.history.IHistoryManager;
import java.net.InetAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Database for client handling. Basic registry with additional info about
 * clients.
 *
 * @author Petr Ječmen
 */
final class ClientDB implements IClientManager {

    private static final Logger log = Logger.getLogger(Communicator.class.getName());
    private final Set<Communicator> clients;
    private IHistoryManager hm;

    ClientDB() {
        clients = Collections.synchronizedSet(new HashSet<Communicator>());
    }

    @Override
    public Communicator registerClient(final InetAddress adress, final int port) {
        Communicator cc = getClient(adress, port);
        if (cc == null) {
            try {
                cc = new Communicator(adress, port);
                cc.registerHistory(hm);
                clients.add(cc);
            } catch (IllegalArgumentException ex) {
                log.log(Level.WARNING, "Invalid Communicator parameters.", ex);
            }
        }

        return cc;
    }

    @Override
    public void deregisterClient(final InetAddress adress, final int port) {
        final Iterator<Communicator> i = clients.iterator();
        Communicator cc;
        while (i.hasNext()) {
            cc = i.next();
            if (cc.getAddress().equals(adress) && cc.getPort() == port) {
                i.remove();
                break;
            }
        }

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
    public Set<Communicator> getClients() {
        return Collections.unmodifiableSet(clients);
    }
    
    /**
     * Register history manager that will store info about received messages.
     *
     * @param hm instance of history manager
     */
    public void registerHistory(final IHistoryManager hm) {
        this.hm = hm;
    }
}
