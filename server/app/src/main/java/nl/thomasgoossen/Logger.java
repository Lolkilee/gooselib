package nl.thomasgoossen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
    private static final String FILE_NAME = "latest.log";
    private final PrintWriter logWriter;

    private final LogLevel cur_level;

    public enum LogLevel {
        ERROR(0, "ERROR"), WARNING(1, "WARNING"), INFO(2, "INFO"), DEBUG(3, "DEBUG");

        public final int value;
        public final String name;

        private LogLevel(int value, String name) {
            this.value = value;
            this.name = name;
        }
    }

    public Logger(LogLevel level) throws IOException {
        this.cur_level = level;
        File logFile = new File(FILE_NAME);
        if (logFile.exists()) {
            new PrintWriter(FILE_NAME).close();
        } else {
            logFile.createNewFile();
        }

        logWriter = new PrintWriter(new FileWriter(logFile, true));
    }

    public void dbg(String message) {
        log(message, LogLevel.DEBUG);
    }

    public void log(String message) {
        log(message, LogLevel.INFO);
    }

    public void log(String message, LogLevel level) {
        if (cur_level.value >= level.value) {
            String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
            String msg = "[" + level.name + "][" + time + "] " + message;
            System.out.println(msg);
            logWriter.println(msg);
            logWriter.flush();
        }
    }

    public void close() {
        try (logWriter) {
            log("Closing Logger instance");
        }
    }
}

