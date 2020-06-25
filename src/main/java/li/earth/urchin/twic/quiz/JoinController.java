package li.earth.urchin.twic.quiz;

import li.earth.urchin.twic.app.Logging;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

class JoinController {

    private static final Logging.Logger LOGGER = Logging.getLogger(JoinController.class);

    static final List<Function<String, Object>> URL_PARAM_PARSERS = List.of(UUID::fromString);
    static final String TEMPLATE_NAME = "join.html";

    private final DataSource db;

    JoinController(DataSource db) {
        this.db = db;
    }

    Map<String, Object> get(List<Object> urlParams) {
        UUID id = (UUID) urlParams.get(0);

        String name;
        Instant started;

        try {
            try (Connection connection = db.getConnection()) {
                try (PreparedStatement stmt = connection.prepareStatement("select q.name, g.started from quiz q join quiz_game g using (quiz_id) where g.quiz_game_id = ?")) {
                    stmt.setString(1, id.toString());

                    try (ResultSet results = stmt.executeQuery()) {
                        if (!results.next()) throw new SQLException("not found");

                        name = results.getString("name");
                        started = results.getTimestamp("started").toInstant();

                        if (results.next()) throw new SQLException("more than one result found");
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return Map.of("name", name,
                      "started", started);
    }

    String post(List<Object> urlParams, Map<String, List<String>> formParams) {
        UUID gameId = (UUID) urlParams.get(0);

        String playerName = formParams.get("name").get(0);

        LOGGER.info("player {0} joined game {1}", playerName, gameId);

        return "/play/" + gameId;
    }

}
