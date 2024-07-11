package nl.thomasgoossen.gooselib.client;

import java.io.Serializable;

public class DownloadInfo implements Serializable {
    public final String appName;
    public final float progress;

    public DownloadInfo(String appName, float progress) {
        this.appName = appName;
        this.progress = progress;
    }
}
