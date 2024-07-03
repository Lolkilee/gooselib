package nl.thomasgoossen.gooselib.client;

import java.io.IOException;

import javax.crypto.SecretKey;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import nl.thomasgoossen.gooselib.shared.messages.HandshakeResp;

public class ConnectionInstance {

    private final Client client;
    private final SecretKey key;

    private Listener listener() {
        return new Listener() {
            @Override
            public void received(Connection conn, Object obj) {
                //TODO
            }
        };
    }

    public ConnectionInstance(String ip, HandshakeResp resp) throws IOException {
        key = resp.getSessionKey();
        client = new Client();
        client.start();
        client.connect(5000, ip, resp.getTCP(), resp.getUDP());
        client.addListener(listener());
    }

    public void stop() {
        client.stop();
    }
}
