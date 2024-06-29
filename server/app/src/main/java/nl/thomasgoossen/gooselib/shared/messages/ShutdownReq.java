package nl.thomasgoossen.gooselib.shared.messages;

import java.io.Serializable;

import nl.thomasgoossen.gooselib.shared.EncryptionHelper;

public class ShutdownReq implements Serializable {
    private final String adminPass;

    public ShutdownReq(String password) {
        this.adminPass = EncryptionHelper.encryptString(password,
                EncryptionHelper.getPublicKey());
    }

    public String getAdminPass() {
        return EncryptionHelper.decryptString(adminPass,
                EncryptionHelper.getPublicKey());
    }
}
