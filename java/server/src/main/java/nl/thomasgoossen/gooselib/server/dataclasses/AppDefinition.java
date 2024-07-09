package nl.thomasgoossen.gooselib.server.dataclasses;

import java.io.Serializable;
import java.util.ArrayList;

public class AppDefinition implements Serializable {
    public final String name;
    private String curVersion;
    private final ArrayList<byte[]> chunks;

    public AppDefinition(String name, String version) {
        this.name = name;
        this.curVersion = version;
        this.chunks = new ArrayList<>();
    }

    public void appendChunk(byte[] chunk) {
        chunks.add(chunk);
    }

    public byte[] getChunk(int i) {
        return chunks.get(i);
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
}
