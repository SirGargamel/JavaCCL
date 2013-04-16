package cz.tul.comm.tester.virtual;

import cz.tul.comm.server.Comm_Server;
import cz.tul.comm.server.Server;

/**
 *
 * @author Petr Ječmen
 */
public class DummyServer {

    private final Server s;
    
    public DummyServer() {
        s = Comm_Server.initNewServer();
    }
    
    public void submitJob(final Action action, final int repetitionCount) {
        final Work w = new Work(action, repetitionCount);
        s.submitJob(w);
    }
    
}
