package cz.tul.comm.tester.virtual;

import cz.tul.comm.job.Assignment;
import static cz.tul.comm.tester.virtual.Action.CANCEL_AFTER_TIME;
import static cz.tul.comm.tester.virtual.Action.CANCEL_BEFORE_TIME;
import java.io.Serializable;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Petr Ječmen
 */
public class Work implements Callable<Object>, Serializable {

    private static final Logger log = Logger.getLogger(Work.class.getName());
    private static final Random r = new Random();

    public static String buildResult(final Action action, final int repCount) {
        return action.toString().concat("-".concat(String.valueOf(repCount)));
    }
    private final Action action;
    private final int repetitionCount;
    private Assignment task;
    private IDummy closer;

    public Work(Action action, int repetitionCount) {
        this.action = action;
        this.repetitionCount = repetitionCount;
    }

    public void setTask(Assignment task) {
        this.task = task;
    }

    public void setCloser(IDummy closer) {
        this.closer = closer;
    }

    @Override
    public Object call() throws Exception {
        int repCount = repetitionCount;
        log.log(Level.CONFIG, "Computing assignment with id {0}", task.getId());
        switch (action) {
            case COMPUTE:
                compute(repetitionCount);
                log.log(Level.CONFIG, "Client computed successfully {0} repetitions.", repetitionCount);
                break;
            case CANCEL_AFTER_TIME:
                compute(repetitionCount);
                task.cancel("Canceling after time.");
                log.log(Level.CONFIG, "Client cancelled after {0} repetitions.", repetitionCount);
                break;
            case CANCEL_BEFORE_TIME:
                repCount = r.nextInt(repetitionCount);
                compute(repCount);
                task.cancel("Canceling before time.");
                log.log(Level.CONFIG, "Client cancelled after {0} repetitions.", repCount);
                break;
            case CRASH_AFTER_TIME:
                compute(repetitionCount);
                closer.closeClient();
                log.log(Level.CONFIG, "Client closed after {0} repetitions.", repetitionCount);
                break;
            case CRASH_BEFORE_TIME:
                repCount = r.nextInt(repetitionCount);
                compute(repCount);
                closer.closeClient();
                log.log(Level.CONFIG, "Client closed after {0} repetitions.", repCount);
                break;
            default:
                log.log(Level.CONFIG, "Illegal action - {0}", action);
                break;
        }

        return buildResult(action, repCount);
    }

    private void compute(final int repetitionCount) {
        synchronized (this) {
            try {
                this.wait(repetitionCount);
            } catch (InterruptedException ex) {
                // nothing
            }
        }
    }
}
