package cz.tul.comm.server.daemons;

import cz.tul.comm.IService;
import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.server.ClientManager;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Daemon asking clients what is their status.
 *
 * @author Petr Ječmen
 */
public class ClientStatusDaemon extends Thread implements IService {

    private static final Logger log = Logger.getLogger(ClientStatusDaemon.class.getName());
    private static final int DELAY = 5_000;  
    private boolean run;
    private final ClientManager clientManager;

    /**
     *
     * @param clientManager client manager
     * @param listenerRegistrator listener registrator for easy response
     * obtaining
     */
    public ClientStatusDaemon(final ClientManager clientManager) {
        if (clientManager == null) {
            throw new IllegalArgumentException("NULL arguments not allowed");
        }

        this.clientManager = clientManager;        
        
        run = true;
    }

    @Override
    public void run() {
        Collection<Communicator> clients;
        while (run) {
            clients = clientManager.getClients();
            for (Communicator c : clients) {
                c.checkStatus();                
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
        log.fine("ClientStatusDaemon has been stopped.");
    }
}
