package nl.thomasgoossen.gooselib.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import com.sun.net.httpserver.HttpServer;

import nl.thomasgoossen.gooselib.client.handlers.TestHandler;

public class HTTPInterface {
    public boolean stopFlag = false;

    private final int TP_THREADS = 2;
    private final HttpServer server;
    private final ThreadPoolExecutor tpExecutor;

    private static HTTPInterface inst;

    public HTTPInterface(int port) throws IOException {
        inst = this;
        tpExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(TP_THREADS);

        server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
        server.createContext("/test", new TestHandler());
        server.setExecutor(tpExecutor);
        server.start();
    }

    public static void stop() {
        inst.stopFlag = true;
    }
}
