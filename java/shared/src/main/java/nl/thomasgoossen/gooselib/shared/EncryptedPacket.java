package nl.thomasgoossen.gooselib.shared;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.crypto.SecretKey;

public class EncryptedPacket {
    
    public final boolean isEncrypted;
    private final byte[] data;

    public EncryptedPacket(byte[] encData, boolean isEncrypted) {
        this.data = encData;
        this.isEncrypted = isEncrypted;
    }

    public EncryptedPacket(Object data) {
        this.data = serialize(data, null);
        this.isEncrypted = false;
    }

    public EncryptedPacket(Object data, SecretKey key) {
        this.data = serialize(data, key);
        this.isEncrypted = true;
    }
    
    private byte[] serialize(Object inp, SecretKey key) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(inp);
            if (key != null)
                return EncryptionHelper.encrypt(baos.toByteArray(), key);
            else
                return baos.toByteArray();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public Object getDataObject(SecretKey key) {
        if (data != null) {
            try {
                if (key != null && isEncrypted) {
                    byte[] decrypted = EncryptionHelper.decrypt(this.data, key);
                    ByteArrayInputStream bais = new ByteArrayInputStream(decrypted);
                    ObjectInputStream ois = new ObjectInputStream(bais);
                    return ois.readObject();
                } else {
                    ByteArrayInputStream bais = new ByteArrayInputStream(data);
                    ObjectInputStream ois = new ObjectInputStream(bais);
                    return ois.readObject();
                }
            } catch (IOException | ClassNotFoundException e) {
                return null;
            }
        } else
            return null;
    }

    public byte[] getData() {
        return data;
    }

    public int getDataLength() {
        return data.length;
    }
}
