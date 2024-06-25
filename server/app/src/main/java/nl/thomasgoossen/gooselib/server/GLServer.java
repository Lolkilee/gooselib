package nl.thomasgoossen.gooselib.server;

public class GLServer {
    private static Logger logger;
    private static Database database;

    public static void main(String[] args) {
        init();
        exit();
    }
    
    public static void init() {
        database = new Database();
    }

    public static void exit() {
        if (database != null)
            database.close();
        if (logger != null)
            logger.close();
    }
}
