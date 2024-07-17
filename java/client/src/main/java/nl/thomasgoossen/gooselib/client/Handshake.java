package nl.thomasgoossen.gooselib.client;

import java.io.IOException;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import nl.thomasgoossen.gooselib.shared.EncryptedPacket;
import nl.thomasgoossen.gooselib.shared.KryoHelper;
import nl.thomasgoossen.gooselib.shared.messages.AuthError;
import nl.thomasgoossen.gooselib.shared.messages.HandshakeReq;
import nl.thomasgoossen.gooselib.shared.messages.HandshakeResp;

public class Handshake {
    private static final int MANAGER_PORT = 61234;

    private static boolean receivedResp = false;
    private static HandshakeResp resp = null;
    private static volatile String err = null;
    private static final Object lock = new Object();

    public static boolean isHandshaking = false;

    private static Listener listener() {
        return (new Listener() {
            @Override
            public void received(Connection connection, Object obj) {
                synchronized (lock) {
                    if (obj instanceof EncryptedPacket) {
                        EncryptedPacket pkt = (EncryptedPacket) obj;
                        Object decoded = pkt.getDataObject(null);
                        
                        if (decoded instanceof HandshakeResp) {
                            resp = (HandshakeResp) decoded;
                        } else if (decoded instanceof AuthError) {
                            err = ((AuthError) decoded).reason;
                        } else {
                            err = "got unexpected packet";
                        }

                        receivedResp = true;
                        lock.notifyAll();
                    } 
                }
            }
        });
    }
    
    public static HandshakeResp performHandshake(String ip, String username, String password)
            throws InterruptedException, IOException {
        
        isHandshaking = true;
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
                        c.stop();
                        return resp;
                    }
                    lock.wait(waitTime);
                }
            }

            c.stop();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        isHandshaking = false;
        return resp;
    }
    
    public static String getError() {
        return err;
    }
}
