package nl.thomasgoossen.gooselib.server.dataclasses;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
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

    private volatile boolean isPublic = true;

    private transient FileOutputStream outStream = null;
    private transient RandomAccessFile inFile = null;
    private transient FileChannel inChannel = null;

    public AppDefinition(String name, String version, int chunkSize) {
        this.name = name;
        this.chunksPath = APPS_FOLDER + name + ".bin";
        this.curVersion = version;
        this.chunkSize = chunkSize;

        if (!Files.exists(Paths.get(APPS_FOLDER))) {
            try {
                Files.createDirectories(Paths.get(APPS_FOLDER));
            } catch (IOException e) {
                Logger.err(e.getMessage());
            }
        }
    }

    public void appendChunk(byte[] chunk) throws IOException {
        if (inChannel != null) {
            inChannel.close();
            inChannel = null;
        }
        
        if (inFile != null) {
            inFile.close();
            inFile = null;
        }

        if (outStream == null) {
            outStream = new FileOutputStream(chunksPath, true);
        }

        outStream.write(chunk);
        bytesCount += chunk.length;
    }

    public byte[] getChunk(int i) throws IOException {
        if (outStream != null) {
            outStream.close();
            outStream = null;
        }

        if (inFile == null) {
            inFile = new RandomAccessFile(chunksPath, "r");
        }

        if (inChannel == null) {
            inChannel = inFile.getChannel();
        }

        long offset = i * chunkSize;
        int len = chunkSize;
        if (offset + len > bytesCount)
            len = (int) (bytesCount % (long) chunkSize);

        ByteBuffer buffer = ByteBuffer.allocate(len);
        inChannel.position(offset);
        inChannel.read(buffer);
        return buffer.array();
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

    public void setIsPublic(boolean val) {
        this.isPublic = val;
    }

    public boolean getIsPublic() {
        return isPublic;
    }

    public String getExecPath() {
        return execPath;
    }

    public void setExecPath(String execPath) {
        this.execPath = execPath;
    }
}