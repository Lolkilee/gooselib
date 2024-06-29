package nl.thomasgoossen.gooselib.server.dataclasses;

import javax.crypto.SecretKey;

import com.esotericsoftware.kryonet.Server;

/**
 * Class containing information about a single connection thread
 */
public class ConnectionThreadRecord {
    private final SecretKey key;
    private final Server server;

    public final int tcp;
    public final int udp;

    public int connections = 0;

    public ConnectionThreadRecord(Server s, SecretKey key, int tcp, int udp) {
        this.tcp = tcp;
        this.udp = udp;
        this.key = key;
        this.server = s;
    }

    public SecretKey getKey() {
        return key;
    }

    public Server getServer() {
        return server;
    }
}
