package nl.thomasgoossen.gooselib.shared.messages;

import java.io.Serializable;

public class ChunkUploadAck implements Serializable {
    public final int chunkSeq;
    
    public ChunkUploadAck(int chunkSeq) {
        this.chunkSeq = chunkSeq;
    }
}
