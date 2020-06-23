package li.earth.urchin.twic.app;

import java.io.FilterOutputStream;
import java.io.OutputStream;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

public class Logging {

    static final String CONSOLE_LOG_FORMAT = "%1$tFT%1$tT.%1$tL%1$tz %3$24s %4$7s %5$s%6$s%n";

    private static Logger mainLogger;

    /**
     * Sets up java.util.logging. All logging goes to the console, in a clean single-line format, flushed after each line. A {@link ExceptionParameterFilter} is applied, so logging can use parameters and exceptions at the same time.
     *
     * @param mainSubject The main class for the application, whose logger is used to report uncaught exceptions
     * @return the logger for the main class
     */
    public static Logger setup(Class<?> mainSubject) {
        if (mainLogger != null) throw new IllegalStateException();

        mainLogger = getLogger(mainSubject);

        LogManager.getLogManager().reset();
        System.setProperty("java.util.logging.SimpleFormatter.format", CONSOLE_LOG_FORMAT);
        Handler handler = new StreamHandler(neverClose(System.out), new SimpleFormatter()) {
            @Override
            public synchronized void publish(LogRecord record) {
                super.publish(record);
                flush();
            }
        };
        handler.setFilter(new ExceptionParameterFilter());
        // TODO add a filter which tags a request ID onto the logger name with an @ if it finds one in a thread local
        java.util.logging.Logger.getLogger("").addHandler(handler);

        Thread.setDefaultUncaughtExceptionHandler((thread, e) -> mainLogger.severe("uncaught exception in thread {0}", thread.getName(), e));

        return mainLogger;
    }

    public static Logger getLogger(Class<?> subject) {
        return new Logger(java.util.logging.Logger.getLogger(subject.getSimpleName()));
    }

    private static OutputStream neverClose(OutputStream out) {
        return new FilterOutputStream(out) {
            @Override
            public void close() {}
        };
    }

    public static class Logger {
        private final java.util.logging.Logger logger;

        public Logger(java.util.logging.Logger logger) {
            this.logger = logger;
        }

        public void log(Level level, String msg, Object... params) {
            logger.log(level, msg, params);
        }

        public void severe(String msg, Object... params) {
            log(Level.SEVERE, msg, params);
        }

        public void warning(String msg, Object... params) {
            log(Level.WARNING, msg, params);
        }

        public void info(String msg, Object... params) {
            log(Level.INFO, msg, params);
        }

        public void fine(String msg, Object... params) {
            log(Level.FINE, msg, params);
        }
    }

}
