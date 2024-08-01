package nl.thomasgoossen.gooselib.shared.messages;

import java.io.Serializable;

import nl.thomasgoossen.gooselib.shared.EncryptionHelper;

public class PutUserReq implements Serializable {
    public final String username;

    private final String adminPass;
    private final String password;

    public PutUserReq(String adminPass, String username, String password) {
        this.adminPass = EncryptionHelper.encryptString(adminPass,
                EncryptionHelper.getPublicKey());
        this.username = username;
        this.password = EncryptionHelper.encryptString(password,
        EncryptionHelper.getPublicKey());
    }

    public String getAdminPass() {
        return EncryptionHelper.decryptString(adminPass,
                EncryptionHelper.getPublicKey());
    }

    public String getPass() {
        return EncryptionHelper.decryptString(password,
                EncryptionHelper.getPublicKey());
    }
}
