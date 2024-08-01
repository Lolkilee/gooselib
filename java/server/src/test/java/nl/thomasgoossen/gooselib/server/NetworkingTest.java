package nl.thomasgoossen.gooselib.server;

import java.io.IOException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import nl.thomasgoossen.gooselib.shared.EncryptedPacket;
import nl.thomasgoossen.gooselib.shared.KryoHelper;
import nl.thomasgoossen.gooselib.shared.messages.HandshakeReq;
import nl.thomasgoossen.gooselib.shared.messages.HandshakeResp;
import nl.thomasgoossen.gooselib.shared.messages.ShutdownReq;

public class NetworkingTest {
    private final String SVR_ADMIN_PASS = "test";

    private Database database;
    private NetworkingManager manager;
    private Thread managerThread;

    private boolean respRecv = false;

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

    @Test
    public void testHandshake() {
        try {
            setupServer();
            assertTrue(NetworkingManager.isRunning());
            Thread.sleep(100);

            Client c = createTestClient();
            c.addListener(new Listener() {
                @Override
                public void received(Connection connection, Object object) {
                    System.out.println("\n====== RESP recv, printing info ======");
                    System.out.println(object.getClass().getSimpleName());
                    if (object instanceof EncryptedPacket pkt) {
                        Object obj = pkt.getDataObject(null);
                        System.out.println(obj.getClass().getSimpleName());
                        if (obj instanceof HandshakeResp resp) {
                            System.out.println("tcp: " + resp.getTCP());
                            System.out.println("udp: " + resp.getUDP());
                            System.out.println(
                                    "key: " + Base64.getEncoder().encodeToString(resp.getSessionKey().getEncoded()));
                            respRecv = true;
                        }
                    }
                    System.out.println("");
                }
            });
            c.start();

            HandshakeReq req = new HandshakeReq("admin", SVR_ADMIN_PASS);
            EncryptedPacket pkt = new EncryptedPacket(req, null);
            c.sendTCP(pkt);

            Thread.sleep(1000);
            assertTrue(respRecv);
            c.stop();
        } catch (IOException | InterruptedException e) {
            fail(e.toString());
        }
    }

    public Client createTestClient() {
        try {
            Client client = new Client();
            client.start();
            KryoHelper.addRegisters(client.getKryo());
            client.connect(5000, "localhost", NetworkingManager.BEGIN_PORT);
            return client;
        } catch (IOException e) {
            System.out.println("error creating client");
            System.out.println(e.toString());
        }
        
        return null;
    }

    public void setupServer() throws IOException {
        if (manager != null)
            manager.close();

        System.out.println("setting up server manager");
        database = new Database(true);
        manager = new NetworkingManager(false);
        Database.putUser("admin", SVR_ADMIN_PASS);
        
        managerThread = new Thread(() -> {
            try {
                manager.run();
            } catch (IOException | InterruptedException e) {
                System.out.println("error in manager thread");
                System.out.println(e.getMessage());
            }
        });
        managerThread.start();

        System.out.println("\n===== Session keys =====");
        for (String s : NetworkingManager.getSessionKeyStrings()) {
            System.out.println(s);
        }
        System.out.println("");
    }

    public void closeServer() {
        System.out.println("closing server & database");
        Database.clearUsrMap();
        database.close();
        if (manager != null)
            manager.close();
        if (managerThread.isAlive()) {
            managerThread.interrupt();
        }
    }
}
