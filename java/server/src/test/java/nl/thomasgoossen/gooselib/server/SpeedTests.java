package nl.thomasgoossen.gooselib.server;

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
        byte[][] chunks = getRandomData(CHUNK_SIZE, CHUNK_COUNT);

        long beginTime = System.currentTimeMillis();
        for (int i = 0; i < CHUNK_COUNT; i++) {
            byte[] c = chunks[i];
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
        byte[][] chunks = getRandomData(CHUNK_SIZE, CHUNK_COUNT);

        for (int i = 0; i < CHUNK_COUNT; i++) {
            byte[] c = chunks[i];
            Database.appendChunk("testRead", c);
        }

        chunks = new byte[CHUNK_COUNT][];
        long beginTime = System.currentTimeMillis();

        for (int i = 0; i < CHUNK_COUNT; i++) {
            chunks[i] = Database.getChunk("testRead", i);
        }

        long endTime = System.currentTimeMillis();
        printResult(endTime - beginTime, "read test");
        assertTrue(chunks.length == CHUNK_COUNT);
        d.close();
    }

    private byte[][] getRandomData(int chunkSize, int chunkCount) {
        byte[][] arr = new byte[chunkCount][];
        Random r = new Random();
        for (int i = 0; i < chunkCount; i++) {
            byte[] c = new byte[chunkSize];
            r.nextBytes(c);
            arr[i] = c;
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
