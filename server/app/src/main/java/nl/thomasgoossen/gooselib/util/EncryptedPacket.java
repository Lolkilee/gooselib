package nl.thomasgoossen.gooselib.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.crypto.SecretKey;

public class EncryptedPacket {
    
    private final byte[] data;

    public EncryptedPacket(Object data, SecretKey key) {
        this.data = serialize(data, key);
    }
    
    private byte[] serialize(Object inp, SecretKey key) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(inp);
            return EncryptionHelper.encrypt(baos.toByteArray(), key);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public Object getData(SecretKey key) {
        try {
            byte[] decrypted = EncryptionHelper.decrypt(this.data, key);
            ByteArrayInputStream bais = new ByteArrayInputStream(decrypted);
            ObjectInputStream ois = new ObjectInputStream(bais);
            return ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }
}
