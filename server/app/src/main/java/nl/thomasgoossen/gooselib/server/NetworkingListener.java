package nl.thomasgoossen.gooselib.server;

import javax.crypto.SecretKey;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

public class NetworkingListener extends Listener {
    private final boolean manager;
    private final SecretKey encKey;

    public NetworkingListener() {
        this.manager = true;
        this.encKey = null;
    }

    public NetworkingListener(SecretKey encKey) {
        this.manager = true;
        this.encKey = encKey;
    }

    @Override
    public void received(Connection connection, Object object) {
        if (manager) { // Manager connection
            Logger.dbg("recv manager req with type: " + object.getClass().getSimpleName());
            onManagerRequest(connection, object);
        } else { // Normal connection
            Logger.dbg("recv req with type: " + object.getClass().getSimpleName());
            onRequest(connection, object);
        }
    }

    private void onRequest(Connection con, Object obj) {

    }

    private void onManagerRequest(Connection con, Object obj) {

    }
}
