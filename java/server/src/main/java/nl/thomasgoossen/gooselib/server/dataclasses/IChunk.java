package nl.thomasgoossen.gooselib.server.dataclasses;

import java.io.Serializable;

public class IChunk implements Serializable {
    public final long begin;
    public final long end;

    public IChunk(long begin, long end) {
        this.begin = begin;
        this.end = end;
    }
}
