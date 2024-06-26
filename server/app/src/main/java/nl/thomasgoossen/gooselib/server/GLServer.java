package nl.thomasgoossen.gooselib.server;

import java.io.IOException;

import nl.thomasgoossen.gooselib.server.Logger.LogLevel;
import nl.thomasgoossen.gooselib.util.EncryptionHelper;

public class GLServer {
    private static Logger logger;
    private static Database database;
    private static NetworkingManager manager;

    private static String adminPass = "admin"; // default password

    /*
     * input args: <admin password> <log level> <flags>
     * flags list:
     * -mt networking multithreading
     */
    public static void main(String[] args) {
        try {
            init(args);
            if (!Database.hasUser("admin"))
                Database.createUser("admin", adminPass);
            manager.run();
            exit();
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
            Logger.log("invalid arguments given, defaulting to password 'admin', and loglevel DEBUG");
        }

        database = new Database();
        manager = new NetworkingManager(checkFlag(args, "mt"),
                    EncryptionHelper.generateKey());
    }

    public static void exit() {
        database.close();
        manager.close();
        logger.close();
    }

    private static boolean checkFlag(String[] args, String flag) {
        for (String a : args) {
            if (a.toLowerCase().replace("-", "").equals(flag))
                return true;
        }

        return false;
    }
}
