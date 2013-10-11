package cz.tul.javaccl.exceptions;

/**
 *
 * @author Petr Jecmen
 */
public enum ConnectionExceptionCause {

    /**
     * UUID cannot communcate with target.
     */
    UUID_NOT_ALLOWED,
    /**
     * Connection timed out.
     */
    TIMEOUT,
    /**
     * Target UUID and targets UUID do not match.
     */
    WRONG_TARGET,
    /**
     * Could not create a connection to target.
     */
    CONNECTION_ERROR,
    /**
     * Could not reach target.
     */
    TARGET_OFFLINE,
    /**
     * Unknow error occured.
     */
    UNKNOWN
}
