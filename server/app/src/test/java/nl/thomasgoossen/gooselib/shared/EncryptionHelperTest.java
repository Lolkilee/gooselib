package nl.thomasgoossen.gooselib.shared;

import java.util.Random;

import javax.crypto.SecretKey;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import org.junit.jupiter.api.Test;

public class EncryptionHelperTest {
    @Test
    public void testKeyGen() {
        assertDoesNotThrow(() -> EncryptionHelper.generateKey());
    }

    @Test
    public void testEncryption() {
        // Test if encryption outputs the same output 10 times
        byte[] arr = new byte[100];
        new Random().nextBytes(arr);
        SecretKey key = EncryptionHelper.generateKey();
        byte[] initial = EncryptionHelper.encrypt(arr, key);

        for (int i = 0; i < 10; i++) {
            byte[] test = EncryptionHelper.encrypt(arr, key);
            assertArrayEquals(initial, test);
        }
    }

    @Test
    public void testDecryption() {
        // Test if decryption outputs the same output 10 times
        byte[] og = new byte[128];
        new Random().nextBytes(og);
        SecretKey key = EncryptionHelper.generateKey();
        byte[] arr = EncryptionHelper.encrypt(og, key);

        for (int i = 0; i < 10; i++) {
            byte[] test = EncryptionHelper.decrypt(arr, key);
            assertArrayEquals(og, test);
        }
    }
}
