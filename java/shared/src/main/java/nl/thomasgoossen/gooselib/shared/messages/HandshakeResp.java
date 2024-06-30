package nl.thomasgoossen.gooselib.shared.messages;

import java.io.Serializable;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class HandshakeResp implements Serializable {
    private final int tcp;
    private final int udp;
    private final byte[] sessionKey;

    public HandshakeResp(int tcp, int udp, SecretKey sessionKey) {
        this.tcp = tcp;
        this.udp = udp;
        this.sessionKey = sessionKey.getEncoded();
    }

    public int getTCP() {
        return tcp;
    }

    public int getUDP() {
        return udp;
    }

    public SecretKey getSessionKey() {
        return new SecretKeySpec(sessionKey, 0, sessionKey.length, "AES");
    }
}
