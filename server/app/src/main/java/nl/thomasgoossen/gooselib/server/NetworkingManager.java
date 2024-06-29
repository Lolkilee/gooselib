package nl.thomasgoossen.gooselib.server;

import java.io.IOException;

import javax.crypto.SecretKey;

import com.esotericsoftware.kryonet.Server;

import nl.thomasgoossen.gooselib.server.dataclasses.ConnectionThreadRecord;
import nl.thomasgoossen.gooselib.shared.KryoHelper;

public class NetworkingManager {

    /**
     * From this port all other ports in use are calculated
     * Example list:
     * 61234 TCP | Manager
     * 61235 TCP | Connection thread 1, TCP
     * 61236 UDP | Connection thread 2, UDP
     * etc. (repeats n time based on cores available).
     */
    public static final int BEGIN_PORT = 61234;

    private static boolean stopFlag = false;

    private final ConnectionThreadRecord[] records;
    private Server manager;

    /**
     * Manager class that handles all threads required for networking
     * @param multiThreaded whether or not to create multiple additional connection threads
     * @param encKey SecretKey handling encryption for all connection threads
     * @throws IOException
     */
    public NetworkingManager(boolean multiThreaded, SecretKey encKey) throws IOException {
        int tCount = multiThreaded ? Runtime.getRuntime().availableProcessors() - 1 : 1;
        records = new ConnectionThreadRecord[tCount];

        Logger.log("starting " + tCount + " connection threads");
        for (int i = 0; i < tCount; i++) {
            Server s = new Server();
            int tcpPort = BEGIN_PORT + i * 2 + 1;
            int udpPort = BEGIN_PORT + i * 2 + 2;
            s.bind(tcpPort, udpPort);
            s.addListener(new NetworkingListener(encKey));
            KryoHelper.addRegisters(s.getKryo());
            records[i] = new ConnectionThreadRecord(new Thread(s));

            Logger.log("started connection thread with TCP port " + tcpPort + ", UDP port " + udpPort);
        }
    }

    /**
     * Runs the manager thread until close message received
     */
    public void run() throws IOException, InterruptedException {
        for (ConnectionThreadRecord r : records) {
            r.getThread().start();
        }

        manager = new Server();
        manager.bind(BEGIN_PORT);
        manager.addListener(new NetworkingListener());
        KryoHelper.addRegisters(manager.getKryo());
        
        Logger.log("starting manager connection loop");
        manager.start();

        Logger.dbg("current thread count: " + Thread.activeCount());
        while (!stopFlag) {
            // run this function until stopflag is set
        }
    }

    /**
     * Closes all connection threads by issuing interrupts
     */
    public void close() {
        Logger.log("stopping manager threads");
        for (ConnectionThreadRecord r : records) {
            r.getThread().interrupt();
        }
    }

    /**
     * Sets the stopflag, which stops the loop in run()
     */
    public static void stop() {
        stopFlag = true;
    }

    public static boolean isRunning() {
        return !stopFlag;
    }
}
