package nl.thomasgoossen.gooselib.server;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentMap;

import org.mapdb.DB;
import org.mapdb.DBMaker;

import nl.thomasgoossen.gooselib.server.dataclasses.User;

public class Database {

    private final String DATA_FILE = "db.bin";
    private final DB fileDb;
    private final ConcurrentMap<String, User> usrMap;

    private static Database inst;

    /**
     * Database class
     * @param temp if set, the database is ran in memory mode, which always starts empty,
     * mostly used for testing purposes
     */
    @SuppressWarnings("unchecked")
    public Database(boolean temp) {
        if (!temp) {
            Path p = Paths.get(DATA_FILE);
            Logger.log("starting database with path: " + p.toAbsolutePath().toString());
            fileDb = DBMaker.fileDB(p.toAbsolutePath().toString()).make();
        }
        else {
            Logger.log("starting database in memory mode");
            fileDb = DBMaker.memoryDB().make();
        }

        usrMap = (ConcurrentMap<String, User>) fileDb.hashMap("user").createOrOpen();
        inst = this;
    }

    public void close() {
        try (fileDb) {
            Logger.log("closing database");
        }
    }

    /** 
     * Creates or replaces an user
     * 
     * @param username username
     * @param password plain-text password
     */
    public static void putUser(String username, String password) {
        User u = new User(password);
        inst.usrMap.put(username, u);
        Logger.log("put user with username '" + username + "'");
    }

    /**
     * Tries to create a new user with a given password and username
     * 
     * @return whether or not the user could be created
     */
    public static boolean createUser(String username, String password) {
        if (!inst.usrMap.containsKey(username)) {
            User u = new User(password);
            inst.usrMap.put(username, u);
            Logger.log("created user with username '" + username + "'");
            return true;
        }

        Logger.warn("tried to create a user that already exists, name: '" + username + "'");
        return false;
    }

    /**
     * Tries to remove username if the password is correct
     * 
     * @param username username of user to remove
     * @param password plain-text password of user to remove
     * @return whether or not the user was removed
     */
    public static boolean removeUser(String username, String password) {
        if (!inst.usrMap.containsKey(username)) {
            Logger.warn("tried to remove user that does not exist with name '" + username + "'");
            return false;
        }

        if (auth(username, password)) {
            inst.usrMap.remove(username);
            Logger.log("removed user with name: " + username);
            return true;
        }

        Logger.warn("tried to remove user '" + username + "', with incorrect password");
        return false;
    }

    /**
     * Changes the password for a user, does nothing if user doesn't exists
     * 
     * @param username username of user to change
     * @param newPass new plain-text password
     */
    public static void changeUserPassword(String username, String newPass) {
        if (!inst.usrMap.containsKey(username)) {
            Logger.warn("tried to change password of user '" +
                    username + "', which doesn't exist");
            return;
        }

        User u = new User(newPass);
        inst.usrMap.replace(username, u);
        Logger.log("changed password of user '" + username + "'");
    }

    /**
     * Tries to authenticate using the usrMap
     * 
     * @param username username
     * @param password plain-text password
     * @return whether or not the authentication was successful
     */
    public static boolean auth(String username, String password) {
        if (!inst.usrMap.containsKey(username)) {
            Logger.warn("tried to authenticate username '" + username + "' which doesn't exist");
            return false;
        }

        return inst.usrMap.get(username).checkPassword(password);
    }

    /**
     * Checks if username is in the usrMap
     * 
     * @param username username
     * @return whether or not username is in the usrMap
     */
    public static boolean hasUser(String username) {
        boolean b = inst.usrMap.containsKey(username);
        Logger.dbg("check if user '" + username + "' exists, result: " + b);
        return b;
    }

    /**
     * Clears the user map in the database
     */
    public static void clearUsrMap() {
        Logger.warn("clearing user map");
        inst.usrMap.clear();
    }
}
