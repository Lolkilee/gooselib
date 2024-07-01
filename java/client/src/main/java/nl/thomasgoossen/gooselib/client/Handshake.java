package nl.thomasgoossen.gooselib.client;

import java.io.IOException;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import nl.thomasgoossen.gooselib.shared.EncryptedPacket;
import nl.thomasgoossen.gooselib.shared.KryoHelper;
import nl.thomasgoossen.gooselib.shared.messages.HandshakeReq;
import nl.thomasgoossen.gooselib.shared.messages.HandshakeResp;

public class Handshake {
    private static final int MANAGER_PORT = 61234;

    private static boolean receivedResp = false;
    private static HandshakeResp resp = null;

    private static Listener listener() {
        return (new Listener() {
            @Override
            public void received(Connection connection, Object obj) {
                System.out.println(obj.getClass().getSimpleName());
                if (obj instanceof EncryptedPacket pkt) {
                    Object decoded = pkt.getDataObject(null);
                    System.out.println(decoded.getClass().getSimpleName());
                    if (decoded instanceof HandshakeResp input) {
                        resp = input;
                    }
                }

                receivedResp = true;
            }
        });
    }
    
    public static String performHandshake(String ip, String username, String password) {
        try {
            Client c = new Client();
            c.start();
            c.addListener(listener());
            KryoHelper.addRegisters(c.getKryo());
            c.connect(5000, ip, MANAGER_PORT);

            HandshakeReq req = new HandshakeReq(username, password);
            EncryptedPacket pkt = new EncryptedPacket(req, null);
            c.sendTCP(pkt);

            long start = System.currentTimeMillis();
            while (!receivedResp) {
                if ((System.currentTimeMillis() - start) < 5000) 
                    return SerializationHelper.jsonError("handshake timeout");
            }

            if (resp != null) {
                return SerializationHelper.seriliazeToString(resp);
            }

        } catch (IOException e) {
            return SerializationHelper.jsonError(e);
        }

        return "error";
    }
}
