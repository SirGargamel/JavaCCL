package cz.tul.comm.messaging.job;

/**
 * Interface for classes hnadling computation of assignments.
 *
 * @author Petr Ječmen
 */
public interface AssignmentListener {

    /**
     * Assign new job.
     *
     * @param task Task for computation
     */
    void receiveTask(final Assignment task);

    /**
     * Computation of this task has been cancelled. (eg. computation has failed,
     * server is shutting down etc.)
     *
     * @param task task, which should be cancelled
     */
    void cancelTask(final Assignment task);
}
