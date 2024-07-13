package nl.thomasgoossen.gooselib.server.dataclasses;

import java.io.Serializable;

public class IChunk implements Serializable {
    public final int subFile;
    public final long begin;
    public final long end;

    public IChunk(int subFile, long begin, long end) {
        this.subFile = subFile;
        this.begin = begin;
        this.end = end;
    }
}
