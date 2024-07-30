package nl.thomasgoossen.gooselib.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.esotericsoftware.kryonet.Connection;

import nl.thomasgoossen.gooselib.shared.EncryptedPacket;
import nl.thomasgoossen.gooselib.shared.messages.UploadCompleteMsg;

public class UploadBuffer {
    public final int totalCount;

    private final String name;
    private int lastAppended = -1;
    private long bytesAppended = 0;
    private final ConcurrentHashMap<Integer, byte[]> recvBuffer;
    private final List<Integer> toRecv;
    private final Connection conn;

    public final ScheduledExecutorService scheduler;

    private static final List<String> uploadingApps = Collections.synchronizedList(new ArrayList<>());

    public UploadBuffer(String name, int totalCount, Connection conn) {
        this.name = name;
        this.totalCount = totalCount;
        this.toRecv = Collections.synchronizedList(new ArrayList<>());
        this.recvBuffer = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.conn = conn;

        for (int i = 0; i < totalCount; i++) {
            toRecv.add(i);
        }

        uploadingApps.add(name);

        scheduler.scheduleAtFixedRate(() -> {
            try {
                pushBuffer();
            } catch (IOException e) {
                Logger.err(e.getMessage());
            }
        }, 1000, 100, TimeUnit.MILLISECONDS);

        Logger.log("created new upload buffer with " + toRecv.size() + " expected chunks");
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
                Database.appendChunk(name, c);
                bytesAppended += c.length;
                keysToRemove.add(lastAppended + 1);
                lastAppended++;
            }

            // if (buffer.containsKey(lastAppended + 2) && lastAppended + 2 < totalCount) {
            //     ChunkUploadReq req = new ChunkUploadReq(name, lastAppended + 2, 1);
            //     conn.sendUDP(new EncryptedPacket(req));
            // }
        }

        buffer.clear();
        Logger.dbg("pushed " + keysToRemove.size() + " chunks to disk");
        for (int i : keysToRemove) {
            recvBuffer.remove(i);
        }

        if (recvBuffer.isEmpty() && toRecv.isEmpty()) {
            Logger.log("writes finished, scheduler shutting down...");
            if (uploadingApps.contains(name)) {
                uploadingApps.remove(uploadingApps.indexOf(name));
            } else {
                Logger.warn("could not remove '" + name + "' from uploading list");
            }

            scheduler.shutdown();
            Logger.log("sending completion message");
            UploadCompleteMsg msg = new UploadCompleteMsg(totalCount);
            EncryptedPacket p = new EncryptedPacket(msg);
            conn.sendTCP(p);
        }
    }

    public int getLastAppended() {
        return lastAppended;
    }

    public long getBytesAppended() {
        return bytesAppended;
    }

    public boolean isDone() {
        return (toRecv.isEmpty() && recvBuffer.isEmpty());
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

    public static boolean isUploading(String name) {
        return uploadingApps.contains(name);
    }
}
