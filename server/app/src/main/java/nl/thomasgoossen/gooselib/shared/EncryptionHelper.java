package nl.thomasgoossen.gooselib.shared;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionHelper {
    private static final String PUBLIC_KEY = "Z29vc2VsaWItcHVibGljaw==";

    public static SecretKey getPublicKey() {
        byte[] dec = Base64.getDecoder().decode(PUBLIC_KEY);
        return new SecretKeySpec(dec, 0, dec.length, "AES");
    }

    public static SecretKey generateKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            return keyGen.generateKey();
        } catch (NoSuchAlgorithmException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public static byte[] encrypt(byte[] data, SecretKey key) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher.doFinal(data);
        } catch (InvalidKeyException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException
                | NoSuchPaddingException e) {
            System.out.println(e.getMessage());
            return data;
        }
    }
    
    public static byte[] decrypt(byte[] data, SecretKey key) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            return cipher.doFinal(data);
        } catch (InvalidKeyException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException
                | NoSuchPaddingException e) {
            System.out.println(e.getMessage());
            return data;
        }
    }

    public static String encryptString(String plainText, SecretKey secretKey) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

            byte[] iv = new byte[cipher.getBlockSize()];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);
            IvParameterSpec ivParams = new IvParameterSpec(iv);

            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParams);

            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes("UTF-8"));

            byte[] encryptedIvAndText = new byte[iv.length + encryptedBytes.length];
            System.arraycopy(iv, 0, encryptedIvAndText, 0, iv.length);
            System.arraycopy(encryptedBytes, 0, encryptedIvAndText, iv.length, encryptedBytes.length);

            return Base64.getEncoder().encodeToString(encryptedIvAndText);
        } catch (UnsupportedEncodingException | InvalidAlgorithmParameterException | InvalidKeyException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }
    
    public static String decryptString(String encryptedText, SecretKey secretKey) {
        try {
            byte[] encryptedIvAndText = Base64.getDecoder().decode(encryptedText);
            
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            int ivSize = cipher.getBlockSize();
            byte[] iv = new byte[ivSize];
            byte[] encryptedBytes = new byte[encryptedIvAndText.length - ivSize];
            
            System.arraycopy(encryptedIvAndText, 0, iv, 0, ivSize);
            System.arraycopy(encryptedIvAndText, ivSize, encryptedBytes, 0, encryptedBytes.length);
            
            IvParameterSpec ivParams = new IvParameterSpec(iv);
            
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParams);
            
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            
            return new String(decryptedBytes, "UTF-8");
        } catch (UnsupportedEncodingException | InvalidAlgorithmParameterException | InvalidKeyException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }
}
