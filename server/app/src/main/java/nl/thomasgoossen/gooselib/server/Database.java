package nl.thomasgoossen.gooselib.server;

import java.util.concurrent.ConcurrentMap;

import org.mapdb.DB;
import org.mapdb.DBMaker;

import nl.thomasgoossen.gooselib.server.dataclasses.User;

public class Database {

    private final String DATA_FILE = "./db.bin";
    private final DB fileDb;
    private final ConcurrentMap<String, User> usrMap;

    private static Database inst;

    @SuppressWarnings("unchecked")
    public Database() {
        Logger.log("starting database");
        fileDb = DBMaker.fileDB(DATA_FILE).make();
        usrMap = (ConcurrentMap<String, User>) fileDb.hashMap("usr").createOrOpen();
        inst = this;
    }

    public void close() {
        try (fileDb) {
            Logger.log("closing database");
        }
    }

    /**
     * Tries to create a new user with a given password and username
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
     * Tries to authenticate using the usrMap
     * @param username username
     * @param password plain-text password
     * @return whether or not the authentication was successful
     */
    public static boolean auth(String username, String password) {
        if (!inst.usrMap.containsKey(username))
            return false;

        return inst.usrMap.get(username).checkPassword(password);
    }

    /**
     * Checks if username is in the usrMap
     * @param username username
     * @return whether or not username is in the usrMap
     */
    public static boolean hasUser(String username) {
        Logger.dbg("check if user '" + username + "' exists");
        return inst.usrMap.containsKey(username);
    }
    
    /**
     * Clears the user map in the database
     */
    public static void clearUsrMap() {
        Logger.warn("clearing user map");
        inst.usrMap.clear();
    }
}
