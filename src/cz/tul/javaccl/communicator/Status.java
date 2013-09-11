package cz.tul.javaccl.communicator;

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
    /**
     * Target could not be reached directly, but he can make connection to local
     * instance and retrieve messages.
     */
    PASSIVE
}
