package cz.tul.comm.communicator;

/**
 * Aviable client statuses.
 *
 * @author Petr Ječmen
 */
public enum Status {

    /**
     * Client is performing computation and is unable to perform any other work.
     */
    BUSY,
    /**
     * Client is not aviable (default client status).
     */
    NA,
    /**
     * Client could be contacted, but is not responding.
     */
    NOT_RESPONDING,
    /**
     * Client could not be contacted (eg. Socket could nou be opened).
     */
    OFFLINE,
    /**
     * Client is aviable and able to perform computation.
     */
    ONLINE,
    REACHABLE;
}
