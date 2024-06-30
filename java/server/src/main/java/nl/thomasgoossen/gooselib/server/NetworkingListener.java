package nl.thomasgoossen.gooselib.server;

import javax.crypto.SecretKey;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import nl.thomasgoossen.gooselib.shared.EncryptedPacket;
import nl.thomasgoossen.gooselib.shared.messages.HandshakeReq;
import nl.thomasgoossen.gooselib.shared.messages.HandshakeResp;
import nl.thomasgoossen.gooselib.shared.messages.ShutdownReq;

public class NetworkingListener extends Listener {
    private final boolean manager;
    private final SecretKey encKey;

    // Manager constructor
    public NetworkingListener() {
        this.manager = true;
        this.encKey = null;
    }

    // Connection thread constructor
    public NetworkingListener(int tcp, int udp, SecretKey encKey) {
        this.manager = false;
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

    private void onRequest(Connection conn, EncryptedPacket pkt) {
        Object data = pkt.getDataObject(encKey);
        Logger.dbg("data object in req listener with type: " + data.getClass().getSimpleName());

    }

    private void onManagerRequest(Connection conn, EncryptedPacket pkt) {
        // Packets to manager are not encrypted
        Object data = pkt.getDataObject(null);
        Logger.dbg("data object in manager listener with type: " + data.getClass().getSimpleName());

        switch (data) {
            case ShutdownReq shutdown -> {
                if (Database.auth("admin", shutdown.getAdminPass())) {
                    Logger.log("recv shutdown request with correct admin password, shutting down");
                    NetworkingManager.stop();
                }
            }
            case HandshakeReq req -> {
                if (Database.auth(req.getUsername(), req.getPassword())) {
                    Logger.log("handshake recv from user '" + req.getUsername() + "', handshaking...");
                    int[] info = NetworkingManager.getNextConnection();
                    HandshakeResp resp = new HandshakeResp(info[1], info[2],
                            NetworkingManager.getEncryptionKey(info[0]));
                    EncryptedPacket rPkt = new EncryptedPacket(resp, null);
                    conn.sendTCP(rPkt);
                    NetworkingManager.addConnection(info[0]);
                }
            }
            default -> Logger.warn("deserialized data was not recognized by onManagerRequest()");
        }
    }
}
