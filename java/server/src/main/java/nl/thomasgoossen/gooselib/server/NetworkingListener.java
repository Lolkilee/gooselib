package nl.thomasgoossen.gooselib.server;

import java.util.ArrayList;
import java.util.HashMap;

import javax.crypto.SecretKey;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.FrameworkMessage.KeepAlive;
import com.esotericsoftware.kryonet.Listener;

import nl.thomasgoossen.gooselib.shared.AppMetaData;
import nl.thomasgoossen.gooselib.shared.EncryptedPacket;
import nl.thomasgoossen.gooselib.shared.messages.ChunkUploadReq;
import nl.thomasgoossen.gooselib.shared.messages.ChunkUploadResp;
import nl.thomasgoossen.gooselib.shared.messages.HandshakeReq;
import nl.thomasgoossen.gooselib.shared.messages.HandshakeResp;
import nl.thomasgoossen.gooselib.shared.messages.LibInfoReq;
import nl.thomasgoossen.gooselib.shared.messages.LibInfoResp;
import nl.thomasgoossen.gooselib.shared.messages.ShutdownReq;
import nl.thomasgoossen.gooselib.shared.messages.UploadCompleteMsg;
import nl.thomasgoossen.gooselib.shared.messages.UploadReq;

public class NetworkingListener extends Listener {
    private final boolean manager;
    private final SecretKey encKey;

    // Initial upload reqs to be sent
    private final int UPLOAD_WINDOW = 8;
    private final HashMap<String, UploadBuffer> uploadBuffers = new HashMap<>();
    private final HashMap<String, ArrayList<Integer>> expectedLists = new HashMap<>();

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
        } else if (!(object instanceof KeepAlive)){
            Logger.warn("received an object that was not an EncryptedPacket, ignoring");
            if (object != null)
                Logger.dbg("type of invalid object: " + object
                        .getClass().getCanonicalName());
        }
    }

    private void onRequest(Connection conn, EncryptedPacket pkt) {
        Object data = pkt.getDataObject(encKey);
        Logger.dbg("data object in req listener with type: " + data.getClass().getSimpleName());
    
        switch (data) {
            case UploadReq req -> {
                if (Database.auth("admin", req.getAdminPass())) {
                    Logger.log("upload req recv, sending chunk upload requests");
                    Database.createOrClearApp(req.appName, req.version);
                    uploadBuffers.put(req.appName, new UploadBuffer(req.appName, req.chunkCount));
                    expectedLists.put(req.appName, new ArrayList<>());
                    for (int i = 0; i < UPLOAD_WINDOW && i < req.chunkCount; i++) {
                        expectedLists.get(req.appName).add(i);
                        EncryptedPacket p = new EncryptedPacket(new ChunkUploadReq(req.appName, i), encKey);
                        conn.sendUDP(p);
                    }
                }
            }
            case ChunkUploadResp resp -> {
                if (Database.auth("admin", resp.getAdminPass())
                        && uploadBuffers.containsKey(resp.appName)
                        && expectedLists.containsKey(resp.appName)) {
                    
                    Logger.dbg("chunk recv, size " + resp.chunk.length + ", index " + resp.index);
                    UploadBuffer buff = uploadBuffers.get(resp.appName);
                    buff.addToBuffer(resp.index, resp.chunk);
                    buff.pushBuffer();
                    
                    ArrayList<Integer> expected = expectedLists.get(resp.appName);
                    expected.remove(resp.index);
                    expectedLists.put(resp.appName, expected);

                    int next = buff.next(expectedLists.get(resp.appName));
                    if (next >= 0) {
                        EncryptedPacket p = new EncryptedPacket(new ChunkUploadReq(resp.appName, next), encKey);
                        conn.sendUDP(p);
                    } else {
                        Logger.log("upload buffer completed, cleaning up...");
                        uploadBuffers.remove(resp.appName);
                        UploadCompleteMsg msg = new UploadCompleteMsg(buff.totalCount);
                        EncryptedPacket p = new EncryptedPacket(msg, encKey);
                        conn.sendTCP(p);
                    }
                }
            }
            case LibInfoReq req -> {
                if (Database.auth(req.username, req.password)) {
                    Logger.dbg("sending library info response");
                    ArrayList<AppMetaData> appData = Database.getAppMetas();
                    LibInfoResp resp = new LibInfoResp(appData);
                    EncryptedPacket p = new EncryptedPacket(resp, encKey);
                    conn.sendTCP(p);
                }
            }
            default -> Logger.warn("deserialized data was not recognized by onRequest()");
        }
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
                }
            }
            default -> Logger.warn("deserialized data was not recognized by onManagerRequest()");
        }
    }
}
