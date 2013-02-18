package cz.tul.comm.gui;

import javax.swing.JOptionPane;

/**
 *
 * @author Petr Ječmen
 */
public class UserLogging {

    public static void showMessageToUser(final String message) {
        JOptionPane.showMessageDialog(null, message, "Message", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showWarningToUser(final String warningMessage) {
        JOptionPane.showMessageDialog(null, warningMessage, "Warning", JOptionPane.WARNING_MESSAGE);
    }

    public static void showErrorToUser(final String errorMessage) {
        JOptionPane.showMessageDialog(null, errorMessage, "Error", JOptionPane.WARNING_MESSAGE);
    }

}
