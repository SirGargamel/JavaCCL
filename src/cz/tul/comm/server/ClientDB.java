package cz.tul.comm.server;

import cz.tul.comm.socket.Communicator;
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
final class ClientDB {

    private static final Logger log = Logger.getLogger(Communicator.class.getName());
    private final Set<Communicator> clients;

    ClientDB() {
        clients = Collections.synchronizedSet(new HashSet<Communicator>());
    }

    Communicator registerClient(final InetAddress adress, final int port) {
        Communicator cc = getClient(adress);
        if (cc == null) {
            try {
                cc = new Communicator(adress, port);
                clients.add(cc);
            } catch (IllegalArgumentException ex) {
                log.log(Level.WARNING, "Invalid Communicator parameters.", ex);
            }
        }

        return cc;
    }

    void deregisterClient(final InetAddress adress) {
        final Iterator<Communicator> i = clients.iterator();
        while (i.hasNext()) {
            if (i.next().getAddress().equals(adress)) {
                i.remove();
                break;
            }
        }

    }

    Communicator getClient(final InetAddress adress) {
        for (Communicator cc : clients) {
            if (cc.getAddress().equals(adress)) {
                return cc;
            }
        }
        return null;
    }

    Set<Communicator> getClients() {
        return Collections.unmodifiableSet(clients);
    }
}
