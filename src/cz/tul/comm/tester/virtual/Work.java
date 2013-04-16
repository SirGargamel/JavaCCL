package cz.tul.comm.tester.virtual;

import com.sun.jmx.snmp.tasks.Task;
import static cz.tul.comm.tester.virtual.Action.CANCEL_AFTER_TIME;
import static cz.tul.comm.tester.virtual.Action.CANCEL_BEFORE_TIME;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Petr Ječmen
 */
public class Work implements Callable<Object> {

    private static final Logger log = Logger.getLogger(Work.class.getName());
    private static final Random r = new Random();
    private final Action action;
    private final int repetitionCount;
    private Task task;
    private IClose closer;

    public Work(Action action, int repetitionCount) {
        this.action = action;
        this.repetitionCount = repetitionCount;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public void setCloser(IClose closer) {
        this.closer = closer;
    }

    @Override
    public Object call() throws Exception {
        int repCount;
        switch (action) {
            case COMPUTE:
                compute(repetitionCount);
                log.log(Level.CONFIG, "Client computed successfully {0} repetitions.", repetitionCount);
            case CANCEL_AFTER_TIME:
                compute(repetitionCount);
                task.cancel();
                log.log(Level.CONFIG, "Client cancelled after {0} repetitions.", repetitionCount);
            case CANCEL_BEFORE_TIME:
                repCount = r.nextInt(repetitionCount);
                compute(repCount);
                task.cancel();
                log.log(Level.CONFIG, "Client cancelled after {0} repetitions.", repCount);
            case CRASH_AFTER_TIME:
                compute(repetitionCount);
                closer.closeClient();
                log.log(Level.CONFIG, "Client closed after {0} repetitions.", repetitionCount);
            case CRASH_BEFORE_TIME:
                repCount = r.nextInt(repetitionCount);
                compute(repCount);
                closer.closeClient();
                log.log(Level.CONFIG, "Client closed after {0} repetitions.", repCount);
        }

        return repetitionCount;
    }

    private static void compute(final int repetitionCount) {
        double n, max = -Double.MAX_VALUE;
        for (int i = 0; i < repetitionCount; i++) {
            n = Math.sqrt(Math.pow(i, r.nextInt(repetitionCount)));
            max = (n > max) ? n : max;
        }
    }
}
