package li.earth.urchin.twic.app;

import java.util.logging.Filter;
import java.util.logging.LogRecord;

/**
 * Modifies log messages passing through by moving a throwable from the last parameter to the vacant thrown exception field, if possible.
 * <p>
 * The upshot of this is that you can write log statements like {@code LOGGER.log(Level.WARNING, "error reading {0}", new Object[]{path, e});} and the exception will be dealt with as an exception, rather than as a random surplus parameter.
 * <p>
 * This should be a basic feature of java.util.logging, but it isn't.
 */
public class ExceptionParameterFilter implements Filter {

    @Override
    public boolean isLoggable(LogRecord record) {
        Object[] parameters = record.getParameters();
        if (record.getThrown() == null && parameters != null && parameters.length > 0 && parameters[parameters.length - 1] instanceof Throwable) {
            Throwable thrown = (Throwable) parameters[parameters.length - 1];
            record.setThrown(thrown);
            parameters[parameters.length - 1] = null;
        }

        return true;
    }

}
