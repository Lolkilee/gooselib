package nl.thomasgoossen.dataclasses;

import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

// Class holding user information
public class User implements Serializable {

    private static final int KEY_ITER = 65536;
    private static final int KEY_LEN = 128;
    private static final String HASH_ALG = "PBKDF2WithHmacSHA1";

    private final byte[] hash;
    private final byte[] salt;

    public User(String password) {
        SecureRandom random = new SecureRandom();
        this.salt = new byte[16];
        random.nextBytes(salt);

        this.hash = hashPwd(password);
    }

    public boolean checkPassword(String password) {
        if (hash == null)
            return false;

        byte[] inpHash = hashPwd(password);
        return Arrays.equals(inpHash, this.hash);
    }

    private byte[] hashPwd(String password) {

        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, KEY_ITER, KEY_LEN);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(HASH_ALG);
            byte[] h = factory.generateSecret(spec).getEncoded();
            System.out.println(Arrays.toString(h));
            return h;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            System.out.println("Error hashing password");
            e.getMessage();
        }

        return null;
    }
}
