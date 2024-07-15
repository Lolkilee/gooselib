package nl.thomasgoossen.gooselib.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.mapdb.DB;
import org.mapdb.DBMaker;

import nl.thomasgoossen.gooselib.server.dataclasses.AppDefinition;
import nl.thomasgoossen.gooselib.server.dataclasses.User;
import nl.thomasgoossen.gooselib.shared.AppMetaData;

public class Database {

    private final String DATA_FILE = "db.bin";
    private final DB fileDb;
    private static ConcurrentMap<String, User> usrMap;
    private static ConcurrentMap<String, AppDefinition> appMap;
    private final boolean temp;

    /**
     * Database class
     * @param temp if set, the database is ran in memory mode, which always starts empty,
     * mostly used for testing purposes
     */
    @SuppressWarnings("unchecked")
    public Database(boolean temp) {
        this.temp = temp;
        if (!temp) {
            Path p = Paths.get(DATA_FILE);
            Logger.log("starting database with path: " + p.toAbsolutePath().toString());
            fileDb = DBMaker.fileDB(p.toAbsolutePath().toString()).make();

            // Load file content into disk cache
            fileDb.getStore().fileLoad();
        }
        else {
            Logger.log("starting database in memory mode");
            fileDb = DBMaker.memoryDB().make();
        }

        usrMap = (ConcurrentMap<String, User>) fileDb.hashMap("user").createOrOpen();
        appMap = (ConcurrentMap<String, AppDefinition>) fileDb.hashMap("appDefs").createOrOpen();
        checkAppsIntegrity();
    }

    public void close() {
        if (temp) { // remove all chunk files
            for (AppDefinition def : appMap.values()) {
                def.deleteFiles();
            }
            try {
                Files.delete(Paths.get(AppDefinition.APPS_FOLDER));
            } catch (IOException e) {
                Logger.err(e.getMessage());
            }
        }

        try (fileDb) {
            Logger.log("closing meta database");
        }
    }

    private void checkAppsIntegrity() {
        Logger.log("checking app integrities");
        ArrayList<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, AppDefinition> entry : appMap.entrySet()) {
            if (!entry.getValue().checkIntegrity()) {
                Logger.warn("Appdef " + entry.getValue().name + " failed integrity check, deleting from DB");
                entry.getValue().deleteFiles();
                toRemove.add(entry.getKey());
            }
        }

        for (String s : toRemove) {
            appMap.remove(s);
        }

        Logger.log("app integrity check complete");
    }

    /** 
     * Creates or replaces an user
     * 
     * @param username username
     * @param password plain-text password
     */
    public static void putUser(String username, String password) {
        User u = new User(password);
        usrMap.put(username, u);
        Logger.log("put user with username '" + username + "'");
    }

    /**
     * Tries to create a new user with a given password and username
     * 
     * @return whether or not the user could be created
     */
    public static boolean createUser(String username, String password) {
        if (!usrMap.containsKey(username)) {
            User u = new User(password);
            usrMap.put(username, u);
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
        if (!usrMap.containsKey(username)) {
            Logger.warn("tried to remove user that does not exist with name '" + username + "'");
            return false;
        }

        if (auth(username, password)) {
            usrMap.remove(username);
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
        if (!usrMap.containsKey(username)) {
            Logger.warn("tried to change password of user '" +
                    username + "', which doesn't exist");
            return;
        }

        User u = new User(newPass);
        usrMap.replace(username, u);
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
        if (!usrMap.containsKey(username)) {
            Logger.warn("tried to authenticate username '" + username + "' which doesn't exist");
            return false;
        }

        return usrMap.get(username).checkPassword(password);
    }

    /**
     * Checks if username is in the usrMap
     * 
     * @param username username
     * @return whether or not username is in the usrMap
     */
    public static boolean hasUser(String username) {
        boolean b = usrMap.containsKey(username);
        Logger.dbg("check if user '" + username + "' exists, result: " + b);
        return b;
    }

    /**
     * Clears the user map in the database
     */
    public static void clearUsrMap() {
        Logger.warn("clearing user map");
        usrMap.clear();
    }

    /**
     * Gets all appnames in database
     * @return string arr containing app names
     */
    public static ArrayList<String> getApps() {
        ArrayList<String> names = new ArrayList<>();
        for (AppDefinition a : appMap.values()) {
            names.add(a.name);
        }
        return names;
    }

    /**
     * Counts the apps in the app database
     * @return amount of apps
     */
    public static int getAppCount() {
        return appMap.values().size();
    }

    /**
     * Appends a chunk to an app definition if present in DB
     * @param name name of app definition
     * @param chunk chunk to append
     */
    public static void appendChunk(String name, byte[] chunk) {
        if (appMap.containsKey(name)) {
            AppDefinition def = appMap.get(name);
            def.appendChunk(chunk);
            appMap.put(name, def);
            Logger.dbg("appended a chunk of size " + chunk.length + " to " + name);
        } else {
            Logger.warn("tried to append to key '" + name + "', which is not present in DB");
        }
    }

    /**
     * Checks if an app exists in database
     * @param name app name
     * @return whether ot not the app is in the database
     */
    public static boolean appExists(String name) {
        return appMap.containsKey(name);
    }

    /**
     * Puts a new app in the appdefinition database
     * @param name name of the app
     * @param version version of the app
     */
    public static void createOrClearApp(String name, String version) {
        appMap.put(name, new AppDefinition(name, version));
    }

    /**
     * Sets the isPublic boolean for an app if it exists
     * @param name app name
     * @param val value to set isPublic to
     */
    public static void setAppPublic(String name, boolean val) {
        if (appMap.containsKey(name)) {
            AppDefinition a = appMap.get(name);
            a.setIsPublic(val);
            appMap.put(name, a);
        }
    }

    /**
     * Returns null if app not present
     * @param name app name
     * @return AppDefinition object
     */
    public static AppDefinition getApp(String name) {
        if (appMap.containsKey(name))
            return appMap.get(name);
        else {
            Logger.err("tried to retrieve app def that is not in database");
            return null;
        }
    }

    /**
     * Retrieves a chunk from the database
     * @param name app name
     * @param index chunk index
     * @return chunk in form of byte[]
     */
    public static byte[] getChunk(String name, int index) {
        if (appMap.containsKey(name) && index < appMap.get(name).getChunkCount()) {
            return appMap.get(name).getChunk(index);
        }
        return null;
    }

    /**
     * Returns a list containg all meta datas of all apps
     * @return list with meta datas
     */
    public static ArrayList<AppMetaData> getAppMetas() {
        ArrayList<AppMetaData> arr = new ArrayList<>();
        for (AppDefinition a : appMap.values()) {
            if (a.getIsPublic())
                arr.add(a.getMetaData());
        }
        return arr;
    }

    /**
     * Sets the optional field of execPath for a given app
     * @param appName name of app
     * @param execPath path of executable
     */
    public static void setExecPath(String appName, String execPath) {
        if (appExists(appName)) {
            AppDefinition def = appMap.get(appName);
            def.setExecPath(execPath);
            appMap.put(appName, def);
        }
    }
}
