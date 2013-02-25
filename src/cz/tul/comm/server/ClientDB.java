package cz.tul.comm.server;

import cz.tul.comm.gui.UserLogging;
import cz.tul.comm.socket.Communicator;
import java.net.InetAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Database for client handling. Basic registry with additional info about
 * clients.
 *
 * @author Petr Ječmen
 */
public final class ClientDB {

    private final Set<Communicator> clients;

    public ClientDB() {
        clients = Collections.synchronizedSet(new HashSet<Communicator>());
    }

    public Communicator registerClient(final InetAddress adress, final int port) {
        Communicator cc = getClient(adress);
        if (cc == null) {
            try {
                cc = new Communicator(adress, port);
                clients.add(cc);
            } catch (IllegalArgumentException ex) {
                UserLogging.showWarningToUser(ex.getLocalizedMessage());
            }
        }

        return cc;
    }

    public void deregisterClient(final InetAddress adress) {
        final Iterator<Communicator> i = clients.iterator();
        while (i.hasNext()) {
            if (i.next().getAddress().equals(adress)) {
                i.remove();
                break;
            }
        }

    }

    public Communicator getClient(final InetAddress adress) {
        for (Communicator cc : clients) {
            if (cc.getAddress().equals(adress)) {
                return cc;
            }
        }
        return null;
    }

    public Set<Communicator> getClients() {
        return Collections.unmodifiableSet(clients);
    }
}
