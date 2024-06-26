package nl.thomasgoossen.gooselib.server;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

public class NetworkingListener extends Listener {
    private final boolean manager;

    public NetworkingListener(boolean manager) {
        this.manager = manager;
        Logger.dbg("started networking listener");
    }

    public void received(Connection connection, Object object) {
        if (manager) { // Manager connection

        } else { // Normal connection

        }
    }
}
