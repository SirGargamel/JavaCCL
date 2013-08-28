package cz.tul.comm.history;

import cz.tul.comm.history.export.ExportUnit;
import cz.tul.comm.history.sorting.HistorySorter;
import java.io.File;
import java.net.InetAddress;

/**
 * Interface for history manager.
 *
 * @author Petr Ječmen
 */
public interface HistoryManager {

    /**
     * Export sorted history to XML file.
     *
     * @param target target file
     * @param sorter sorter, which will sort data according to some parameter.
     * @return true for successfull export
     */
    boolean export(final File target, final HistorySorter sorter);

    /**
     * Log that message has been received.
     *
     * @param ipSource source IP
     * @param data transmitted data
     * @param accepted true if data has been received successfully
     * @param answer response to the message 
     */
    void logMessageReceived(final InetAddress ipSource, final Object data, final boolean accepted, final Object answer);

    /**
     * Log that message has been sent.
     *
     * @param ipDestination target IP
     * @param data transmitted data
     * @param accepted true if data has been sent successfully
     * @param response received answer
     */
    void logMessageSend(final InetAddress ipDestination, final Object data, final boolean accepted, final Object response);

    /**
     * Register new {@link ExportUnit}.
     *
     * @param eu new export unit
     */
    void registerExporter(final ExportUnit eu);
}
