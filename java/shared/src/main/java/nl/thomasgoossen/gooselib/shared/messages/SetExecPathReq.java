package nl.thomasgoossen.gooselib.shared.messages;

import java.io.Serializable;

import nl.thomasgoossen.gooselib.shared.EncryptionHelper;

public class SetExecPathReq implements Serializable {
    public final String appName;
    public final String execPath;
    private final String adminPass;

    public SetExecPathReq(String appName, String execPath, String adminPass) {
        this.appName = appName;
        this.execPath = execPath;
        this.adminPass = EncryptionHelper.encryptString(adminPass,
                EncryptionHelper.getPublicKey());
    }
    
    public String getAdminPass() {
        return EncryptionHelper.decryptString(adminPass,
                EncryptionHelper.getPublicKey());
    }
}
