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
import java.util.zip.Deflater;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipParameters;

import com.esotericsoftware.kryonet.Connection;

import nl.thomasgoossen.gooselib.shared.EncryptedPacket;
import nl.thomasgoossen.gooselib.shared.messages.ChunkUploadResp;
import nl.thomasgoossen.gooselib.shared.messages.UploadReq;

public class Upload {
    private final static String UPLOAD_FILE = "./temp.tar.gz";
    public final static int CHUNK_SIZE = 65536; // bytes per chunk

    private static String curUploadName = "undefined";
    private static String status = "idle";

    private static int chunkCount = 0;
    private static long totalLength = -1;

    private static int curChunk = -1;
    private static FileInputStream fis;

    private static long beginTime = -1;
    private static long bytesSent = 0;

    public static void upload(String password, String folder, String name, String version) {
        curUploadName = name;
        bytesSent = 0;
        beginTime = System.currentTimeMillis();
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

        GzipParameters gzipParams = new GzipParameters();
        gzipParams.setCompressionLevel(Deflater.BEST_SPEED); // HUGE speedup

        byte[] buffer = new byte[256 * 1024]; // 256 KB buffer

        try (FileOutputStream fos = new FileOutputStream(out);
            BufferedOutputStream bos = new BufferedOutputStream(fos, 256 * 1024);
            GzipCompressorOutputStream gzos = new GzipCompressorOutputStream(bos, gzipParams);
            TarArchiveOutputStream taos = new TarArchiveOutputStream(gzos)) {

            taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
            taos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);

            Files.walk(sourcePath)
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    TarArchiveEntry entry = new TarArchiveEntry(
                            path.toFile(),
                            sourcePath.relativize(path).toString()
                    );

                    try (FileInputStream fis = new FileInputStream(path.toFile())) {
                        taos.putArchiveEntry(entry);

                        int read;
                        while ((read = fis.read(buffer)) != -1) {
                            taos.write(buffer, 0, read);
                        }

                        taos.closeArchiveEntry();
                    } catch (IOException e) {
                        System.out.println("compress error: " + e.getMessage());
                    }
                });

        } catch (IOException e) {
            System.out.println("compress failed: " + e.getMessage());
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
            bytesSent += bRead;
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

    public static DownloadInfo[] getDownloadInfos() {
        if (chunkCount <= 0 || curChunk < 0)
            return new DownloadInfo[0];

        float progress = Math.min(1.0f, (float) curChunk / (float) chunkCount);

        if (progress != 1.0f) {
            return new DownloadInfo[] {
                new DownloadInfo(curUploadName, progress, progress, speed())
            };
        } else return new DownloadInfo[] {};
    }
    
    public static long speed() {
        if (beginTime < 0) return 0;
        long elapsed = System.currentTimeMillis() - beginTime;
        return elapsed > 0 ? (bytesSent * 1000) / elapsed : 0;
    }
}
