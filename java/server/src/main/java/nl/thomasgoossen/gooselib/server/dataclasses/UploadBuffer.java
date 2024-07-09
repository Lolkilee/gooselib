package nl.thomasgoossen.gooselib.server.dataclasses;

import java.util.ArrayList;
import java.util.HashMap;

import nl.thomasgoossen.gooselib.server.Database;

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
    }

    public void addToBuffer(int index, byte[] chunk) {
        if (toRecv.contains(index)) {
            if (index == lastAppended + 1) {
                Database.appendChunk(name, chunk);
                lastAppended = index;
            } else {
                buffer.put(index, chunk);
            }
            toRecv.remove(index);
        }
    }

    public void pushBuffer() {
        ArrayList<Integer> toRemove = new ArrayList<>();
        for (int i : buffer.keySet()) {
            if (i == lastAppended + 1) {
                Database.appendChunk(name, buffer.get(i));
                toRemove.add(i);
                lastAppended = i;
            } else {
                break;
            }
        }

        for (int k : toRemove) {
            buffer.remove(k);
        }
    }

    public int getLastAppended() {
        return lastAppended;
    }

    public boolean isDone() {
        return toRecv.isEmpty();
    }

    public int next(int window) {
        if (lastAppended + window < toRecv.size()) {
            return lastAppended + window;
        } else if (!toRecv.isEmpty()) {
            return toRecv.getFirst();
        } else {
            return -1;
        }
    }
}
