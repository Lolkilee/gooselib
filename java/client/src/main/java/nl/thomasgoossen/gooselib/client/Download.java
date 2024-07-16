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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import nl.thomasgoossen.gooselib.shared.AppMetaData;
import nl.thomasgoossen.gooselib.shared.messages.ChunkReq;

public class Download {
    private final static String DL_FILE = "./temp.tar.gz";

    private final static HashMap<String, Download> instances = new HashMap<>();

    private final int totalChunkCount;
    private final ArrayList<Integer> chunkQ;
    private final List<byte[]> buffer;
    private final List<Integer> indices;
    private final FileOutputStream fos;
    private final String path;
    private final String folder;
    private final String appName;
    private final TimeoutThread tThread;
    
    private final ScheduledExecutorService scheduler;
    private volatile boolean pushActive = false;

    private volatile int lastAppended = -1;
    private volatile boolean done = false;

    public Download(AppMetaData meta, String folder) throws FileNotFoundException {
        this.appName = meta.name;
        this.totalChunkCount = meta.chunkCount;
        this.folder = folder;
        chunkQ = new ArrayList<>();
        buffer = Collections.synchronizedList(new ArrayList<>());
        indices = Collections.synchronizedList(new ArrayList<>());;
        this.scheduler = Executors.newScheduledThreadPool(1);

        path = DL_FILE.replace("temp", meta.name);
        fos = new FileOutputStream(path);
        for (int i = 0; i < meta.chunkCount; i++) {
            chunkQ.add(i);
        }

        Download d = this;
        instances.put(meta.name, d);

        ChunkReq req = new ChunkReq(meta.name, 0);
        GLClient.sendPacketTCP(req);

        tThread = new TimeoutThread(appName);
    }
    
    public void start() {
        Thread t = new Thread(tThread);
        t.start();
    }
    
    private void pushBuffer() {
        for (int bufIndex = 0; bufIndex < buffer.size(); bufIndex++) {
            int i = indices.get(bufIndex);
            if (i == lastAppended + 1) {
                try {
                    fos.write(buffer.get(i));
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
                lastAppended = i;
            } else if (!indices.contains(lastAppended + 1))
                break;
        }

        System.out.println(lastAppended + " | " + chunkQ.getFirst());

        for (int i = 0; i < buffer.size(); i++) {
            if (indices.get(i) <= lastAppended) {
                buffer.remove(i);
                indices.remove(i);
            }
        }

        // Download is done, decompress and finalize
        if (buffer.isEmpty() && chunkQ.isEmpty()) {
            System.out.println("finished download for " + appName);
            try {
                fos.close();
                decompress(path, folder);
                done = true;
                tThread.stop();
                instances.remove(appName);
                File f = new File(path);
                f.delete();
                scheduler.shutdown();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
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
                        byte[] buff = new byte[1024];
                        int len;
                        while ((len = tais.read(buff)) != -1) {
                            os.write(buff, 0, len);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public void addChunk(int index, byte[] bytes) {
        buffer.add(bytes);
        indices.add(index);

        chunkQ.remove(chunkQ.indexOf(index));

        if (!pushActive) {
            scheduler.scheduleAtFixedRate(() -> {
                pushBuffer();
            }, 1000, 1000, TimeUnit.MILLISECONDS);
            pushActive = true;
        }
    }
    
    public static void kill() {
        for (Download inst : instances.values()) {
            inst.scheduler.shutdown();
        }
    }

    public int next() {
        if (!chunkQ.isEmpty() && !done) {
            int index = chunkQ.getFirst();
            return index;
        }
        return -1;
    }

    public float progress() {
        return 1.0f - ((float) chunkQ.size()) / ((float) totalChunkCount);
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
            d.tThread.updateRecv();
            d.addChunk(index, bytes);
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
            arr[i] = new DownloadInfo(d.appName, d.progress());
            i++;
        }

        return arr;
    }

    public static boolean isDone(String name) {
        if (!instances.containsKey(name))
            return true;
        return instances.get(name).getDone();
    }
}