package cz.tul.comm.communicator;

/**
 * Possible client statuses.
 *
 * @author Petr Ječmen
 */
public enum Status {

    /**
     * Client could not be contacted (eg. Socket could not be opened).
     */
    OFFLINE,
    /**
     * Client is aviable.
     */
    ONLINE,
}
