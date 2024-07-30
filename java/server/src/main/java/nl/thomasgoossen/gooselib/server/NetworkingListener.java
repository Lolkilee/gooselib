package nl.thomasgoossen.gooselib.server;

import java.util.ArrayList;
import java.util.HashMap;

import javax.crypto.SecretKey;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.FrameworkMessage.KeepAlive;
import com.esotericsoftware.kryonet.Listener;

import nl.thomasgoossen.gooselib.shared.AppMetaData;
import nl.thomasgoossen.gooselib.shared.Constants;
import nl.thomasgoossen.gooselib.shared.EncryptedPacket;
import nl.thomasgoossen.gooselib.shared.messages.AuthError;
import nl.thomasgoossen.gooselib.shared.messages.ChunkReq;
import nl.thomasgoossen.gooselib.shared.messages.ChunkResp;
import nl.thomasgoossen.gooselib.shared.messages.ChunkUploadReq;
import nl.thomasgoossen.gooselib.shared.messages.ChunkUploadResp;
import nl.thomasgoossen.gooselib.shared.messages.ChunksReq;
import nl.thomasgoossen.gooselib.shared.messages.HandshakeReq;
import nl.thomasgoossen.gooselib.shared.messages.HandshakeResp;
import nl.thomasgoossen.gooselib.shared.messages.LibInfoReq;
import nl.thomasgoossen.gooselib.shared.messages.LibInfoResp;
import nl.thomasgoossen.gooselib.shared.messages.SetExecPathReq;
import nl.thomasgoossen.gooselib.shared.messages.ShutdownReq;
import nl.thomasgoossen.gooselib.shared.messages.UploadCompleteMsg;
import nl.thomasgoossen.gooselib.shared.messages.UploadReq;

public class NetworkingListener extends Listener {
    public static final int CHUNK_WINDOW = 4; // on request send index + n chunks

    private final boolean manager;
    private final SecretKey encKey;

    // Initial upload reqs to be sent
    private final HashMap<String, UploadBuffer> uploadBuffers = new HashMap<>();

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
        if (data != null) {
            Logger.dbg("data object in req listener with type: " + data.getClass().getSimpleName());

            switch (data) {
                case ChunksReq req -> {
                    int stop = req.beginIndex + req.length;
                    for (int i = req.beginIndex; i < stop && i < Database.getChunkCount(req.appName); i++) {
                        byte[] c = Database.getChunk(req.appName, i);
                        ChunkResp resp = new ChunkResp(req.appName, i, c);
                        EncryptedPacket p = new EncryptedPacket(resp);
                        Logger.dbg("sending chunk " + i + " for app " + req.appName);
                        conn.sendUDP(p);
                    }
                }
                case ChunkReq req -> {
                    if (req.index  < Database.getChunkCount(req.appName)) {
                        byte[] c = Database.getChunk(req.appName, req.index);
                        ChunkResp resp = new ChunkResp(req.appName, req.index, c);
                        EncryptedPacket p = new EncryptedPacket(resp);
                        Logger.dbg("sending chunk " + (req.index) + " for app " + req.appName);
                        conn.sendTCP(p);
                    }
                }
                case UploadReq req -> {
                    if (Database.auth("admin", req.getAdminPass())) {
                        Logger.log("upload req recv, sending chunk upload requests");
                        Database.createOrClearApp(req.appName, req.version, req.chunkSize);
                        uploadBuffers.put(req.appName, new UploadBuffer(req.appName, req.chunkCount, conn));

                        for (int i = 0; i < Constants.DEF_CHUNK_WINDOW; i++) {
                            EncryptedPacket p = new EncryptedPacket(new ChunkUploadReq(req.appName));
                            conn.sendTCP(p);
                        }
                    }
                }
                case ChunkUploadResp resp -> {
                    if (uploadBuffers.containsKey(resp.appName)) {
                        Logger.dbg("chunk recv, size " + resp.chunk.length + ", index " + resp.index);
                        //int latest = uploadFinals.get(resp.appName);
                        // if (resp.index >= latest) {
                        //     ChunkUploadReq req = new ChunkUploadReq(resp.appName, resp.index + 1, CHUNK_WINDOW);
                        //     EncryptedPacket p = new EncryptedPacket(req);
                        //     conn.sendTCP(p);
                        //     uploadFinals.put(resp.appName, resp.index + CHUNK_WINDOW);
                        // }
                        
                        UploadBuffer buff = uploadBuffers.get(resp.appName);
                        buff.addToBuffer(resp.index, resp.chunk);

                        if (resp.index >= buff.totalCount) {
                            Logger.log("all upload chunks received");
                            UploadCompleteMsg msg = new UploadCompleteMsg(buff.totalCount);
                            EncryptedPacket p = new EncryptedPacket(msg, encKey);
                            conn.sendTCP(p);
                        } else {
                            ChunkUploadReq req = new ChunkUploadReq(resp.appName);
                            conn.sendTCP(new EncryptedPacket(req));
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
                case SetExecPathReq req -> {
                    if (Database.auth("admin", req.getAdminPass())) {
                        Database.setExecPath(req.appName, req.execPath);
                    }
                }
                default -> Logger.warn("deserialized data was not recognized by onRequest()");
            }
        }
    }

    private void onManagerRequest(Connection conn, EncryptedPacket pkt) {
        // Packets to manager are not encrypted
        Object data = pkt.getDataObject(null);

        if (data != null) {
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
                    } else {
                        AuthError err = new AuthError("could not auth username + password");
                        EncryptedPacket p = new EncryptedPacket(err, null);
                        conn.sendTCP(p);
                    }
                }
                default -> Logger.warn("deserialized data was not recognized by onManagerRequest()");
            }
        }
    }
}
