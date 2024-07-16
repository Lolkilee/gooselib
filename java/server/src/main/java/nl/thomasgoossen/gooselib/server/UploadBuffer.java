package nl.thomasgoossen.gooselib.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class UploadBuffer {
    private final String name;

    public final int totalCount;

    private int lastAppended = -1;
    private final List<byte[]> buffer;
    private final List<Integer> indices;
    private final ArrayList<Integer> toRecv;
    private final ScheduledExecutorService scheduler;

    public UploadBuffer(String name, int totalCount) {
        this.name = name;
        this.totalCount = totalCount;
        this.toRecv = new ArrayList<>();
        this.buffer = Collections.synchronizedList(new ArrayList<>());
        this.indices = Collections.synchronizedList(new ArrayList<>());
        this.scheduler = Executors.newScheduledThreadPool(1);

        for (int i = 0; i < totalCount; i++) {
            toRecv.add(i);
        }

        scheduler.scheduleAtFixedRate(() -> {
            pushBuffer();
        }, 1000, 1000, TimeUnit.MILLISECONDS);

        Logger.dbg("created new upload buffer with " + toRecv.size() + " expected chunks");
    }

    public void addToBuffer(int index, byte[] chunk) {
        if (toRecv.contains(index)) {
            buffer.add(chunk);
            indices.add(index);
            toRecv.remove(toRecv.indexOf(index));
        }
    }

    public void pushBuffer() {
        if (!buffer.isEmpty()) {
            for (int bufIndex = 0; bufIndex < buffer.size(); bufIndex++) {
                int i = indices.get(bufIndex);
                if (i == lastAppended + 1) {
                    Database.appendChunk(name, buffer.get(bufIndex));
                    lastAppended = i;
                } else
                    break;
            }

            Logger.dbg("lA: " + lastAppended + ", first: " + indices.getFirst());
            for (int i = 0; i < buffer.size(); i++) {
                if (indices.get(i) <= lastAppended) {
                    buffer.remove(i);
                    indices.remove(i);
                }
            }
        }

        if (buffer.isEmpty() && toRecv.isEmpty()) {
            Logger.log("writes finished, scheduler shutting down...");
            scheduler.shutdown();
            Database.setAppPublic(name, true);
        }
    }

    public int getLastAppended() {
        return lastAppended;
    }

    public boolean isDone() {
        return toRecv.isEmpty();
    }

    public int next(ArrayList<Integer> alreadyExpected) {
        if (toRecv.isEmpty())
            return -1;

        int next = toRecv.getFirst();
        int i = 0;
        while (alreadyExpected.contains(next)) {
            i++;
            next = toRecv.get(i);
        }

        return next;
    }

    public ArrayList<Integer> getToRecv() {
        return toRecv;
    }
}
