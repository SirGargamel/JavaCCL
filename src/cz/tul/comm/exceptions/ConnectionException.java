package cz.tul.comm.exceptions;

/**
 *
 * @author Petr Jecmen
 */
public class ConnectionException extends Exception {
    
    private final ConnectionExceptionCause exceptionCause;

    /**
     * Creates a new instance of
     * <code>ConnectionException</code> without detail message.
     */
    public ConnectionException(ConnectionExceptionCause cause) {
        this.exceptionCause = cause;
    }

    public ConnectionExceptionCause getExceptionCause() {
        return exceptionCause;
    }
}
