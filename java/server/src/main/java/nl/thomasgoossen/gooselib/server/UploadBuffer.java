package nl.thomasgoossen.gooselib.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class UploadBuffer {
    private final String name;

    public final int totalCount;

    private int lastAppended = -1;
    private final HashMap<Integer, byte[]> buffer = new HashMap<>();
    private final ArrayList<Integer> toRecv;

    public UploadBuffer(String name, int totalCount) {
        this.name = name;
        this.totalCount = totalCount;
        this.toRecv = new ArrayList<>();

        for (int i = 0; i < totalCount; i++) {
            toRecv.add(i);
        }

        Logger.dbg("created new upload buffer with " + toRecv.size() + " expected chunks");
    }

    public void addToBuffer(int index, byte[] chunk) {
        if (toRecv.contains(index)) {
            if (index == lastAppended + 1) {
                Database.appendChunk(name, chunk);
                lastAppended = index;
            } else {
                Logger.dbg("added chunk to queue");
                buffer.put(index, chunk);
            }
            toRecv.remove(toRecv.indexOf(index));
        }
    }

    public void pushBuffer() {
        if (!buffer.keySet().isEmpty()) {
            ArrayList<Integer> toRemove = new ArrayList<>();

            // Make sure that the first key checked is the lowest value
            ArrayList<Integer> keys = new ArrayList<>(buffer.keySet());
            Collections.sort(keys);

            for (int i : keys) {
                if (i == lastAppended + 1) {
                    Database.appendChunk(name, buffer.get(i));
                    toRemove.add(i);
                    lastAppended = i;
                } else
                    break;
            }

            Logger.dbg("pushed " + toRemove.size() + " queued chunks");
            for (int k : toRemove) {
                buffer.remove(k);
            }
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
