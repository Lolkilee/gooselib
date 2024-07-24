package nl.thomasgoossen.gooselib.server;

import java.io.IOException;

import com.esotericsoftware.minlog.Log;
import static com.esotericsoftware.minlog.Log.LEVEL_ERROR;

import nl.thomasgoossen.gooselib.server.Logger.LogLevel;

public class GLServer {
    private static Logger logger;
    private static Database database;
    private static NetworkingManager manager;

    private static String adminPass = "admin"; // default password
    private static boolean hasCalledExit = false;

    /*
     * input args: <admin password> <log level> <flags>
     * flags list:
     * -mt use networking multithreading
     * -np start database without persistence (changes are not saved)
     */
    public static void main(String[] args) {
        Log.set(LEVEL_ERROR);
        
        // Add shutdown hook to ensure cleanup on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                Logger.log("shutdown hook called");
                exit();
            }
        });

        try {
            init(args);
            if (!Database.hasUser("admin"))
                Database.createUser("admin", adminPass);
            else if (!Database.auth("admin", adminPass))
                Database.changeUserPassword("admin", adminPass);
            manager.run();
        } catch (IOException | InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void init(String[] args) throws IOException {
        if (args.length >= 2) {
            adminPass = args[0];
            LogLevel lvl = Logger.levelFromString(args[1]);
            logger = new Logger(lvl);
            Logger.log("started with admin password: '" + adminPass + "'");
        } else {
            Logger.log("invalid arguments given, defaulting to password 'admin', and loglevel INFO");
        }

        database = new Database(checkFlag(args, "np"));
        manager = new NetworkingManager(checkFlag(args, "mt"));

        Logger.log("init complete, currently "
                + Database.getAppCount()
                + " app(s) in database");
    }

    public static void exit() {
        if (!hasCalledExit) {
            database.close();
            manager.close();
            logger.close();
            hasCalledExit = true;
        }
    }

    private static boolean checkFlag(String[] args, String flag) {
        for (String a : args) {
            if (a.toLowerCase().replace("-", "").equals(flag.toLowerCase()))
                return true;
        }

        return false;
    }
}
