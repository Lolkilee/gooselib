package nl.thomasgoossen.gooselib.client;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import nl.thomasgoossen.gooselib.shared.messages.ChunkReq;

public class TimeoutThread implements Runnable {
    private final int TIMEOUT_MS = 5000;

    private volatile long lastUpdate;
    private volatile boolean stopFlag = false;

    private final String appName;
    private final ScheduledExecutorService scheduler;

    public TimeoutThread(String appName) {
        this.appName = appName;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void run() {
        lastUpdate = System.currentTimeMillis();
        scheduler.scheduleAtFixedRate(this::checkTimeout, 0, 100, TimeUnit.MILLISECONDS);
    }

    private void checkTimeout() {
        if (stopFlag) {
            scheduler.shutdown();
            return;
        }

        if (System.currentTimeMillis() - lastUpdate > TIMEOUT_MS) {
            int next = Download.nextChunk(appName);
            if (next >= 0) {
                ChunkReq req = new ChunkReq(appName, next);
                GLClient.sendPacketUDP(req);
            } else {
                System.out.println("this should never work, yet it does! (TimeoutThread)");
            }
        }
    }
    
    public void updateRecv() {
        lastUpdate = System.currentTimeMillis();
    }

    public void stop() {
        stopFlag = true;
    }
}