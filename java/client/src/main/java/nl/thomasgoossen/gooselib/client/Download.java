package nl.thomasgoossen.gooselib.client;

import java.io.BufferedInputStream;
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

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import nl.thomasgoossen.gooselib.shared.AppMetaData;
import nl.thomasgoossen.gooselib.shared.messages.ChunkReq;

public class Download {
    private final static String DL_FILE = "./temp.tar.gz";
    private final static int WINDOW_SIZE = 8;

    private final static HashMap<String, Download> instances = new HashMap<>();

    private final int totalChunkCount;
    private final ArrayList<Integer> chunkQ;
    private final HashMap<Integer, byte[]> chunksBuff;
    private final FileOutputStream fos;
    private final String path;
    private final String folder;
    private final String appName;
    private final TimeoutThread tThread;

    private int lastAppended = -1;

    public Download(AppMetaData meta, String folder) throws FileNotFoundException {
        this.appName = meta.name;
        this.totalChunkCount = meta.chunkCount;
        this.folder = folder;
        chunkQ = new ArrayList<>();
        chunksBuff = new HashMap<>();
        path = DL_FILE.replace("temp", meta.name);
        fos = new FileOutputStream(path);
        for (int i = 0; i < meta.chunkCount; i++) {
            chunkQ.add(i);
        }

        Download d = this;
        instances.put(meta.name, d);

        for (int i = 0; i < WINDOW_SIZE && i < meta.chunkCount; i++) {
            ChunkReq req = new ChunkReq(meta.name, i);
            GLClient.sendPacketUDP(req);
        }

        tThread = new TimeoutThread(appName);
    }
    
    public void start() {
        Thread t = new Thread(tThread);
        t.start();
    }
    
    private void pushBuffer() {
        if (!chunksBuff.keySet().isEmpty()) {
            ArrayList<Integer> toRemove = new ArrayList<>();
            ArrayList<Integer> keys = new ArrayList<>(chunksBuff.keySet());
            Collections.sort(keys);

            for (int i : keys) {
                if (i == lastAppended + 1) {
                    toRemove.add(i);
                    lastAppended = i;
                    try {
                        fos.write(chunksBuff.get(i));
                    } catch (IOException e) {
                        System.out.println(e.getMessage());
                    }
                } else
                    break;
            }
            
            for (int i : toRemove) {
                chunkQ.remove(chunkQ.indexOf(i));
                chunksBuff.remove(i);
            }

            // Download is done, decompress and finalize
            if (chunkQ.isEmpty()) {
                try {
                    fos.close();
                    decompress(path, folder);
                    System.out.println("finished download for " + appName);
                    tThread.stop();
                    instances.remove(appName);
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
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
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = tais.read(buffer)) != -1) {
                            os.write(buffer, 0, len);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public void addChunk(int index, byte[] bytes) {
        chunksBuff.put(index, bytes);
    }

    public int next() {
        if (!chunkQ.isEmpty())
            return chunkQ.getFirst();
        return -1;
    }

    public float progress() {
        return ((float) chunkQ.size()) / ((float) totalChunkCount);
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
            d.pushBuffer();
            instances.put(name, d);
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
}