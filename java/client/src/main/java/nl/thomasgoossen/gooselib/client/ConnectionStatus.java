package nl.thomasgoossen.gooselib.client;

import java.io.Serializable;

public class ConnectionStatus implements Serializable {
    public final boolean connected;

    public ConnectionStatus(boolean connected) {
        this.connected = connected;
    }
}
