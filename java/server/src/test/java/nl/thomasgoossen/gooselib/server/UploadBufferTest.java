package nl.thomasgoossen.gooselib.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class UploadBufferTest {
    private final int CHUNK_SIZE = 1024; // bytes per chunk
    private final int MIN_CHUNKS = 256;
    private final int MAX_CHUNKS = 1024;

    private Database db;
    
    @Test
    public void basicUploadTest() {
        createTestDatabase();
        ArrayList<byte[]> data = generateRandomData();

        Database.createOrClearApp("test", "testver");
        UploadBuffer buff = new UploadBuffer("test", data.size());

        int next = buff.next(new ArrayList<>());
        while (next >= 0) {
            buff.addToBuffer(next, data.get(next));
            buff.pushBuffer();
            next = buff.next(new ArrayList<>());
        }

        assertTrue(buff.isDone());
        assertTrue(dataEqual(data, Database.getApp("test").allChunks()));

        db.close();
    }
    
    @Test
    public void randomOrderUploadTest() {
        createTestDatabase();
        ArrayList<byte[]> data = generateRandomData();

        Random r = new Random();
        Database.createOrClearApp("test", "testver");
        UploadBuffer buff = new UploadBuffer("test", data.size());

        while (!buff.isDone()) {
            ArrayList<Integer> choices = buff.getToRecv();
            int choiceIndex = r.nextInt(choices.size());
            int chunkIndex = choices.get(choiceIndex);
            buff.addToBuffer(chunkIndex, data.get(chunkIndex));
            buff.pushBuffer();
        }

        assertTrue(buff.isDone());
        assertTrue(dataEqual(data, Database.getApp("test").allChunks()));

        db.close();
    }

    @Test
    public void dataEqualTest() {
        ArrayList<byte[]> d1 = generateRandomData();
        ArrayList<byte[]> d2 = generateRandomData();

        assertTrue(dataEqual(d1, d1));
        assertFalse(dataEqual(d1, d2));
    }

    private void createTestDatabase() {
        db = new Database(true);
    }

    private ArrayList<byte[]> generateRandomData() {
        ArrayList<byte[]> data = new ArrayList<>();
        Random r = new Random();
        for (int i = 0; i < r.nextInt(MIN_CHUNKS, MAX_CHUNKS); i++) {
            byte[] b = new byte[CHUNK_SIZE];
            r.nextBytes(b);
            data.add(b);
        }

        return data;
    }

    private boolean dataEqual(ArrayList<byte[]> d1, ArrayList<byte[]> d2) {
        if (d1.size() != d2.size()) {
            System.out.println("Size mismatch!");
            System.out.println(d1.size() + " | " + d2.size());
            return false;
        }
        
        int i = 0;
        for (byte[] c1 : d1) {
            if (!Arrays.equals(c1, d2.get(i)))
                return false;
            i++;
        }

        return true;
    } 
}
