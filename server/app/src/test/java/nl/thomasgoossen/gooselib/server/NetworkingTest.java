package nl.thomasgoossen.gooselib.server;

import java.io.IOException;

import javax.crypto.SecretKey;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;

import com.esotericsoftware.kryonet.Client;

import nl.thomasgoossen.gooselib.shared.EncryptedPacket;
import nl.thomasgoossen.gooselib.shared.EncryptionHelper;
import nl.thomasgoossen.gooselib.shared.KryoHelper;
import nl.thomasgoossen.gooselib.shared.ShutdownReq;

public class NetworkingTest {
    private final String SVR_ADMIN_PASS = "test";

    private Database database;
    private NetworkingManager manager;

    /**
     * Tests the basic lifecycle of a server:
     * creates a server, client, checks if the server is running
     * and issues a shutdown request
     */
    @Test
    public void testShutdown() {
        try {
            setupServer();
            assertTrue(NetworkingManager.isRunning());
            Client c = createTestClient();
            ShutdownReq data = new ShutdownReq(SVR_ADMIN_PASS);
            EncryptedPacket pkt = new EncryptedPacket(data, null);
            c.sendTCP(pkt);
            Thread.sleep(1000);
            assertFalse(NetworkingManager.isRunning());

            c.stop();
            NetworkingManager.stop();
            closeServer();
        } catch (IOException | InterruptedException e) {
            NetworkingManager.stop();
            closeServer();
            System.out.println(e.toString());
            fail("svr shutdown test exception");
        }
    }

    public Client createTestClient() throws IOException {
        Client client = new Client();
        client.start();
        KryoHelper.addRegisters(client.getKryo());
        client.connect(5000, "localhost", NetworkingManager.BEGIN_PORT);
        return client;
    }

    public SecretKey setupServer() throws IOException {
        if (manager != null)
            manager.close();

        System.out.println("setting up server manager");
        database = new Database(true);
        SecretKey encKey = EncryptionHelper.generateKey();
        manager = new NetworkingManager(false, encKey);
        Database.putUser("admin", SVR_ADMIN_PASS);
        
        Thread t = new Thread(() -> {
            try {
                manager.run();
            } catch (IOException | InterruptedException e) {
                System.out.println("error in manager thread");
                System.out.println(e.getMessage());
            }
        });
        t.start();
        
        return encKey;
    }

    public void closeServer() {
        System.out.println("closing server & database");
        Database.clearUsrMap();
        database.close();
        if (manager != null)
            manager.close();
    }
}
