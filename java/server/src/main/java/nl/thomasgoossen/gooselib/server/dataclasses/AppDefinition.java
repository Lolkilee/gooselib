package nl.thomasgoossen.gooselib.server.dataclasses;

import java.io.IOException;
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
    private final ArrayList<String> chunks; // stores paths of files

    private volatile boolean isPublic = true;

    public AppDefinition(String name, String version) {
        this.name = name;
        this.chunksPath = APPS_FOLDER + name + "/";
        this.curVersion = version;
        this.chunks = new ArrayList<>();

        if (!Files.exists(Paths.get(chunksPath))) {
            try {
                Files.createDirectories(Paths.get(chunksPath));
            } catch (IOException e) {
                Logger.err(e.getMessage());
            }
        }
    }

    public void appendChunk(byte[] chunk) {
        String path = chunksPath + chunks.size() + ".bin";
        try {
            Files.write(Paths.get(path), chunk);
        } catch (IOException e) {
            Logger.err(e.getMessage());
        }
        this.chunks.add(path);
    }

    public byte[] getChunk(int i) {
        String path = chunks.get(i);
        byte[] c;
        try {
            c = Files.readAllBytes(Paths.get(path));
            return c;
        } catch (IOException e) {
            Logger.err(e.getMessage());
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
        for (String p : chunks) {
            try {
                Files.delete(Paths.get(p));
            } catch (IOException e) {
                Logger.err(e.getMessage());
            }
        }
        try {
            Files.delete(Paths.get(chunksPath));
        } catch (IOException e) {
            Logger.err("err deleting, msg: " + e.getMessage());
        }
    }

    public boolean checkIntegrity() {
        for (String p : chunks) {
            if(!Files.exists(Paths.get(p)))
                return false;
        }

        return true;
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

    public void setIsPublic(boolean val) {
        this.isPublic = val;
    }

    public boolean getIsPublic() {
        return isPublic;
    }
}