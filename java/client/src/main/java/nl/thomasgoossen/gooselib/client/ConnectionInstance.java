package nl.thomasgoossen.gooselib.client;

import java.io.IOException;

import javax.crypto.SecretKey;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import nl.thomasgoossen.gooselib.shared.EncryptedPacket;
import nl.thomasgoossen.gooselib.shared.KryoHelper;
import nl.thomasgoossen.gooselib.shared.messages.ChunkResp;
import nl.thomasgoossen.gooselib.shared.messages.ChunkUploadReq;
import nl.thomasgoossen.gooselib.shared.messages.ChunkUploadResp;
import nl.thomasgoossen.gooselib.shared.messages.HandshakeResp;
import nl.thomasgoossen.gooselib.shared.messages.LibInfoResp;
import nl.thomasgoossen.gooselib.shared.messages.UploadCompleteMsg;

public class ConnectionInstance {
    private static Client client;
    private static SecretKey key;

    private void dataSwitch(Object data) {
        if (data instanceof ChunkUploadReq) {
            ChunkUploadReq req = (ChunkUploadReq) data;
            if (req.appName.equals(Upload.getCurUploadName())) {
                byte[] chunk = Upload.getChunk(req.index);
                ChunkUploadResp resp = new ChunkUploadResp(
                        Upload.getCurUploadName(),
                        req.index, chunk);
                sendPlainPacketUDP(resp);
            }
        } else if (data instanceof UploadCompleteMsg) {
            UploadCompleteMsg msg = (UploadCompleteMsg) data;
            System.out.println("upload complete, server wrote "
                    + msg.chunksWritten + " chunks");
            Upload.done();
        } else if (data instanceof LibInfoResp) {
            LibInfoResp resp = (LibInfoResp) data;
            GLClient.setMetaData(resp.apps);
            GLClient.setMetaSignal();
        } else if (data instanceof ChunkResp) {
            ChunkResp resp = (ChunkResp) data;
            Download.recvBytes(resp.cIndex, resp.data, resp.appName);
        } else {
            System.out.println("invalid data object");
        }
    }

    private Listener listener() {
        return new Listener() {
            @Override
            public void received(Connection conn, Object obj) {
                if (obj instanceof EncryptedPacket) {
                    EncryptedPacket p = (EncryptedPacket) obj;
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
