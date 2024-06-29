package nl.thomasgoossen.gooselib.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.crypto.SecretKey;

public class EncryptedPacket {
    
    private final byte[] data;

    public EncryptedPacket(byte[] encData) {
        this.data = encData;
    }

    public EncryptedPacket(Object data, SecretKey key) {
        this.data = serialize(data, key);
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
        try {
            if (key != null) {
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
            System.out.println(e.getMessage());
            return null;
        }
    }

    public byte[] getData() {
        return data;
    }

    public int getDataLength() {
        return data.length;
    }
}
