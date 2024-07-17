package nl.thomasgoossen.gooselib.client;

import java.io.Serializable;

public class DownloadInfo implements Serializable {
    public final String appName;
    public final float writeProgress;
    public final float netwProgress;
    public final long speed; // in bytes / second

    public DownloadInfo(String appName, float progress, float netwProgress, long speed) {
        this.appName = appName;
        this.writeProgress = progress;
        this.netwProgress = netwProgress;
        this.speed = speed;
    }
}
