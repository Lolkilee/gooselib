package nl.thomasgoossen.gooselib.shared;

import java.io.Serializable;

public class AppMetaData implements Serializable {
    public final String name;
    public final String latestVersion;
    public final String execPath;
    public final int chunkCount;
    public final long bytesCount;

    public AppMetaData(String name, String latestVer, String execPath, int chunkCount, long bytesCount) {
        this.name = name;
        this.latestVersion = latestVer;
        this.execPath = execPath;
        this.chunkCount = chunkCount;
        this.bytesCount = bytesCount;
    }
}
