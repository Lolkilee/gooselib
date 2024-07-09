package nl.thomasgoossen.gooselib.shared.messages;

import java.io.Serializable;

import nl.thomasgoossen.gooselib.shared.EncryptionHelper;

public class UploadReq implements Serializable {
    private final String adminPass;
    public final String appName;
    public final String version;
    public final int chunkCount;

    public UploadReq(String password, String name, String version, int chunkCount) {
        this.adminPass = EncryptionHelper.encryptString(password,
                EncryptionHelper.getPublicKey());
        this.appName = name;
        this.version = version;
        this.chunkCount = chunkCount;
    }

    public String getAdminPass() {
        return EncryptionHelper.decryptString(adminPass,
                EncryptionHelper.getPublicKey());
    }
}
