package li.earth.urchin.twic.app;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.net.HttpURLConnection;

public class LoggingFilter extends SelfDescribingFilter {

    private final Logging.Logger logger;

    public LoggingFilter(Logging.Logger logger) {
        this.logger = logger;
    }

    @Override
    public void doFilter(HttpExchange http, Chain chain) {
        logReceived(http);

        try {
            chain.doFilter(http);
        } catch (IOException | RuntimeException e) {
            logError("handling", http, e);
        }

        if (http.getResponseCode() == -1) {
            try {
                http.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, -1);
            } catch (IOException e) {
                logError("aborting", http, e);
            }
        }

        http.close();

        logHandled(http);
    }

    private void logReceived(HttpExchange http) {
        logger.info("received request from {0} for {1}",
                    http.getRemoteAddress(),
                    http.getRequestURI());
    }

    private void logError(String action, HttpExchange http, Exception e) {
        logger.severe("error {0} request from {1} for {2}",
                      action,
                      http.getRemoteAddress(),
                      http.getRequestURI(),
                      e);
    }

    private void logHandled(HttpExchange http) {
        logger.info("handled request from {0} for {1} with {2}",
                    http.getRemoteAddress(),
                    http.getRequestURI(),
                    http.getResponseCode());
    }

}
