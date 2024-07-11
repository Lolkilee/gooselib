package nl.thomasgoossen.gooselib.shared.messages;

import java.io.Serializable;

public class ChunkResp implements Serializable {
    public final String appName;
    public final int cIndex;
    public final byte[] data;

    public ChunkResp(String appName, int cIndex, byte[] data) {
        this.appName = appName;
        this.cIndex = cIndex;
        this.data = data;
    }
}
