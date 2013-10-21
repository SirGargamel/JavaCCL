package cz.tul.javaccl.exceptions;

/**
 * Exception signaling that comething went wrong during the communication
 * process.
 *
 * @author Petr Jecmen
 */
public class ConnectionException extends Exception {

    private final ConnectionExceptionCause exceptionCause;

    /**
     * Creates a new instance of <code>ConnectionException</code> withgiven
     * cause.
     *
     * @param cause cause of the exception
     */
    public ConnectionException(ConnectionExceptionCause cause) {
        this.exceptionCause = cause;
    }

    /**
     * @return cause of the exception
     */
    public ConnectionExceptionCause getExceptionCause() {
        return exceptionCause;
    }
}
