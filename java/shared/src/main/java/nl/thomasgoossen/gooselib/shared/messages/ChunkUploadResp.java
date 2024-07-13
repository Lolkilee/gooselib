package nl.thomasgoossen.gooselib.shared.messages;

import java.io.Serializable;

// Sent from client to server
public class ChunkUploadResp implements Serializable {
    public final String appName;
    public final int index;
    public final byte[] chunk;

    public ChunkUploadResp(String appName, int index, byte[] chunk) {
        this.appName = appName;
        this.index = index;
        this.chunk = chunk;
    }
}
