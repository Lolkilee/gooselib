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
    private static final Object lock = new Object();

    private static Listener listener() {
        return (new Listener() {
            @Override
            public void received(Connection connection, Object obj) {
                synchronized (lock) {
                    if (obj instanceof EncryptedPacket pkt) {
                        Object decoded = pkt.getDataObject(null);
                        if (decoded instanceof HandshakeResp input) {
                            resp = input;
                        }
                    }

                    receivedResp = true;
                    lock.notifyAll();
                }
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

            synchronized (lock) {
                long start = System.currentTimeMillis();
                while (!receivedResp) {
                    long elapsed = System.currentTimeMillis() - start;
                    long waitTime = 5000 - elapsed;
                    if (waitTime <= 0) {
                        return SerializationHelper.jsonError("handshake timeout");
                    }
                    lock.wait(waitTime);
                }
            }

            if (resp != null) {
                return SerializationHelper.seriliazeToString(resp);
            }

        } catch (IOException | InterruptedException e) {
            return SerializationHelper.jsonError(e);
        }

        return "error";
    }
}
