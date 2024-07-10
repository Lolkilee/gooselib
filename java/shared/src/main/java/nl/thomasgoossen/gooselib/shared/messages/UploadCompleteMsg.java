package nl.thomasgoossen.gooselib.shared.messages;

import java.io.Serializable;

public class UploadCompleteMsg implements Serializable {
    public final int chunksWritten;

    public UploadCompleteMsg(int chunksWritten) {
        this.chunksWritten = chunksWritten;
    }
}
