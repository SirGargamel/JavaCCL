package cz.tul.comm.exceptions;

/**
 *
 * @author Petr Jecmen
 */
public class ConnectionException extends Exception {

    /**
     * Creates a new instance of
     * <code>ConnectionException</code> without detail message.
     */
    public ConnectionException() {
    }

    /**
     * Constructs an instance of
     * <code>ConnectionException</code> with the specified detail message.
     *
     * @param msg the detail message.
     */
    public ConnectionException(String msg) {
        super(msg);
    }
}
