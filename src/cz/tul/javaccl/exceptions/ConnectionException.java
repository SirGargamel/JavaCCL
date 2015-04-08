package cz.tul.javaccl.exceptions;

/**
 * Exception signaling that comething went wrong during the communication
 * process.
 *
 * @author Petr Jecmen
 */
public class ConnectionException extends Exception {

    private final ConnectionExceptionCause exceptionCause;
    private final String response;

    /**
     * Creates a new instance of <code>ConnectionException</code> withgiven
     * cause.
     *
     * @param cause cause of the exception
     * @param response response from the opposite side
     */
    public ConnectionException(ConnectionExceptionCause cause, String response) {
        this.exceptionCause = cause;
        this.response = response;
    }
    
    /**
     * Creates a new instance of <code>ConnectionException</code> withgiven
     * cause.
     *
     * @param cause cause of the exception
     */
    public ConnectionException(ConnectionExceptionCause cause) {        
        this(cause, "No Response");
    }    

    /**
     * @return cause of the exception
     */
    public ConnectionExceptionCause getExceptionCause() {
        return exceptionCause;
    }
    
    public String getResponse() {
        return response;
    }
    
    @Override
    public String toString() {
        return exceptionCause.toString() + ": " + response;
    }
}
