package li.earth.urchin.twic.quiz;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import li.earth.urchin.twic.app.ExactPathFilter;
import li.earth.urchin.twic.app.FormHandler;
import li.earth.urchin.twic.app.Logging;
import li.earth.urchin.twic.app.LoggingFilter;
import li.earth.urchin.twic.app.Resources;
import li.earth.urchin.twic.app.SimpleThreadFactory;
import li.earth.urchin.twic.app.StaticFileHandler;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Executors;

public class App {

    private static final Logging.Logger LOGGER = Logging.setup(App.class);

    public static void main(String[] args) throws IOException {
        int port = 8080;

        Properties dbProperties;
        try (InputStream in = Resources.open(App.class, "db.properties")) {
            dbProperties = loadProperties(in);
        }

        try {
            main(port, dbProperties);
        } catch (Throwable e) {
            LOGGER.severe("application failed", e);
            System.exit(1);
        }
    }

    private static Properties loadProperties(InputStream open) throws IOException {
        Properties properties = new Properties();
        properties.load(open);
        return properties;
    }

    private static void main(int port, Properties dbProperties) throws IOException, SQLException {
        LOGGER.info("application starting");

        HikariConfig dbConfig = new HikariConfig(dbProperties);
        DataSource db = new HikariDataSource(dbConfig);

        HttpServer httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        httpServer.setExecutor(Executors.newCachedThreadPool(new SimpleThreadFactory(HttpServer.class.getSimpleName())));
        Filter logging = new LoggingFilter(Logging.getLogger(HttpServer.class));
        Filter exactPath = new ExactPathFilter();

        createContext(httpServer, "/", StaticFileHandler.of(App.class, "index.html"), logging, exactPath);

        StartController start = new StartController(db);
        createContext(httpServer,
                      "/start",
                      FormHandler.of(App.class,
                                     StartController.URL_PARAM_PARSERS,
                                     StartController.TEMPLATE_NAME,
                                     start::get,
                                     start::post),
                      logging);

        createContext(httpServer,
                      "/join",
                      FormHandler.of(App.class,
                                     List.of(UUID::fromString),
                                     "join.html",
                                     urlParams -> List.of("a quiz with ID " + urlParams.get(0)),
                                     (urlParams, formParams) -> "/play/" + urlParams.get(0)),
                      logging);

        createContext(httpServer, "/play", StaticFileHandler.of(App.class, "play.html"), logging);

        httpServer.start();
        LOGGER.info("server started on {0}", httpServer.getAddress());

        LOGGER.info("application started");
    }

    private static void createContext(HttpServer httpServer, String path, HttpHandler handler, Filter... filters) {
        HttpContext context = httpServer.createContext(path, handler);
        context.getFilters().addAll(Arrays.asList(filters));
    }

}
