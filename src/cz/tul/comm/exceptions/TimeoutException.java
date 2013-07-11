package cz.tul.comm.exceptions;

/**
 *
 * @author Petr Jecmen
 */
public class TimeoutException extends RuntimeException {

    /**
     * Creates a new instance of
     * <code>TimeoutException</code> without detail message.
     */
    public TimeoutException() {
    }

    /**
     * Constructs an instance of
     * <code>TimeoutException</code> with the specified detail message.
     *
     * @param msg the detail message.
     */
    public TimeoutException(String msg) {
        super(msg);
    }
}
