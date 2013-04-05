package cz.tul.comm.messaging.job;

/**
 *
 * @author Petr Ječmen
 */
public interface IAssignmentListener {

    void receiveTask(final Assignment task);

    void cancelTask(final Assignment taskId);
}
