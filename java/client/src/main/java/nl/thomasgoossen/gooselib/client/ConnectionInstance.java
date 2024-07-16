package nl.thomasgoossen.gooselib.client;

import java.io.IOException;

import javax.crypto.SecretKey;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import nl.thomasgoossen.gooselib.shared.EncryptedPacket;
import nl.thomasgoossen.gooselib.shared.KryoHelper;
import nl.thomasgoossen.gooselib.shared.messages.ChunkReq;
import nl.thomasgoossen.gooselib.shared.messages.ChunkResp;
import nl.thomasgoossen.gooselib.shared.messages.ChunkUploadReq;
import nl.thomasgoossen.gooselib.shared.messages.ChunkUploadResp;
import nl.thomasgoossen.gooselib.shared.messages.HandshakeResp;
import nl.thomasgoossen.gooselib.shared.messages.LibInfoResp;
import nl.thomasgoossen.gooselib.shared.messages.UploadCompleteMsg;

public class ConnectionInstance {

    private final Client client;
    private final SecretKey key;

    private void dataSwitch(Object data) {
        switch (data) {
            case ChunkUploadReq req -> {
                if (req.appName.equals(Upload.getCurUploadName())) {
                    byte[] chunk = Upload.getChunk(req.index);
                    ChunkUploadResp resp = new ChunkUploadResp(
                            Upload.getCurUploadName(),
                            req.index, chunk);
                    sendPlainPacketUDP(resp);
                }
            }
            case UploadCompleteMsg msg -> {
                System.out.println("upload complete, server wrote "
                        + msg.chunksWritten + " chunks");
                Upload.done();
            }
            case LibInfoResp resp -> {
                GLClient.setMetaData(resp.apps);
                GLClient.setMetaSignal();
            }
            case ChunkResp resp -> {
                Download.recvBytes(resp.cIndex, resp.data, resp.appName);
                int next = Download.nextChunk(resp.appName);
                if (next >= 0 && !Download.isDone(resp.appName)) {
                    ChunkReq req = new ChunkReq(resp.appName, next);
                    sendPlainPacketUDP(req);
                }
            }
            default -> {
                System.out.println("invalid data object");
            }
        }
    }

    private Listener listener() {
        return new Listener() {
            @Override
            public void received(Connection conn, Object obj) {
                if (obj instanceof EncryptedPacket p) {
                    Object data = p.getDataObject(key);
                    dataSwitch(data);
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

    public void sendPlainPacketTCP(Object data) {
        EncryptedPacket p = new EncryptedPacket(data);
        client.sendTCP(p);
    }

    public void sendPlainPacketUDP(Object data) {
        EncryptedPacket p = new EncryptedPacket(data);
        client.sendUDP(p);
    }

    public void sendPacketTCP(Object data) {
        EncryptedPacket p = new EncryptedPacket(data, key);
        client.sendTCP(p);
    }

    public void sendPacketUDP(Object data) {
        EncryptedPacket p = new EncryptedPacket(data, key);
        client.sendUDP(p);
    }

    public boolean isConnected() {
        return client.isConnected();
    }
}
