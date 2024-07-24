package nl.thomasgoossen.gooselib.shared.messages;

import java.io.Serializable;

// Sent from server to client
public class ChunkUploadReq implements Serializable {
    public final String appName;
    public final int index;
    public final int length;

    public ChunkUploadReq(String appName, int index, int length) {
        this.appName = appName;
        this.index = index;
        this.length = length;
    }
}
