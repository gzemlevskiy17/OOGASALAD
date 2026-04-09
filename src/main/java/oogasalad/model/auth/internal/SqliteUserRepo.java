package oogasalad.model.auth.internal;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.UUID;
import oogasalad.model.auth.api.UserRecord;
import oogasalad.model.auth.api.UserRepository;

/**
 * Stores user accounts in a SQLite database file at {@code ~/.oogasalad/users.db}.
 *
 * The database and schema are created automatically on first launch,
 * so no manual setup is needed.
 */
public class SqliteUserRepo implements UserRepository {

  private final Connection connection;

  public SqliteUserRepo() {
    try {
      Path dbDir = Path.of(System.getProperty("user.home"), ".oogasalad");
      Files.createDirectories(dbDir);
      connection = DriverManager.getConnection("jdbc:sqlite:" + dbDir.resolve("users.db"));
      initSchema();
    } catch (Exception e) {
      throw new RuntimeException("Failed to open user database", e);
    }
  }

  private void initSchema() throws SQLException {
    try (Statement s = connection.createStatement()) {
      s.execute("""
          CREATE TABLE IF NOT EXISTS users (
            id TEXT PRIMARY KEY,
            username TEXT UNIQUE NOT NULL,
            password_hash TEXT NOT NULL,
            salt TEXT NOT NULL,
            avatar_path TEXT NOT NULL DEFAULT ''
          )
          """);
    }
  }

  @Override
  public Optional<UserRecord> findByUsername(String username) {
    try (PreparedStatement ps = connection.prepareStatement(
        "SELECT id, username, password_hash, salt, avatar_path FROM users WHERE username = ?")) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(new UserRecord(rs.getString("id"), rs.getString("username"),
              rs.getString("password_hash"), rs.getString("salt"), rs.getString("avatar_path")));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to find user: " + username, e);
    }
    return Optional.empty();
  }

  @Override
  public UserRecord save(String username, String passwordHash, String salt, String avatarPath) {
    String id = UUID.randomUUID().toString();
    String path = avatarPath == null ? "" : avatarPath;
    try (PreparedStatement ps = connection.prepareStatement(
        "INSERT INTO users (id, username, password_hash, salt, avatar_path) VALUES (?, ?, ?, ?, ?)")) {
      ps.setString(1, id);
      ps.setString(2, username);
      ps.setString(3, passwordHash);
      ps.setString(4, salt);
      ps.setString(5, path);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Failed to save user: " + username, e);
    }
    return new UserRecord(id, username, passwordHash, salt, path);
  }

  @Override
  public boolean usernameExists(String username) {
    try (PreparedStatement ps = connection.prepareStatement(
        "SELECT 1 FROM users WHERE username = ?")) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to check username: " + username, e);
    }
  }

  @Override
  public void updateAvatarPath(String username, String avatarPath) {
    try (PreparedStatement ps = connection.prepareStatement(
        "UPDATE users SET avatar_path = ? WHERE username = ?")) {
      ps.setString(1, avatarPath);
      ps.setString(2, username);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Failed to update avatar for: " + username, e);
    }
  }
}
