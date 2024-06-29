package nl.thomasgoossen.gooselib.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class DatabaseTest {
    @Test
    public void createUserTest() {
        Database db = new Database(true);
        Database.createUser("testUser", "pwd");
        assertTrue(Database.hasUser("testUser"));
        Database.clearUsrMap();
        db.close();
    }

    @Test
    public void createDuplicateUserTest() {
        Database db = new Database(true);
        Database.createUser("testUser", "pwd");
        assertFalse(Database.createUser("testUser", "newpassword"));
        Database.clearUsrMap();
        db.close();
    }

    @Test
    public void persistenceTest() {
        Database db = new Database(false);
        Database.createUser("test", "pwd");
        db.close();
        db = new Database(false);
        assertTrue(Database.hasUser("test"));
        Database.clearUsrMap();
        db.close();
    }

    @Test
    public void userAuthTest() {
        Database db = new Database(true);
        Database.createUser("test", "1234");
        assertTrue(Database.auth("test", "1234"));
        assertFalse(Database.auth("test", "notCorrect"));
        assertFalse(Database.auth("testButFake", "1234"));
        Database.clearUsrMap();
        db.close();
    }

    @Test
    public void removeUserTest() {
        Database db = new Database(true);
        Database.createUser("test", "1234");
        assertTrue(Database.hasUser("test"));
        assertFalse(Database.removeUser("test", "1235"));
        assertTrue(Database.removeUser("test", "1234"));
        assertFalse(Database.hasUser("test"));
        Database.clearUsrMap();
        db.close();
    }
}
