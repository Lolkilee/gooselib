package nl.thomasgoossen.gooselib.shared;

import java.io.Serializable;

public class AppMetaData implements Serializable {
    public final String name;
    public final String latestVersion;
    public final int chunkCount;

    public AppMetaData(String name, String latestVer, int chunkCount) {
        this.name = name;
        this.latestVersion = latestVer;
        this.chunkCount = chunkCount;
    }
}
