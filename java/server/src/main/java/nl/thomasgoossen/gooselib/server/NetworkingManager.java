package nl.thomasgoossen.gooselib.server;

import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;

import com.esotericsoftware.kryonet.Server;

import nl.thomasgoossen.gooselib.server.dataclasses.ConnectionThreadRecord;
import nl.thomasgoossen.gooselib.shared.Constants;
import nl.thomasgoossen.gooselib.shared.EncryptionHelper;
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

    private static volatile boolean stopFlag = false;

    private static ConnectionThreadRecord[] records;
    private Server manager;
    private ScheduledExecutorService scheduler;

    /**
     * Manager class that handles all threads required for networking
     * @param multiThreaded whether or not to create multiple additional connection threads
     * @param encKey SecretKey handling encryption for all connection threads
     * @throws IOException
     */
    public NetworkingManager(boolean multiThreaded) throws IOException {
        int tCount = multiThreaded ? Runtime.getRuntime().availableProcessors() - 2 : 1;
        if (tCount < 1)
            tCount = 1;
        records = new ConnectionThreadRecord[tCount];

        Logger.log("starting " + tCount + " connection threads");
        for (int i = 0; i < tCount; i++) {
            Server s = new Server(Constants.BUF_PER_CHUNK * Constants.DEF_CHUNK_WINDOW, Constants.BUF_PER_CHUNK);
            int tcpPort = BEGIN_PORT + i * 2 + 1;
            int udpPort = BEGIN_PORT + i * 2 + 2;
            s.bind(tcpPort, udpPort);
            SecretKey sessionKey = EncryptionHelper.generateKey();
            s.addListener(new NetworkingListener(tcpPort, udpPort, sessionKey));
            KryoHelper.addRegisters(s.getKryo());
            records[i] = new ConnectionThreadRecord(s, sessionKey, tcpPort, udpPort);

            Logger.log("started connection thread with TCP port " + tcpPort + ", UDP port " + udpPort);
        }
    }

    /**
     * Runs the manager thread until close message received
     */
    public void run() throws IOException, InterruptedException {
        for (ConnectionThreadRecord r : records) {
            r.getServer().start();
        }

        manager = new Server();
        manager.bind(BEGIN_PORT);
        manager.addListener(new NetworkingListener());
        KryoHelper.addRegisters(manager.getKryo());

        Logger.log("starting manager connection loop");
        manager.start();

        Logger.dbg("current thread count: " + Thread.activeCount());
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::checkStopFlag, 0, 100, TimeUnit.MILLISECONDS);
    }
    
    private void checkStopFlag() {
        if (stopFlag) {
            scheduler.shutdown();
            GLServer.exit();
        }
    }

    /**
     * Closes all connection threads by issuing interrupts
     */
    public void close() {
        Logger.log("stopping manager threads");
        for (ConnectionThreadRecord r : records) {
            r.getServer().close();
        }

        manager.close();
        stopFlag = false;
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

    /**
     * @return returns the next connection thread info to be used 
     * in an array, [index, tcp, udp]
     */
    public static int[] getNextConnection() {
        int lowestCount = Integer.MAX_VALUE;
        int i = 0;
        int index = -1;
        for (ConnectionThreadRecord r : records) {
            if (r.getConnections() < lowestCount) {
                lowestCount = r.getConnections();
                index = i;
            }
            i++;
        }

        return new int[] { index, records[index].tcp, records[index].udp };
    }

    public static SecretKey getEncryptionKey(int index) {
        return records[index].getKey();
    }

    /**
     * Debug function
     * @return an array containing the string version of all session keys in the connection threads
     */
    public static String[] getSessionKeyStrings() {
        String[] arr = new String[records.length];
        for (int i = 0; i < records.length; i++) {
            arr[i] = Base64.getEncoder().encodeToString(records[i].getKey().getEncoded());
        }

        return arr;
    }

    /**
     * Debug function to print all connection counts across threads
     */
    public static void printConnectionCounts() {
        for (ConnectionThreadRecord r : records) {
            Logger.dbg("conn thread with " + r.getConnections() + " connections");
        }
    }
}
