package nl.thomasgoossen.gooselib.server;

import java.util.ArrayList;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class SpeedTests {
    private final int CHUNK_SIZE = 1024;
    private final int CHUNK_COUNT = 10240; // test 10MB files

    @Test
    public void chunkWriteSpeed() {
        Database d = new Database(true);
        Database.createOrClearApp("test", "v1", CHUNK_SIZE);
        ArrayList<byte[]> chunks = getRandomData(CHUNK_SIZE, CHUNK_COUNT);

        long beginTime = System.currentTimeMillis();
        for (byte[] c : chunks) {
            Database.appendChunk("test", c);
        }

        long endTime = System.currentTimeMillis();
        printResult(endTime - beginTime, "write test");
        assertTrue(Database.getChunkCount("test") == CHUNK_COUNT);

        d.close();
    }
    
    @Test
    public void chunkReadSpeed() {
        Database d = new Database(true);
        Database.createOrClearApp("testRead", "v1", CHUNK_SIZE);
        ArrayList<byte[]> chunks = getRandomData(CHUNK_SIZE, CHUNK_COUNT);

        for (byte[] c : chunks) {
            Database.appendChunk("testRead", c);
        }

        chunks.clear();
        long beginTime = System.currentTimeMillis();

        for (int i = 0; i < CHUNK_COUNT; i++) {
            chunks.add(Database.getChunk("testRead", i));
        }

        long endTime = System.currentTimeMillis();
        printResult(endTime - beginTime, "read test");
        assertTrue(chunks.size() == CHUNK_COUNT);
        d.close();
    }

    private ArrayList<byte[]> getRandomData(int chunkSize, int chunkCount) {
        ArrayList<byte[]> arr = new ArrayList<>();
        Random r = new Random();
        for (int i = 0; i < chunkCount; i++) {
            byte[] c = new byte[chunkSize];
            r.nextBytes(c);
            arr.add(c);
        }

        return arr;
    }
    
    private void printResult(long elapsed, String title) {
        double speed = (CHUNK_SIZE * CHUNK_COUNT) / elapsed;

        System.out.println("");
        System.out.println("===== " + title + " result =====");
        System.out.println("elapsed: " + elapsed + " ms");
        System.out.println("speed: " + speed / 1000 + " MB/s");
        System.out.println("");
    }
}
