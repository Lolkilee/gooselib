package nl.thomasgoossen.gooselib.shared.messages;

import java.io.Serializable;

import nl.thomasgoossen.gooselib.shared.EncryptionHelper;

public class ChunkUploadResp implements Serializable {
    private final String adminPass;
    public final String appName;
    public final int index;
    public final byte[] chunk;

    public ChunkUploadResp(String password, String appName, int index, byte[] chunk) {
        this.adminPass = EncryptionHelper.encryptString(password,
                EncryptionHelper.getPublicKey());
        this.appName = appName;
        this.index = index;
        this.chunk = chunk;
    }

    public String getAdminPass() {
        return EncryptionHelper.decryptString(adminPass,
                EncryptionHelper.getPublicKey());
    }
}
