package cz.tul.comm.tester.virtual;

import cz.tul.comm.messaging.job.Assignment;
import static cz.tul.comm.tester.virtual.Action.CANCEL_AFTER_TIME;
import static cz.tul.comm.tester.virtual.Action.CANCEL_BEFORE_TIME;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
        boolean res;
        log.log(Level.CONFIG, "Computing assignment with id {0}", task.getId());
        switch (action) {
            case COMPUTE:
                res = compute(repetitionCount);
                log.log(Level.CONFIG, "Client computed successfully {0} repetitions.", repetitionCount);
                break;
            case CANCEL_AFTER_TIME:
                res = compute(repetitionCount);
                task.cancel("Canceling after time.");
                log.log(Level.CONFIG, "Client cancelled after {0} repetitions.", repetitionCount);
                break;
            case CANCEL_BEFORE_TIME:
                repCount = r.nextInt(repetitionCount);
                res = compute(repCount);
                task.cancel("Canceling before time.");
                log.log(Level.CONFIG, "Client cancelled after {0} repetitions.", repCount);
                break;
            case CRASH_AFTER_TIME:
                res = compute(repetitionCount);
                closer.closeClient();
                log.log(Level.CONFIG, "Client closed after {0} repetitions.", repetitionCount);
                break;
            case CRASH_BEFORE_TIME:
                repCount = r.nextInt(repetitionCount);
                res = compute(repCount);
                closer.closeClient();
                log.log(Level.CONFIG, "Client closed after {0} repetitions.", repCount);
                break;
            default:
                res = true;
                break;
        }
        
        return action.toString().concat("-".concat(String.valueOf(repCount))).concat("-").concat(String.valueOf(res));
    }

    private static boolean compute(final int repetitionCount) {        
        final List<Double> listA = new ArrayList<>(repetitionCount);
        final List<Double> listB = new ArrayList<>(repetitionCount);

        for (int i = 0; i < repetitionCount; i++) {
            listA.add(r.nextGaussian());
            listB.add(r.nextGaussian());
        }

        Collections.sort(listA);
        Collections.sort(listB);
        
        Collections.rotate(listA, r.nextInt(repetitionCount));
        Collections.rotate(listB, r.nextInt(repetitionCount));

        return Collections.disjoint(listA, listB);
    }
}
