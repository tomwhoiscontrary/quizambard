package li.earth.urchin.twic.app;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.net.HttpURLConnection;

public class ExactPathFilter extends SelfDescribingFilter {

    @Override
    public void doFilter(HttpExchange http, Chain chain) throws IOException {
        if (!http.getRequestURI().getPath().equals(http.getHttpContext().getPath())) {
            http.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, -1);
            return;
        }

        chain.doFilter(http);
    }

}
