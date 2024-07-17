package nl.thomasgoossen.gooselib.shared.messages;

import java.io.Serializable;
import java.util.ArrayList;

import nl.thomasgoossen.gooselib.shared.AppMetaData;

public class LibInfoResp implements Serializable {
    public final AppMetaData[] apps;

    public LibInfoResp(ArrayList<AppMetaData> arr) {
        apps = new AppMetaData[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            apps[i] = arr.get(i);
        }
    }
}
