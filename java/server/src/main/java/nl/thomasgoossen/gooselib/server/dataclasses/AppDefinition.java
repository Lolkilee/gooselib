package nl.thomasgoossen.gooselib.server.dataclasses;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import nl.thomasgoossen.gooselib.server.Logger;
import nl.thomasgoossen.gooselib.shared.AppMetaData;

public class AppDefinition implements Serializable {
    public static final String APPS_FOLDER = "./app-chunks/";

    public final String name;

    private String curVersion;
    private final String chunksPath;

    // <begin index (inclusive), end index (inclusive)
    private final ArrayList<IChunk> chunks;
    private Long currentEndIndex = (long) 0;

    private RandomAccessFile raf;

    public AppDefinition(String name, String version) {
        this.name = name;
        this.chunksPath = APPS_FOLDER + name + ".bin";
        this.curVersion = version;
        this.chunks = new ArrayList<>();

        if (!Files.exists(Paths.get(APPS_FOLDER))) {
            try {
                Files.createDirectories(Paths.get(APPS_FOLDER));
            } catch (IOException e) {
                Logger.err(e.getMessage());
            }
        }
    }

    public void appendChunk(byte[] chunk) {
        if (raf != null) {
            try {
                raf.close();
                raf = null;
            } catch (IOException e) {
                Logger.err(e.getMessage());
            }
        }

        try (FileOutputStream stream = new FileOutputStream(chunksPath, true)) {
            stream.write(chunk);
            IChunk entry = new IChunk(currentEndIndex, currentEndIndex + chunk.length - 1);
            chunks.addLast(entry);
            currentEndIndex += chunk.length;
        } catch (IOException e) {
            Logger.err(e.getMessage());
        }
    }

    public byte[] getChunk(int i) {
        if (raf == null) {
            try {
                raf = new RandomAccessFile(chunksPath, "r");
            } catch (IOException e) {
                Logger.err(e.getMessage());
            }
        }

        if (raf != null && i >= 0 && i < chunks.size()) {
            IChunk chunkEntry = chunks.get(i);
            long length = chunkEntry.end - chunkEntry.begin + 1;
            try {
                raf.seek(chunkEntry.begin);
                byte[] chunk = new byte[(int) length];
                raf.readFully(chunk);
                return chunk;
            } catch (IOException e1) {
                Logger.err(e1.getMessage());
            }
        }
        
        return null;
    }

    public int getChunkCount() {
        return chunks.size();
    }

    public void setVersion(String v) {
        this.curVersion = v;
    }

    public String getVersion() {
        return this.curVersion;
    }

    public void deleteFiles() {
        Logger.warn("deleteChunks() called for AppDef " + name);
        try {
            Files.delete(Paths.get(chunksPath));
        } catch (IOException e) {
            Logger.err("err deleting, msg: " + e.getMessage());
        }
    }

    public boolean checkIntegrity() {
        File f = new File(chunksPath);
        return (f.length() == currentEndIndex + 1);
    }

    // Should only called by tests
    public ArrayList<byte[]> allChunks() {
        Logger.warn("allChunks() called for AppDef " + name);
        ArrayList<byte[]> arr = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            arr.add(getChunk(i));
        }
        return arr;
    }

    public AppMetaData getMetaData() {
        return new AppMetaData(this.name, this.curVersion, this.chunks.size());
    }
}
