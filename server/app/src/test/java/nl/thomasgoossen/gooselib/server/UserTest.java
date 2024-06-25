package nl.thomasgoossen.gooselib.server;

import java.nio.charset.Charset;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import nl.thomasgoossen.gooselib.server.dataclasses.User;

class UserTest {
    @Test
    void passwordTest() {
        // Test 10 random passwords with random lengths (between 4 and 1000)
        for (int i = 0; i < 10; i++) {
            int len = ThreadLocalRandom.current().nextInt(4, 101);
            byte[] arr = new byte[len];
            new Random().nextBytes(arr);
            String pwd = new String(arr, Charset.forName("UTF-8"));

            User usr = new User(pwd);
            assertTrue(usr.checkPassword(pwd));
        }
    }
}
