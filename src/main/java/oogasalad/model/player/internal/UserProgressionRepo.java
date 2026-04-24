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
import java.util.Optional;
import oogasalad.data.api.LevelId;
import oogasalad.model.player.api.UserProgression;
import oogasalad.model.player.api.UserProgressionRepository;

/**
 * Stores the per-user level-progression cursor in {@code ~/.oogasalad/user_progression.db},
 * a dedicated SQLite file kept separate from the user-account and play-history databases.
 * Schema is created lazily on first construction.
 */
public class UserProgressionRepo implements UserProgressionRepository {

  private static final String COLUMNS =
      "user_id, last_world_key, last_level_key, updated_at";

  private final Connection connection;

  public UserProgressionRepo() {
    this(defaultJdbcUrl());
  }

  public UserProgressionRepo(String jdbcUrl) {
    try {
      this.connection = DriverManager.getConnection(jdbcUrl);
      initSchema();
    } catch (SQLException e) {
      throw new RuntimeException("Failed to open user progression database", e);
    }
  }

  private static String defaultJdbcUrl() {
    try {
      Path dbDir = Path.of(System.getProperty("user.home"), ".oogasalad");
      Files.createDirectories(dbDir);
      return "jdbc:sqlite:" + dbDir.resolve("user_progression.db");
    } catch (Exception e) {
      throw new RuntimeException("Failed to prepare database directory", e);
    }
  }

  private void initSchema() throws SQLException {
    try (Statement s = connection.createStatement()) {
      s.execute("""
          CREATE TABLE IF NOT EXISTS user_progression (
            user_id TEXT PRIMARY KEY,
            last_world_key TEXT NOT NULL,
            last_level_key TEXT NOT NULL,
            updated_at INTEGER NOT NULL
          )
          """);
    }
  }

  @Override
  public void save(UserProgression progression) {
    try (PreparedStatement ps = connection.prepareStatement(
        "INSERT OR REPLACE INTO user_progression(" + COLUMNS + ") VALUES(?, ?, ?, ?)")) {
      ps.setString(1, progression.userId());
      ps.setString(2, progression.lastLevel().worldKey());
      ps.setString(3, progression.lastLevel().levelKey());
      ps.setLong(4, progression.updatedAt().toEpochMilli());
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Failed to save user progression for " + progression.userId(), e);
    }
  }

  @Override
  public Optional<UserProgression> findByUser(String userId) {
    try (PreparedStatement ps = connection.prepareStatement(
        "SELECT " + COLUMNS + " FROM user_progression WHERE user_id = ?")) {
      ps.setString(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(new UserProgression(
              rs.getString("user_id"),
              new LevelId(rs.getString("last_world_key"), rs.getString("last_level_key")),
              Instant.ofEpochMilli(rs.getLong("updated_at"))));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to load user progression for " + userId, e);
    }
    return Optional.empty();
  }
}
