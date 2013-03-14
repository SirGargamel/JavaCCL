package cz.tul.comm.gui;

import javax.swing.JOptionPane;

/**
 * Methods for presenting message/warnings/errors to user.
 *
 * @author Petr Ječmen
 */
public abstract class UserLogging {

    /**
     * Show message to user.
     *
     * @param message message for presenting
     */
    public static void showMessageToUser(final String message) {
        JOptionPane.showMessageDialog(null, message, "Message", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Show warning message to user.
     *
     * @param warningMessage message for presenting
     */
    public static void showWarningToUser(final String warningMessage) {
        JOptionPane.showMessageDialog(null, warningMessage, "Warning", JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Show error message to user.
     *
     * @param errorMessage message for presenting
     */
    public static void showErrorToUser(final String errorMessage) {
        JOptionPane.showMessageDialog(null, errorMessage, "Error", JOptionPane.WARNING_MESSAGE);
    }
}
