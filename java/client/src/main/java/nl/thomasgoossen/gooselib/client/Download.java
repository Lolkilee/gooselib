package nl.thomasgoossen.gooselib.client;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import nl.thomasgoossen.gooselib.shared.AppMetaData;
import nl.thomasgoossen.gooselib.shared.Constants;
import nl.thomasgoossen.gooselib.shared.messages.ChunkReq;

public class Download {
    private final static String DL_FILE = "./temp.tar.gz";
    private final static int CHUNK_WINDOW = Constants.DEF_CHUNK_WINDOW;

    private final static HashMap<String, Download> instances = new HashMap<>();
    
    private final AppMetaData meta;
    private final int totalChunkCount;
    private final FileOutputStream fos;
    private final DownloadBuffer buff;
    private final String path;
    private final String folder;
    private final String appName;
    private final long beginTime;
    
    private final ScheduledExecutorService scheduler;

    private volatile boolean done = false;
    public volatile long bytesRecv = 0;
    public volatile int next = 0;

    public Download(AppMetaData meta, String folder) throws FileNotFoundException {
        this.meta = meta;
        this.appName = meta.name;
        this.totalChunkCount = meta.chunkCount;
        this.folder = folder;
        this.scheduler = Executors.newScheduledThreadPool(1);

        path = DL_FILE.replace("temp", meta.name);
        boolean appendMode = false;
        boolean skip = false;
        int chunkOffset = 0;
        if (Files.exists(Paths.get(path))) {
            System.out.println("Previous download already present, attempting to reconstruct...");
            try {
                long l = Files.size(Paths.get(path));
                if (l % Upload.CHUNK_SIZE == 0) {
                    chunkOffset = (int) (l / (long) Upload.CHUNK_SIZE);
                    appendMode = true;
                    System.out.println("Valid reconstruction, starting from index " + chunkOffset);
                } else if (l == meta.bytesCount) {
                    System.out.println("Full download already detected, skipping to installation");
                    appendMode = true;
                    chunkOffset = totalChunkCount - 1;
                    skip = true;
                } else {
                    System.out.println("Existing file not in valid chunks");
                }
            } catch (IOException e) {
                System.out.println("reconstruction failed; " + e.getMessage());
            }
        }

        next = chunkOffset;

        this.fos = new FileOutputStream(path, appendMode);
        this.buff = new DownloadBuffer(meta.name, totalChunkCount, fos, chunkOffset - 1);

        Download d = this;
        instances.put(meta.name, d);

        if (!skip) {
            System.out.println("Sending first request with index: " + next);
            for (int i = 0; i < CHUNK_WINDOW; i++) {
                ChunkReq req = new ChunkReq(appName, next);
                GLClient.sendPlainPacketTCP(req);
                next++;
            }
        }

        beginTime = System.currentTimeMillis();
    }

    private void decompress(String src, String dest) {
        Path destPath = Paths.get(dest);
        try (FileInputStream fis = new FileInputStream(src);
                BufferedInputStream bis = new BufferedInputStream(fis);
                GzipCompressorInputStream gzis = new GzipCompressorInputStream(bis);
                TarArchiveInputStream tais = new TarArchiveInputStream(gzis)) {
            
            System.out.println("expected decompression size: " + meta.bytesCount + ", actual: " + Files.size(Paths.get(path)));
            TarArchiveEntry entry;
            while ((entry = tais.getNextTarEntry()) != null) {
                Path outputPath = destPath.resolve(entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(outputPath);
                } else {
                    Files.createDirectories(outputPath.getParent());
                    try (OutputStream os = Files.newOutputStream(outputPath)) {
                        byte[] outArr = new byte[16384];
                        int len;
                        while ((len = tais.read(outArr)) != -1) {
                            os.write(outArr, 0, len);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("error decompressing: " + e.getMessage());
        }
    }

    public void addChunk(int index, byte[] bytes) {
        buff.addToBuffer(index, bytes);
    }
    
    public static void kill() {
        for (Download inst : instances.values()) {
            inst.scheduler.shutdown();
            inst.buff.scheduler.shutdown();
        }
    }

    private int next() {
        return buff.next(new ArrayList<>());
    }

    public float progress() {
        return ((float) buff.getLastAppended()) / ((float) totalChunkCount);
    }

    public float networkProgress() {
        return 1.0f - ((float) buff.getToRecv().size()) / ((float) totalChunkCount);
    }
    
    // in bytes/second
    public long speed() {
        long elapsed = System.currentTimeMillis() - beginTime;
        return (bytesRecv / elapsed) * 1000;
    }

    public boolean getDone() {
        return done;
    }

    public String getName() {
        return appName;
    }

    public static int nextChunk(String name) {
        if (instances.containsKey(name)) 
            return instances.get(name).next();
        return -1;
    }
    
    public static void recvBytes(int index, byte[] bytes, String name) {
        if (instances.containsKey(name)) {
            Download d = instances.get(name);
            d.addChunk(index, bytes);
            d.bytesRecv += bytes.length;

            if (d.next < d.totalChunkCount) {
                ChunkReq req = new ChunkReq(d.appName, d.next);
                GLClient.sendPacketTCP(req);
                d.next++;
            }
        }
    }

    public static float getProgress(String name) {
        if (instances.containsKey(name)) {
            return instances.get(name).progress();
        }
        return 0f;
    }

    public static DownloadInfo[] getDownloadInfos() {
        DownloadInfo[] arr = new DownloadInfo[instances.values().size()];
        int i = 0;
        for (Download d : instances.values()) {
            arr[i] = new DownloadInfo(d.appName, d.progress(), d.networkProgress(), d.speed());
            i++;
        }

        return arr;
    }

    public static boolean isDone(String name) {
        if (!instances.containsKey(name))
            return true;
        return instances.get(name).getDone();
    }

    public static void finish(String name) {
        if (instances.containsKey(name)) {
            Download d = instances.get(name);
            d.decompress(d.path, d.folder);
            d.done = true;
            instances.remove(name);
            File f = new File(d.path);
            f.delete();
            d.scheduler.shutdown();
        }
    }
}