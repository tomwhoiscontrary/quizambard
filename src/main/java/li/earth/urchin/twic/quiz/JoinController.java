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

        String playerId;
        try {
            try (Connection connection = db.getConnection()) {
                // need to have a when matched arm to get a row in the final table, so make a no-op update :(
                try (PreparedStatement stmt = connection.prepareStatement("select quiz_game_player_id from final table (merge into quiz_game_player using dual on quiz_game_id = ?2 and name = ?3 when not matched then insert (quiz_game_player_id, quiz_game_id, name) values (?1, ?2, ?3) when matched then update set name = ?3)")) {
                    stmt.setString(1, UUID.randomUUID().toString());
                    stmt.setString(2, gameId.toString());
                    stmt.setString(3, playerName);

                    ResultSet results = stmt.executeQuery();

                    if (!results.next()) throw new SQLException("not found");

                    playerId = results.getString("quiz_game_player_id");

                    if (results.next()) throw new SQLException("more than one result found");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        LOGGER.info("player {0} (re)joined game {1} as {2}", playerName, gameId, playerId);

        return "/play/" + playerId;
    }

}
