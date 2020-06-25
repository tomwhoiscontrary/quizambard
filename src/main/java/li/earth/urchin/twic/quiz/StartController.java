package li.earth.urchin.twic.quiz;

import li.earth.urchin.twic.app.Logging;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Function;

class StartController {

    private static final Logging.Logger LOGGER = Logging.getLogger(StartController.class);

    static final List<Function<String, Object>> URL_PARAM_PARSERS = List.of();
    static final String TEMPLATE_NAME = "start.html";

    private final DataSource db;

    StartController(DataSource db) {
        this.db = db;
    }

    Map<String, Object> get(List<Object> urlParams) {
        SortedSet<Quiz> quizzes = new TreeSet<>();

        try {
            try (Connection connection = db.getConnection()) {
                try (PreparedStatement stmt = connection.prepareStatement("select q.quiz_id, q.name, g.quiz_game_id, g.started from quiz q left outer join quiz_game g using (quiz_id)")) {
                    try (ResultSet results = stmt.executeQuery()) {
                        Quiz quiz = null;

                        while (results.next()) {
                            UUID quizId = UUID.fromString(results.getString("quiz_id"));
                            String name = results.getString("name");
                            // can be null because of outer join
                            UUID gameId = applyIfNotNull(results.getString("quiz_game_id"), UUID::fromString);
                            Instant started = applyIfNotNull(results.getTimestamp("started"), Timestamp::toInstant);

                            if (quiz == null || !quizId.equals(quiz.id)) {
                                quiz = new Quiz(quizId, name, new TreeMap<>());
                                quizzes.add(quiz);
                            }

                            if (gameId != null) {
                                quiz.games.put(gameId, started);
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e); // TODO replace the functions with interfaces which can throw!
        }

        return Map.of("quizzes", quizzes);
    }

    String post(List<Object> urlParams, Map<String, List<String>> formParams) {
        UUID quizId = UUID.fromString(formParams.get("id").get(0));
        UUID gameId = UUID.randomUUID();
        Instant started = Instant.now();

        try {
            try (Connection connection = db.getConnection()) {
                try (PreparedStatement stmt = connection.prepareStatement("insert into quiz_game (quiz_game_id, quiz_id, started) values (?, ?, ?)")) {
                    stmt.setString(1, gameId.toString());
                    stmt.setString(2, quizId.toString());
                    stmt.setTimestamp(3, Timestamp.from(started));

                    int count = stmt.executeUpdate();
                    if (count != 1) throw new SQLException("inserted no rows");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e); // TODO replace the functions with interfaces which can throw!
        }

        LOGGER.info("created game {0} for quiz {1} at {2}", gameId, quizId, started);

        return "/start"; // TODO not right that both this controller and the app know where this controller is mounted
    }

    private static <T, R> R applyIfNotNull(T value, Function<T, R> function) {
        return value != null ? function.apply(value) : null;
    }

}
