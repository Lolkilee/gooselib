package nl.thomasgoossen.gooselib.shared.messages;

import java.io.Serializable;

public class ChunkUploadReq implements Serializable {
    public final String appName;
    public final int index;

    public ChunkUploadReq(String appName, int index) {
        this.appName = appName;
        this.index = index;
    }
}
