package li.earth.urchin.twic.quiz;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

class Quiz implements Comparable<Quiz> {

    final UUID id;
    final String name;
    final Map<UUID, Instant> games;

    Quiz(UUID id, String name, Map<UUID, Instant> games) {
        this.id = id;
        this.name = name;
        this.games = games;
    }

    public Set<Map.Entry<UUID, Instant>> getGames() {
        return games.entrySet();
    }

    @Override
    public int compareTo(Quiz that) {
        int d = this.name.compareTo(that.name);
        if (d == 0) d = this.id.compareTo(that.id);
        if (d == 0 && !this.games.equals(that.games)) throw new IllegalStateException();
        return d;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Quiz)) return false;
        Quiz quiz = (Quiz) o;
        return id.equals(quiz.id) &&
               name.equals(quiz.name) &&
               games.equals(quiz.games);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, games);
    }

}
