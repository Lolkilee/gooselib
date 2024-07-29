package nl.thomasgoossen.gooselib.client;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

import com.esotericsoftware.kryonet.Connection;

import nl.thomasgoossen.gooselib.shared.EncryptedPacket;
import nl.thomasgoossen.gooselib.shared.messages.ChunkUploadResp;
import nl.thomasgoossen.gooselib.shared.messages.UploadReq;

public class Upload {
    private final static String UPLOAD_FILE = "./temp.tar.gz";
    public final static int CHUNK_SIZE = 1024; // bytes per chunk

    private static String curUploadName = "undefined";
    private static String status = "idle";

    private static int chunkCount = 0;
    private static long totalLength = -1;

    private static int curChunk = -1;
    private static FileInputStream fis;

    public static void upload(String password, String folder, String name, String version) {
        curUploadName = name;
        System.out.println("compressing...");
        compress(folder, UPLOAD_FILE);
        if (!Files.exists(Paths.get(UPLOAD_FILE))) {
            System.out.println("no upload file detected, stopping upload");
            return;
        }

        // Create and send initial uploadReq
        File f = new File(UPLOAD_FILE);
        chunkCount = (int) Math.ceil((double) f.length() / CHUNK_SIZE);
        System.out.println("chunks: " + chunkCount);
        UploadReq req = new UploadReq(password, name, version, chunkCount, CHUNK_SIZE);
        GLClient.sendPacketTCP(req);
        status = "uploading";
    }

    private static void compress(String src, String out) {
        status = "compressing";
        Path sourcePath = Paths.get(src);
        try (FileOutputStream fos = new FileOutputStream(out);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                GzipCompressorOutputStream gzos = new GzipCompressorOutputStream(bos);
                TarArchiveOutputStream taos = new TarArchiveOutputStream(gzos)) {

            Files.walk(sourcePath)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        TarArchiveEntry entry = new TarArchiveEntry(path.toFile(),
                                sourcePath.relativize(path).toString());
                        try {
                            taos.putArchiveEntry(entry);
                            Files.copy(path, taos);
                            taos.closeArchiveEntry();
                        } catch (IOException e) {
                            System.out.println(e.getMessage());
                        }
                    });
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static byte[] getChunk(int chunkIndex) {
        long begin = chunkIndex * CHUNK_SIZE;
        long end = begin + CHUNK_SIZE;

        if (totalLength < 0) {
            File f = new File(UPLOAD_FILE);
            totalLength = f.length();
        }

        if (end >= totalLength)
            end = totalLength;

        try (RandomAccessFile raf = new RandomAccessFile(UPLOAD_FILE, "r")) {
            raf.seek(begin);
            long length = end - begin;
            byte[] buff = new byte[(int) length];
            raf.readFully(buff);
            // System.out.println("read chunk: " + chunkIndex + " | " + begin + " | " +
            // end);
            return buff;
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        return null;
    }

    public static void sendNextBytes(Connection conn) throws IOException {
        if (fis == null || curChunk == -1) {
            curChunk = 0;
            fis = new FileInputStream(UPLOAD_FILE);
        }

        byte[] buff = new byte[CHUNK_SIZE];
        int bRead = fis.read(buff, 0, CHUNK_SIZE);

        if (bRead > 0) {
            byte[] actual = new byte[CHUNK_SIZE];
            if (bRead < CHUNK_SIZE) {
                actual = new byte[bRead];
            }

            System.arraycopy(buff, 0, actual, 0, bRead);

            ChunkUploadResp resp = new ChunkUploadResp(curUploadName, curChunk, actual);
            conn.sendTCP(new EncryptedPacket(resp));
            curChunk++;
        }
    }

    public static String getStatus() {
        return status;
    }

    public static String getCurUploadName() {
        return curUploadName;
    }

    public static void done() {
        if (fis != null) {
            try {
                fis.close();
                fis = null;
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
        status = "done";

        // Cleanup temp file
        File f = new File(UPLOAD_FILE);
        f.delete();
    }

    public static int getChunkCount() {
        return chunkCount;
    }
}
