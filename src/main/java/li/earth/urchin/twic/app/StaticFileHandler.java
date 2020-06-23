package li.earth.urchin.twic.app;

import com.sun.net.httpserver.HttpHandler;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class StaticFileHandler {

    public static HttpHandler of(Class<?> subjectClass, String name) throws FileNotFoundException {
        URL resource = Resources.find(subjectClass, name);

        return http -> {
            boolean serveBody;
            switch (http.getRequestMethod().toUpperCase()) {
                case "GET":
                    serveBody = true;
                    break;
                case "HEAD":
                    serveBody = false;
                    break;
                default:
                    http.sendResponseHeaders(HttpURLConnection.HTTP_BAD_METHOD, -1);
                    return;
            }

            URLConnection connection = resource.openConnection();

            http.getResponseHeaders().set("Content-Type", connection.getContentType());
            http.sendResponseHeaders(HttpURLConnection.HTTP_OK, connection.getContentLength());

            if (!serveBody) return;

            try (InputStream in = connection.getInputStream()) {
                try (OutputStream out = http.getResponseBody()) {
                    in.transferTo(out);
                }
            }
        };
    }

}
