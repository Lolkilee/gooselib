package nl.thomasgoossen.gooselib.server;

import java.io.IOException;

import nl.thomasgoossen.gooselib.server.Logger.LogLevel;

public class GLServer {
    private static Logger logger;
    private static Database database;

    private static String adminPass = "admin";

    // input args: <admin password> <log level>
    public static void main(String[] args) {
        try {
            init(args);
            exit();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
    
    public static void init(String[] args) throws IOException {
        if (args.length == 2) {
            adminPass = args[0];
            LogLevel lvl = Logger.levelFromString(args[1]);
            logger = new Logger(lvl);
            Logger.log("started with admin password: '" + adminPass + "'");
        } else {
            Logger.log("invalid arguments given, defaulting to password 'admin', and loglevel DEBUG");
        }
        
        database = new Database();
    }

    public static void exit() {
        if (database != null)
            database.close();
        if (logger != null)
            logger.close();
    }
}
