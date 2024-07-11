package nl.thomasgoossen.gooselib.shared.messages;

import java.io.Serializable;

public class ChunkReq implements Serializable {
    public final String appName;
    public final int index;

    public ChunkReq(String appName, int index) {
        this.appName = appName;
        this.index = index;
    }
}
