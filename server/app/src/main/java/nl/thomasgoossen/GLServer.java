package nl.thomasgoossen;

import java.io.IOException;

import nl.thomasgoossen.Logger.LogLevel;

public class GLServer {
    public static Logger logger;

    public static void main(String[] args) {
        try {
            logger = new Logger(LogLevel.DEBUG);
            logger.log("Started Logger instance");
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }

        if (logger != null) logger.close();
    }
}
