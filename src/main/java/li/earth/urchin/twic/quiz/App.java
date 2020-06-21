package li.earth.urchin.twic.quiz;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import li.earth.urchin.twic.app.ExactPathFilter;
import li.earth.urchin.twic.app.Logging;
import li.earth.urchin.twic.app.LoggingFilter;
import li.earth.urchin.twic.app.SimpleThreadFactory;
import li.earth.urchin.twic.app.StaticFileHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.Executors;

public class App {

    private static final Logging.Logger LOGGER = Logging.setup(App.class);

    public static void main(String[] args) {
        int port = 8080;

        try {
            main(port);
        } catch (Throwable e) {
            LOGGER.severe("application failed", e);
            System.exit(1);
        }
    }

    private static void main(int port) throws IOException {
        LOGGER.info("application starting");

        HttpServer httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        httpServer.setExecutor(Executors.newCachedThreadPool(new SimpleThreadFactory(HttpServer.class.getSimpleName())));
        Filter logging = new LoggingFilter(Logging.getLogger(HttpServer.class));
        Filter exactPath = new ExactPathFilter();

        createContext(httpServer, "/", StaticFileHandler.of(App.class, "index.html"), logging, exactPath);

        httpServer.start();
        LOGGER.info("server started on {0}", httpServer.getAddress());

        LOGGER.info("application started");
    }

    private static void createContext(HttpServer httpServer, String path, HttpHandler handler, Filter... filters) {
        HttpContext context = httpServer.createContext(path, handler);
        context.getFilters().addAll(Arrays.asList(filters));
    }

}
