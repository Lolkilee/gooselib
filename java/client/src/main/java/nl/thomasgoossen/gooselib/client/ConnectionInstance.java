package nl.thomasgoossen.gooselib.client;

import java.io.IOException;

import javax.crypto.SecretKey;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import nl.thomasgoossen.gooselib.shared.EncryptedPacket;
import nl.thomasgoossen.gooselib.shared.KryoHelper;
import nl.thomasgoossen.gooselib.shared.messages.ChunkUploadReq;
import nl.thomasgoossen.gooselib.shared.messages.ChunkUploadResp;
import nl.thomasgoossen.gooselib.shared.messages.HandshakeResp;
import nl.thomasgoossen.gooselib.shared.messages.UploadCompleteMsg;

public class ConnectionInstance {

    private final Client client;
    private final SecretKey key;

    private Listener listener() {
        return new Listener() {
            @Override
            public void received(Connection conn, Object obj) {
                if (obj instanceof EncryptedPacket p) {
                    Object data = p.getDataObject(key);
                    switch (data) {
                        case ChunkUploadReq req -> {
                            if (req.appName.equals(Upload.getCurUploadName())) {
                                byte[] chunk = Upload.getChunk(req.index);
                                ChunkUploadResp resp = new ChunkUploadResp(
                                        GLClient.getPassword(),
                                        Upload.getCurUploadName(),
                                        req.index, chunk);
                                GLClient.sendPacketUDP(resp);
                            }
                        }
                        case UploadCompleteMsg msg -> {
                            System.out.println("upload complete, server wrote "
                                    + msg.chunksWritten + " chunks");
                            Upload.done();
                        }
                        default -> {
                            System.out.println("invalid data object");
                        }
                    }
                }
            }
        };
    }

    public ConnectionInstance(String ip, HandshakeResp resp) throws IOException {
        key = resp.getSessionKey();
        client = new Client();
        KryoHelper.addRegisters(client.getKryo());
        client.start();
        client.connect(5000, ip, resp.getTCP(), resp.getUDP());
        client.addListener(listener());
    }

    public void stop() {
        client.stop();
    }

    public void sendPacketTCP(Object data) {
        EncryptedPacket p = new EncryptedPacket(data, key);
        client.sendTCP(p);
    }

    public void sendPacketUDP(Object data) {
        EncryptedPacket p = new EncryptedPacket(data, key);
        client.sendUDP(p);
    }
}
