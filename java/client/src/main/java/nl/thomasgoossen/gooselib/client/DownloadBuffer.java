package nl.thomasgoossen.gooselib.client;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import nl.thomasgoossen.gooselib.shared.messages.ChunkReq;

public class DownloadBuffer {
    public final int totalCount;

    private final String name;
    private int lastAppended = -1;
    private long bytesAppended = 0;
    private final ConcurrentHashMap<Integer, byte[]> recvBuffer;
    private final List<Integer> toRecv;
    private final FileOutputStream fos;

    public final ScheduledExecutorService scheduler;

    public DownloadBuffer(String name, int totalCount, FileOutputStream fos) {
        this.name = name;
        this.totalCount = totalCount;
        this.toRecv = Collections.synchronizedList(new ArrayList<>());
        this.recvBuffer = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.fos = fos;

        for (int i = 0; i < totalCount; i++) {
            toRecv.add(i);
        }

        scheduler.scheduleAtFixedRate(() -> {
            try {
                pushBuffer();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }, 100, 1000, TimeUnit.MILLISECONDS);

        System.out.println("created new download buffer with " + toRecv.size() + " expected chunks");
    }

    public void addToBuffer(int index, byte[] chunk) {
        if (toRecv.contains(index)) {
            recvBuffer.put(index, chunk);
            toRecv.remove(toRecv.indexOf(index));
        }
    }

    public void pushBuffer() throws IOException {
        // Copy hash map
        HashMap<Integer, byte[]> buffer = new HashMap<>();
        for (Integer key : recvBuffer.keySet()) {
            buffer.put(key, recvBuffer.get(key));
        }

        ArrayList<Integer> keysToRemove = new ArrayList<>();

        if (!buffer.isEmpty()) {
            while (buffer.containsKey(lastAppended + 1)) {
                byte[] c = buffer.get(lastAppended + 1);
                fos.write(c);
                bytesAppended += c.length;
                keysToRemove.add(lastAppended + 1);
                lastAppended++;
            }

            // There is a gap in the buffer
            if (buffer.containsKey(lastAppended + 2) && lastAppended + 2 < totalCount) {
                ChunkReq req = new ChunkReq(name, lastAppended + 2);
                GLClient.sendPlainPacketUDP(req);
            }
        }

        buffer.clear();
        for (int i : keysToRemove) {
            recvBuffer.remove(i);
        }

        if (recvBuffer.isEmpty() && toRecv.isEmpty()) {
            fos.close();
            System.out.println("writes finished, scheduler shutting down...");
            scheduler.shutdown();
            Download.finish(name);
        }
    }

    public int getLastAppended() {
        return lastAppended;
    }

    public long getBytesAppended() {
        return bytesAppended;
    }

    public boolean isDone() {
        return toRecv.isEmpty() && recvBuffer.isEmpty();
    }

    public int next(List<Integer> alreadyExpected) {
        if (toRecv.isEmpty())
            return -1;

        int next = toRecv.get(0);
        int i = 0;
        while (alreadyExpected.contains(next)) {
            i++;
            next = toRecv.get(i);
        }

        return next;
    }

    public List<Integer> getToRecv() {
        return toRecv;
    }
}
