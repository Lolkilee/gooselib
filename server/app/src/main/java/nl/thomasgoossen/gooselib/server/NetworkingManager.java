package nl.thomasgoossen.gooselib.server;

import java.io.IOException;

import javax.crypto.SecretKey;

import com.esotericsoftware.kryonet.Server;

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

    // Additional threads
    private final Thread[] threads;
    private Server manager;

    public NetworkingManager(boolean multiThreaded, SecretKey encKey) throws IOException {
        int tCount = multiThreaded ? Runtime.getRuntime().availableProcessors() : 1;
        threads = new Thread[tCount];

        for (int i = 0; i < tCount; i++) {
            Server s = new Server();
            int tcpPort = BEGIN_PORT + i * 2 + 1;
            int udpPort = BEGIN_PORT + i * 2 + 2;
            s.bind(tcpPort, udpPort);
            s.addListener(new NetworkingListener(encKey));
            threads[i] = new Thread(s);

            Logger.log("started connection thread with TCP port " + tcpPort + ", UDP port " + udpPort);
        }
    }

    /**
     * Runs the manager thread until close message received
     */
    public void run() throws IOException, InterruptedException {
        for (Thread t : threads) {
            t.start();
        }

        manager = new Server();
        manager.bind(BEGIN_PORT);
        manager.addListener(new NetworkingListener());
        Thread t = new Thread(manager);

        Logger.log("starting manager connection loop");
        t.join();
    }

    public void close() {
        for (Thread t : threads) {
            t.interrupt();
        }
    }
}
