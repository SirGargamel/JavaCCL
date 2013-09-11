package cz.tul.javaccl.exceptions;

/**
 *
 * @author Petr Jecmen
 */
public class ConnectionException extends Exception {
    
    private final ConnectionExceptionCause exceptionCause;

    /**
     * Creates a new instance of
     * <code>ConnectionException</code> without detail message.
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
