package nl.thomasgoossen.gooselib.server.dataclasses;

import java.io.Serializable;

public class IChunk implements Serializable {
    public final long offset;
    public final int length;

    public IChunk(long offset, int length) {
        this.offset = offset;
        this.length = length;
    }
}
