package nl.thomasgoossen.gooselib.shared.messages;

import java.io.Serializable;

import nl.thomasgoossen.gooselib.shared.EncryptionHelper;

public class HandshakeReq implements Serializable {
    private final String username;
    private final String password;

    public HandshakeReq(String username, String password) {
        this.username = EncryptionHelper.encryptString(username, EncryptionHelper.getPublicKey());
        this.password = EncryptionHelper.encryptString(password, EncryptionHelper.getPublicKey());
    }
    
    public String getUsername() {
        return EncryptionHelper.decryptString(username, EncryptionHelper.getPublicKey());
    }

    public String getPassword() {
        return EncryptionHelper.decryptString(password, EncryptionHelper.getPublicKey());
    }
}
