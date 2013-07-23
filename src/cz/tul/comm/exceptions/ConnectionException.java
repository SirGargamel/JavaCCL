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
    public ConnectionException() {
        exceptionCause = ConnectionExceptionCause.UNKNOWN;
    }

    /**
     * Constructs an instance of
     * <code>ConnectionException</code> with the specified detail message.
     *
     * @param msg the detail message.
     */
    public ConnectionException(String msg, ConnectionExceptionCause cause) {
        super(msg);
        this.exceptionCause = cause;
    }

    public ConnectionExceptionCause getExceptionCause() {
        return exceptionCause;
    }
}
