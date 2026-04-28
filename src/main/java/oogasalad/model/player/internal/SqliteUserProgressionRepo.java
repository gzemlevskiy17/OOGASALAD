package oogasalad.model.player.internal;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import oogasalad.data.api.LevelId;
import oogasalad.model.player.api.UserProgression;
import oogasalad.model.player.api.UserProgressionRepository;

/**
 * Stores per-user progression cursors in {@code ~/.oogasalad/user_progression.db}.
 *
 * <p>Schema carries two (worldKey, levelKey) pairs per user: {@code furthest_*} is nullable
 * and only ever advances to a higher index, while {@code last_clicked_*} is non-null and is
 * overwritten on every selection. Schema is created lazily on first construction.
 */
public class SqliteUserProgressionRepo implements UserProgressionRepository {

  private static final String SELECT_COLUMNS =
      "user_id, furthest_world_key, furthest_level_key, "
          + "last_clicked_world_key, last_clicked_level_key, updated_at";

  private final Connection connection;

  public SqliteUserProgressionRepo() {
    this(defaultJdbcUrl());
  }

  public SqliteUserProgressionRepo(String jdbcUrl) {
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
            furthest_world_key TEXT,
            furthest_level_key TEXT,
            last_clicked_world_key TEXT NOT NULL,
            last_clicked_level_key TEXT NOT NULL,
            updated_at INTEGER NOT NULL
          )
          """);
    }
  }

  @Override
  public Optional<UserProgression> findByUser(String userId) {
    try (PreparedStatement ps = connection.prepareStatement(
        "SELECT " + SELECT_COLUMNS + " FROM user_progression WHERE user_id = ?")) {
      ps.setString(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(toEntry(rs));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to load user progression for " + userId, e);
    }
    return Optional.empty();
  }

  @Override
  public void saveLastClicked(String userId, LevelId clicked) {
    Optional<UserProgression> existing = findByUser(userId);
    LevelId currentFurthest = existing.map(UserProgression::furthestReached).orElse(null);
    upsert(userId, currentFurthest, clicked);
  }

  @Override
  public void advanceFurthestReached(String userId, LevelId newLevel, List<LevelId> playOrder) {
    int newIdx = playOrder.indexOf(newLevel);
    if (newIdx < 0) {
      return;
    }
    Optional<UserProgression> existing = findByUser(userId);
    LevelId currentFurthest = existing.map(UserProgression::furthestReached).orElse(null);
    int currentIdx = currentFurthest != null ? playOrder.indexOf(currentFurthest) : -1;
    if (newIdx <= currentIdx) {
      return;
    }
    LevelId lastClicked = existing.map(UserProgression::lastClicked).orElse(newLevel);
    upsert(userId, newLevel, lastClicked);
  }

  private void upsert(String userId, LevelId furthest, LevelId lastClicked) {
    try (PreparedStatement ps = connection.prepareStatement(
        "INSERT OR REPLACE INTO user_progression("
            + "user_id, furthest_world_key, furthest_level_key, "
            + "last_clicked_world_key, last_clicked_level_key, updated_at) "
            + "VALUES(?, ?, ?, ?, ?, ?)")) {
      ps.setString(1, userId);
      if (furthest != null) {
        ps.setString(2, furthest.worldKey());
        ps.setString(3, furthest.levelKey());
      } else {
        ps.setNull(2, Types.VARCHAR);
        ps.setNull(3, Types.VARCHAR);
      }
      ps.setString(4, lastClicked.worldKey());
      ps.setString(5, lastClicked.levelKey());
      ps.setLong(6, Instant.now().toEpochMilli());
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Failed to upsert user progression for " + userId, e);
    }
  }

  private static UserProgression toEntry(ResultSet rs) throws SQLException {
    String furthestWorld = rs.getString("furthest_world_key");
    String furthestLevel = rs.getString("furthest_level_key");
    LevelId furthest = (furthestWorld != null && furthestLevel != null)
        ? new LevelId(furthestWorld, furthestLevel) : null;
    return new UserProgression(
        rs.getString("user_id"),
        furthest,
        new LevelId(rs.getString("last_clicked_world_key"),
            rs.getString("last_clicked_level_key")),
        Instant.ofEpochMilli(rs.getLong("updated_at")));
  }
}
