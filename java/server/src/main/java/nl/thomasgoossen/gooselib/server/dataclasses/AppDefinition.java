package nl.thomasgoossen.gooselib.server.dataclasses;

import java.io.BufferedOutputStream;
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
    private String execPath;
    private final String chunksPath;
    private final int chunkSize;
    private long bytesCount = 0;

    private transient volatile boolean readMode = true;

    private transient BufferedOutputStream outStream;

    public AppDefinition(String name, String version, int chunkSize) {
        this.name = name;
        this.chunksPath = APPS_FOLDER + name + ".bin";
        this.curVersion = version;
        this.chunkSize = chunkSize;
        this.readMode = true;

        if (!Files.exists(Paths.get(APPS_FOLDER))) {
            try {
                Files.createDirectories(Paths.get(APPS_FOLDER));
            } catch (IOException e) {
                Logger.err(e.getMessage());
            }
        }

        if (Files.exists(Paths.get(chunksPath))) {
            try {
                Files.delete(Paths.get(chunksPath));
            } catch (IOException e) {
                Logger.err(e.getMessage());
            }
        }
    }

    public void flushBuffer() throws IOException {
        if (outStream != null) {
            outStream.flush();
        }
    }

    public void appendChunk(byte[] chunk) throws IOException {
        if (readMode || outStream == null) {
            outStream = new BufferedOutputStream(new FileOutputStream(chunksPath, true));
            readMode = false;
        }

        outStream.write(chunk);
        bytesCount += chunk.length;
    }

    public byte[] getChunk(int i) throws IOException {
        disableWrites();

        long offset = i * chunkSize;
        int len = chunkSize;
        if (offset + len > bytesCount)
            len = (int) (bytesCount % (long) chunkSize);

        return readChunk(chunksPath, offset, len);
    }

    public int getChunkCount() {
        return (int) Math.ceil((double)bytesCount / chunkSize);
    }

    public void setVersion(String v) {
        this.curVersion = v;
    }

    public String getVersion() {
        return this.curVersion;
    }

    public void deleteFiles() {
        Logger.warn("deleteFiles() called for AppDef " + name);

        try {
            Files.delete(Paths.get(chunksPath));
        } catch (IOException e) {
            Logger.err("err deleting, msg: " + e.getMessage());
        }
    }

    public boolean checkIntegrity() {
        if (!Files.exists(Paths.get(chunksPath)))
            return false;
        File f = new File(chunksPath);
        return f.length() == bytesCount;
    }

    // Should only called by tests
    public ArrayList<byte[]> allChunks() {
        Logger.warn("allChunks() called for AppDef " + name);
        ArrayList<byte[]> arr = new ArrayList<>();
        for (int i = 0; i < getChunkCount(); i++) {
            try {
                arr.add(getChunk(i));
            } catch (IOException e) {
                Logger.err(e.getMessage());
            }
        }
        return arr;
    }

    public AppMetaData getMetaData() {
        return new AppMetaData(this.name, this.curVersion,
                this.execPath, getChunkCount(), this.bytesCount);
    }

    public void disableWrites() {
        readMode = true;
        if (outStream != null) {
            try {
                outStream.close();
            } catch (IOException e) {
                Logger.err(e.getMessage());
            }
            outStream = null;
        }
    }

    public boolean getIsPublic() {
        return readMode;
    }

    public String getExecPath() {
        return execPath;
    }

    public void setExecPath(String execPath) {
        this.execPath = execPath;
    }

    private static byte[] readChunk(String path, long start, int length) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(path, "r")) {
            file.seek(start);
            byte[] buffer = new byte[length];
            file.read(buffer, 0, length);
            return buffer;
        }
    }
}