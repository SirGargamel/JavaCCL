package cz.tul.javaccl;

import java.util.Observable;

/**
 *
 * @author Petr Jecmen
 */
public class CCLObservable extends Observable {

    /**
     * Server / Client registration
     */
    public static final String REGISTER = "Register";
    /**
     * Server / Client deregistration
     */
    public static final String DEREGISTER = "Deregister";

    /**
     * Notify listeners about some change. Defines universal format of arg -
     * Object[] {String, Object[]}, where String describes action and object
     * array contains info about action (IP, UUID etc.).
     *
     * @param action description of action that happened
     * @param arg action parameters
     */
    protected void notifyChange(final String action, final Object[] arg) {
        setChanged();
        notifyObservers(new Object[]{action, arg});
    }
}
