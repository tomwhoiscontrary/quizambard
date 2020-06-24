package li.earth.urchin.twic.app;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class FormHandler {

    private static final Logging.Logger LOGGER = Logging.getLogger(FormHandler.class);

    private static final Path EMPTY_PATH = Path.of("");
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String LOCATION = "Location";

    public static HttpHandler of(Class<?> subjectClass,
                                 List<Function<String, Object>> urlParamParsers,
                                 String templateName,
                                 Function<List<Object>, List<Object>> getAction,
                                 BiFunction<List<Object>, Map<String, List<String>>, String> postAction) throws IOException {
        String templateSource;
        try (InputStream templateResource = Resources.open(subjectClass, templateName)) {
            templateSource = readFully(templateResource);
        }

        MessageFormat template = new MessageFormat(templateSource);

        return http -> {
            boolean get;
            switch (http.getRequestMethod().toUpperCase()) {
                case "GET":
                    get = true;
                    break;
                case "POST":
                    get = false;
                    break;
                default:
                    http.sendResponseHeaders(HttpURLConnection.HTTP_BAD_METHOD, -1);
                    return;
            }

            Path path = Path.of(http.getRequestURI().getPath());
            Path contextPath = Path.of(http.getHttpContext().getPath());
            if (!path.startsWith(contextPath)) {
                LOGGER.warning("request for {0} was routed to {1} but that is not a prefix", path, contextPath);
                http.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, -1);
                return;
            }
            Path urlParamsPath = contextPath.relativize(path);

            if (getNameCount(urlParamsPath) != urlParamParsers.size()) {
                http.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, -1);
                return;
            }

            List<Object> urlParams = parseUrlParameters(urlParamParsers, urlParamsPath);
            if (urlParams.size() != getNameCount(urlParamsPath)) {
                http.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, -1);
                return;
            }

            if (get) {
                List<Object> templateParams = getAction.apply(urlParams);

                String content = template.format(templateParams.toArray(Object[]::new));

                sendContent(http, "text/html", content);
            } else {
                String contentType = http.getRequestHeaders().getFirst(CONTENT_TYPE);
                if (!contentType.equals("application/x-www-form-urlencoded")) {
                    http.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, -1);
                    return;
                }

                Map<String, List<String>> formParams = parseFormParameters(http.getRequestBody());

                String location = postAction.apply(urlParams, formParams);

                sendRedirect(http, location);
            }
        };
    }

    static int getNameCount(Path urlParamsPath) {
        return urlParamsPath.equals(EMPTY_PATH) ? 0 : urlParamsPath.getNameCount();
    }

    private static String readFully(InputStream in) throws IOException {
        return readFully(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    private static String readFully(Reader in) throws IOException {
        StringWriter buf = new StringWriter();
        in.transferTo(buf);
        return buf.toString();
    }

    static List<Object> parseUrlParameters(List<Function<String, Object>> parsers, Path urlParamsPath) {
        return IntStream.range(0, getNameCount(urlParamsPath))
                        .boxed()
                        .flatMap(i -> {
                            String paramString = urlParamsPath.getName(i).toString();
                            try {
                                return Stream.of(parsers.get(i).apply(paramString));
                            } catch (Exception e) {
                                LOGGER.info("could not parse parameter {0}: {1}", i, paramString);
                                return Stream.empty();
                            }
                        })
                        .collect(Collectors.toList());
    }

    private static Map<String, List<String>> parseFormParameters(InputStream body) {
        // TODO apply a map of parsers to these parameters too, for consistency
        return splitOn(body, StandardCharsets.UTF_8, "&")
                .map(nameAndValue -> {
                    int separatorIdx = nameAndValue.indexOf('=');
                    if (separatorIdx == -1) {
                        separatorIdx = nameAndValue.length();
                    }

                    return Map.entry(URLDecoder.decode(nameAndValue.substring(0, separatorIdx), StandardCharsets.UTF_8),
                                     URLDecoder.decode(nameAndValue.substring(separatorIdx + 1), StandardCharsets.UTF_8));
                })
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                                               Collectors.mapping(Map.Entry::getValue,
                                                                  Collectors.toList())));
    }

    private static Stream<String> splitOn(InputStream in, Charset charset, String delimiter) {
        return new Scanner(in, charset).useDelimiter(delimiter).tokens();
    }

    private static void sendContent(HttpExchange http, String contentType, String content) throws IOException {
        http.getResponseHeaders().set(CONTENT_TYPE, contentType);
        http.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0); // we only know the length in characters!
        try (OutputStreamWriter body = new OutputStreamWriter(http.getResponseBody())) {
            body.write(content);
        }
    }

    private static void sendRedirect(HttpExchange http, String location) throws IOException {
        http.getResponseHeaders().set(LOCATION, location);
        http.sendResponseHeaders(HttpURLConnection.HTTP_SEE_OTHER, -1);
    }

}
