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
import nl.thomasgoossen.gooselib.shared.messages.ChunkReq;

public class Download {
    private final static String DL_FILE = "./temp.tar.gz";
    private final static int INITIAL_REQ_COUNT = 0;

    private final static HashMap<String, Download> instances = new HashMap<>();

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

    public Download(AppMetaData meta, String folder) throws FileNotFoundException {
        this.appName = meta.name;
        this.totalChunkCount = meta.chunkCount;
        this.folder = folder;
        this.scheduler = Executors.newScheduledThreadPool(1);

        path = DL_FILE.replace("temp", meta.name);
        fos = new FileOutputStream(path);
        this.buff = new DownloadBuffer(meta.name, totalChunkCount, fos);

        Download d = this;
        instances.put(meta.name, d);

        ChunkReq req = new ChunkReq(meta.name, this.next());
        GLClient.sendPlainPacketTCP(req);
        for (int i = 1; i < INITIAL_REQ_COUNT && i < totalChunkCount; i++) {
            ChunkReq nReq = new ChunkReq(meta.name, this.next());
            GLClient.sendPlainPacketUDP(nReq);
        }

        beginTime = System.currentTimeMillis();
    }

    private void decompress(String src, String dest) {
        Path destPath = Paths.get(dest);
        try (FileInputStream fis = new FileInputStream(src);
                BufferedInputStream bis = new BufferedInputStream(fis);
                GzipCompressorInputStream gzis = new GzipCompressorInputStream(bis);
                TarArchiveInputStream tais = new TarArchiveInputStream(gzis)) {

            TarArchiveEntry entry;
            while ((entry = tais.getNextTarEntry()) != null) {
                Path outputPath = destPath.resolve(entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(outputPath);
                } else {
                    Files.createDirectories(outputPath.getParent());
                    try (OutputStream os = Files.newOutputStream(outputPath)) {
                        byte[] outArr = new byte[1024];
                        int len;
                        while ((len = tais.read(outArr)) != -1) {
                            os.write(outArr, 0, len);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
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