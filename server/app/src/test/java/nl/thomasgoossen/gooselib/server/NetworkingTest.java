package nl.thomasgoossen.gooselib.server;

import java.io.IOException;

import javax.crypto.SecretKey;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;

import com.esotericsoftware.kryonet.Client;

import nl.thomasgoossen.gooselib.util.EncryptedPacket;
import nl.thomasgoossen.gooselib.util.EncryptionHelper;
import nl.thomasgoossen.gooselib.util.ShutdownReq;

public class NetworkingTest {
    private Database database;
    private NetworkingManager manager;

    private final String SVR_ADMIN_PASS = "test";

    @Test
    public void testShutdown() {
        try {
            SecretKey key = setupServer();
            assertTrue(NetworkingManager.isRunning());
            Client c = createTestClient();
            ShutdownReq data = new ShutdownReq(SVR_ADMIN_PASS);
            EncryptedPacket pkt = new EncryptedPacket(data, key);
            c.sendTCP(pkt);
            assertFalse(NetworkingManager.isRunning());
        } catch (IOException e) {
            NetworkingManager.stop();
            closeServer();
            fail(e.getMessage());
        }

        NetworkingManager.stop();
        closeServer();
    }

    public Client createTestClient() throws IOException {
        Client client = new Client();
        client.start();
        client.getKryo().register(EncryptedPacket.class);
        client.getKryo().register(byte[].class);
        client.connect(5000, "localhost", NetworkingManager.BEGIN_PORT);
        return client;
    }

    public SecretKey setupServer() throws IOException {
        database = new Database();
        SecretKey encKey = EncryptionHelper.generateKey();
        manager = new NetworkingManager(false, encKey);
        Database.putUser("admin", SVR_ADMIN_PASS);
        Thread t = new Thread(() -> {
            try {
                manager.run();
            } catch (IOException | InterruptedException e) {
                System.out.println(e.getMessage());
            }
        });
        t.start();
        return encKey;
    }

    public void closeServer() {
        Database.clearUsrMap();
        database.close();
        manager.close();
    }
}
