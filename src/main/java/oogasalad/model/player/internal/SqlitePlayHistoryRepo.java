package oogasalad.model.player.internal;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import oogasalad.model.player.api.PlayHistoryEntry;
import oogasalad.model.player.api.PlayHistoryRepository;

/**
 * Stores playthrough history in {@code ~/.oogasalad/play_history.db}, a SQLite file dedicated
 * to gameplay records and kept separate from the user-account database. Schema is created
 * lazily on first construction.
 */
public class SqlitePlayHistoryRepo implements PlayHistoryRepository {

  private static final String COLUMNS =
      "id, user_id, level_id, distance_traveled, elapsed_seconds, score, won, finished_at";

  private final Connection connection;

  public SqlitePlayHistoryRepo() {
    this(defaultJdbcUrl());
  }

  SqlitePlayHistoryRepo(String jdbcUrl) {
    try {
      this.connection = DriverManager.getConnection(jdbcUrl);
      initSchema();
    } catch (SQLException e) {
      throw new RuntimeException("Failed to open play history database", e);
    }
  }

  private static String defaultJdbcUrl() {
    try {
      Path dbDir = Path.of(System.getProperty("user.home"), ".oogasalad");
      Files.createDirectories(dbDir);
      return "jdbc:sqlite:" + dbDir.resolve("play_history.db");
    } catch (Exception e) {
      throw new RuntimeException("Failed to prepare database directory", e);
    }
  }

  private void initSchema() throws SQLException {
    try (Statement s = connection.createStatement()) {
      s.execute("""
          CREATE TABLE IF NOT EXISTS play_history (
            id TEXT PRIMARY KEY,
            user_id TEXT NOT NULL,
            level_id TEXT NOT NULL,
            distance_traveled REAL NOT NULL,
            elapsed_seconds REAL NOT NULL,
            score REAL NOT NULL,
            won INTEGER NOT NULL,
            finished_at INTEGER NOT NULL
          )
          """);
      s.execute("CREATE INDEX IF NOT EXISTS idx_play_history_user_level "
          + "ON play_history(user_id, level_id)");
      s.execute("CREATE INDEX IF NOT EXISTS idx_play_history_user "
          + "ON play_history(user_id)");
    }
  }

  @Override
  public PlayHistoryEntry save(PlayHistoryEntry entry) {
    String id = entry.id() != null ? entry.id() : UUID.randomUUID().toString();
    try (PreparedStatement ps = connection.prepareStatement(
        "INSERT INTO play_history(" + COLUMNS + ") VALUES(?, ?, ?, ?, ?, ?, ?, ?)")) {
      ps.setString(1, id);
      ps.setString(2, entry.userId());
      ps.setString(3, entry.levelId());
      ps.setDouble(4, entry.distanceTraveled());
      ps.setDouble(5, entry.elapsedSeconds());
      ps.setDouble(6, entry.score());
      ps.setInt(7, entry.won() ? 1 : 0);
      ps.setLong(8, entry.finishedAt().toEpochMilli());
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Failed to save play history entry", e);
    }
    return new PlayHistoryEntry(id, entry.userId(), entry.levelId(), entry.distanceTraveled(),
        entry.elapsedSeconds(), entry.score(), entry.won(), entry.finishedAt());
  }

  @Override
  public Optional<PlayHistoryEntry> findBestForLevel(String userId, String levelId) {
    try (PreparedStatement ps = connection.prepareStatement(
        "SELECT " + COLUMNS + " FROM play_history "
            + "WHERE user_id = ? AND level_id = ? ORDER BY score DESC LIMIT 1")) {
      ps.setString(1, userId);
      ps.setString(2, levelId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(toEntry(rs));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to find best for level " + levelId, e);
    }
    return Optional.empty();
  }

  @Override
  public List<PlayHistoryEntry> findAllByUser(String userId) {
    List<PlayHistoryEntry> results = new ArrayList<>();
    try (PreparedStatement ps = connection.prepareStatement(
        "SELECT " + COLUMNS + " FROM play_history "
            + "WHERE user_id = ? ORDER BY finished_at DESC")) {
      ps.setString(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          results.add(toEntry(rs));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to load history for user " + userId, e);
    }
    return results;
  }

  private static PlayHistoryEntry toEntry(ResultSet rs) throws SQLException {
    return new PlayHistoryEntry(
        rs.getString("id"),
        rs.getString("user_id"),
        rs.getString("level_id"),
        rs.getDouble("distance_traveled"),
        rs.getDouble("elapsed_seconds"),
        rs.getDouble("score"),
        rs.getInt("won") != 0,
        Instant.ofEpochMilli(rs.getLong("finished_at"))
    );
  }
}
