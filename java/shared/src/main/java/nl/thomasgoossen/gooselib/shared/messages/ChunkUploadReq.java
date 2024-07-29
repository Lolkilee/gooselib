package nl.thomasgoossen.gooselib.shared.messages;

import java.io.Serializable;

// Sent from server to client
public class ChunkUploadReq implements Serializable {
    public final String appName;

    public ChunkUploadReq(String appName) {
        this.appName = appName;
    }
}
