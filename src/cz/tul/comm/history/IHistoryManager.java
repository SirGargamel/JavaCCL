package cz.tul.comm.history;

import cz.tul.comm.history.export.IExportUnit;
import cz.tul.comm.history.sorting.HistorySorter;
import java.io.File;
import java.net.InetAddress;

/**
 * Interface for history manager.
 * @author Petr Ječmen
 */
public interface IHistoryManager {

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
     */
    void logMessageReceived(final InetAddress ipSource, final Object data, final boolean accepted);

    /**
     * Log that message has been sent.
     *
     * @param ipDestination target IP
     * @param data transmitted data
     * @param accepted true if data has been sent successfully
     */
    void logMessageSend(final InetAddress ipDestination, final Object data, final boolean accepted);

    /**
     * Register new {@link IExportUnit}.
     *
     * @param eu new export unit
     */
    void registerExporter(final IExportUnit eu);
    
}
