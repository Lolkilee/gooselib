package nl.thomasgoossen.gooselib.server;

import javax.crypto.SecretKey;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import nl.thomasgoossen.gooselib.util.EncryptedPacket;
import nl.thomasgoossen.gooselib.util.ShutdownReq;

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
        if (object instanceof EncryptedPacket encryptedPacket) {
            if (manager) { // Manager connection
                Logger.dbg("recv manager req with type: " + object.getClass().getSimpleName());
                onManagerRequest(connection, encryptedPacket);
            } else { // Normal connection
                Logger.dbg("recv req with type: " + object.getClass().getSimpleName());
                onRequest(connection, encryptedPacket);
            }
        } else {
            Logger.warn("received an object that was not an EncryptedPacket, ignoring");
        }
    }

    private void onRequest(Connection con, EncryptedPacket pkt) {

    }

    private void onManagerRequest(Connection con, EncryptedPacket pkt) {
        Object data = pkt.getData(encKey);
        if (data instanceof ShutdownReq shutdown) {
            if (Database.auth("admin", shutdown.getAdminPass()))
                NetworkingManager.stop();
        } else {
            Logger.warn("deserialized data was not recognized my onManagerRequest()");
        }
    }
}
