package cz.tul.comm.communicator;

/**
 * Possible client statuses.
 *
 * @author Petr Ječmen
 */
public enum Status {
    
    /**
     * Client is not aviable (default client status).
     */
    NA,
    /**
     * Client could be contacted, but is not responding.
     */
    NOT_RESPONDING,
    /**
     * Client could not be contacted (eg. Socket could not be opened).
     */
    OFFLINE,
    /**
     * Client is aviable.
     */
    ONLINE,
    /**
     * Client could be contacted, but failed to confirm data receive.
     */
    REACHABLE;
}
