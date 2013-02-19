package cz.tul.comm.server;

import cz.tul.comm.socket.Communicator;
import cz.tul.comm.client.Comm_Client;
import java.net.InetAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Database for client handling. Basic registry with additional
 * info about clients.
 * @author Petr Ječmen
 */
public final class ClientDB {

    private final Set<Communicator> clients;

    public ClientDB() {
        clients = new HashSet<>();
    }

    public Communicator registerClient(final InetAddress adress) {
        Communicator cc = getClient(adress);
        if (cc == null) {
            cc = new Communicator(adress, Comm_Client.PORT);
            if (cc != null) {
                clients.add(cc);
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
