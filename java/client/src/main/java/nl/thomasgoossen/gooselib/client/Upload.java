package nl.thomasgoossen.gooselib.client;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

import nl.thomasgoossen.gooselib.shared.messages.UploadReq;

public class Upload {
    private final static String UPLOAD_FILE = "./temp.tar.gz";
    public final static int CHUNK_SIZE = 1024; // bytes per chunk

    private static String curUploadName = "undefined";
    private static String status = "idle";

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
        int chunkCount = (int) Math.ceil((double) f.length() / CHUNK_SIZE);
        System.out.println("chunks: " + chunkCount);
        UploadReq req = new UploadReq(password, name, version, chunkCount);
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

        File f = new File(UPLOAD_FILE);
        if (end >= f.length())
            end = f.length();

        try (RandomAccessFile raf = new RandomAccessFile(f, "r")) {
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

    public static String getStatus() {
        return status;
    }

    public static String getCurUploadName() {
        return curUploadName;
    }

    public static void done() {
        status = "done";

        // Cleanup temp file
        File f = new File(UPLOAD_FILE);
        f.delete();
    }
}
