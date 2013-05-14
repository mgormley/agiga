package edu.jhu.agiga;

import java.util.Arrays;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Util {

    private Util() {
        // Private constructor.
    }
    
    public static boolean safeEquals(Object a, Object b) {
        if (a == null && b == null) {
            return true;
        } else if (a == null) {
            return false;
        } else if (b == null) {
            return false;
        } else {
            return a.equals(b);
        }
    }
    
    public static int safeHashCode(Object... objects) {
        return Arrays.hashCode(objects);
    }

    public static void initializeLogging() {
        initializeLogging(Level.INFO);
    }
    
    public static void initializeLogging(Level consoleLevel) {
        LogManager.getLogManager().reset();
        SimpleFormatter sf = new SimpleFormatter() {
            private final long startMillis = System.currentTimeMillis();
            public String format(LogRecord record) {
                StringBuilder sb = new StringBuilder();
                sb.append(record.getMillis() - startMillis);
                sb.append(" ");
                sb.append(String.format("%-7s", record.getLevel()));
                sb.append(" ");
                sb.append(record.getLoggerName());
                sb.append(" - ");
                sb.append(record.getMessage());
                sb.append("\n");
                return sb.toString();
            }
        };
        Handler ch = new ConsoleHandler();
        ch.setFormatter(sf);
        Logger globalLog = Logger.getLogger("");
        globalLog.addHandler(ch);
        
        ch.setLevel(consoleLevel);
    }

}
