package cz.tul.comm.tester.virtual;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Petr Ječmen
 */
public class Work implements Callable<Object>, Serializable {

    private static final Logger log = Logger.getLogger(Work.class.getName());
    private final int repetitionCount;
    private UUID jobId;

    /**
     *
     * @param repetitionCount
     */
    public Work(int repetitionCount) {
        this.repetitionCount = repetitionCount;
    }

    /**
     *
     * @param jobId
     */
    public void setJobId(final UUID jobId) {
        this.jobId = jobId;
    }

    @Override
    public Object call() throws Exception {
        log.log(Level.CONFIG, "Computing assignment with id {0}", jobId);
        compute(repetitionCount);
        log.log(Level.CONFIG, "Client computed successfully {0} repetitions for job with id {1}.", new Object[]{repetitionCount, jobId});
        return buildResult();
    }    

    private void compute(final int repetitionCount) {
        synchronized (this) {
            try {
                this.wait(repetitionCount);
            } catch (InterruptedException ex) {
                log.warning("Client computation has been interrupted.");
            }
        }
    }
    
    /**
     *
     * @return
     */
    public String buildResult() {
        return "-".concat(String.valueOf(repetitionCount));
    }

    /**
     *
     * @return
     */
    public int getRepetitionCount() {
        return repetitionCount;
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Work - ");
        sb.append(jobId);
        sb.append(" - ");
        sb.append(repetitionCount);
        
        return sb.toString();
    }
}
