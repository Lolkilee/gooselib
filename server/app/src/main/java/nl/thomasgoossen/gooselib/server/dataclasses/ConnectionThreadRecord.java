package nl.thomasgoossen.gooselib.server.dataclasses;

/**
 * Class containing information about a single connection thread
 */
public class ConnectionThreadRecord {
    private final Thread thread;

    public int connections = 0;

    public ConnectionThreadRecord(Thread t) {
        this.thread = t;
    }

    public Thread getThread() {
        return thread;
    }
}
