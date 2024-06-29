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
                onManagerRequest(encryptedPacket);
            } else { // Normal connection
                Logger.dbg("recv req with type: " + object.getClass().getSimpleName());
                onRequest(encryptedPacket);
            }
        } else {
            Logger.warn("received an object that was not an EncryptedPacket, ignoring");
        }
    }

    private void onRequest(EncryptedPacket pkt) {
        Object data = pkt.getDataObject(encKey);
        Logger.dbg("data object in req listener with type: " + data.getClass().getSimpleName());
    }

    private void onManagerRequest(EncryptedPacket pkt) {
        // Packets to manager are not encrypted
        Object data = pkt.getDataObject(null);
        Logger.dbg("data object in manager listener with type: " + data.getClass().getSimpleName());

        if (data instanceof ShutdownReq shutdown) {
            if (Database.auth("admin", shutdown.getAdminPass())) {
                Logger.log("recv shutdown request with correct admin password, shutting down");
                NetworkingManager.stop();
            }
        } else {
            Logger.warn("deserialized data was not recognized my onManagerRequest()");
        }
    }
}
