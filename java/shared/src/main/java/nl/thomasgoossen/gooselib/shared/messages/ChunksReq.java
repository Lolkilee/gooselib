package nl.thomasgoossen.gooselib.shared.messages;

import java.io.Serializable;

public class ChunksReq implements Serializable {
    public final String appName;
    public final int beginIndex;
    public final int length;

    public ChunksReq(String appName, int beginIndex, int length) {
        this.appName = appName;
        this.beginIndex = beginIndex;
        this.length = length;
    }
}
