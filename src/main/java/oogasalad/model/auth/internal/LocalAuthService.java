package oogasalad.model.auth.internal;

import java.util.Optional;
import oogasalad.model.auth.api.AuthResult;
import oogasalad.model.auth.api.AuthService;
import oogasalad.model.auth.api.PlayerProfile;
import oogasalad.model.auth.api.SessionContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Authenticates players against a local SQLite database.
 * <p>
 * Delegates password hashing to {@link PasswordHasher} and storage to {@link UserRepository},
 * keeping this class focused on login/register logic only.
 *
 * @author Paulina Vvedenskaya
 */
public class LocalAuthService implements AuthService {

  private static final Logger LOG = LogManager.getLogger(LocalAuthService.class);

  private final UserRepository repo;
  private PlayerProfile loggedInUser;

  /** Creates a service that reads and writes accounts through the given repository. */
  public LocalAuthService(UserRepository repo) {
    this.repo = repo;
  }

  private static ProfileRecord toProfile(UserRecord r) {
    return new ProfileRecord(r.username(), r.avatarPath());
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }

  /**
   * Logs in a player with the given credentials.
   * Returns a failure if the username or password is blank, or if the credentials are wrong.
   */
  @Override
  public AuthResult login(String username, String password) {
    if (isBlank(username) || isBlank(password)) {
      return new AuthResult.Failure("Username and password are required");
    }
    Optional<UserRecord> record = repo.findByUsername(username.trim());
    if (record.isEmpty() || !PasswordHasher.matches(password, record.get().passwordHash(),
        record.get().salt())) {
      LOG.warn("Login failed for user '{}'", username.trim());
      return new AuthResult.Failure("Invalid username or password");
    }
    loggedInUser = toProfile(record.get());
    LOG.info("Login succeeded for user '{}'", loggedInUser.username());
    return new AuthResult.Success(loggedInUser);
  }

  /**
   * Creates a new account with the given username, password, and optional avatar path.
   * The password is salted and hashed before storage — it is never saved in plain text.
   * Returns a failure if the username is already taken or if any required field is blank.
   */
  @Override
  public AuthResult register(String username, String password, String avatarPath) {
    if (isBlank(username)) {
      return new AuthResult.Failure("Username is required");
    }
    if (isBlank(password)) {
      return new AuthResult.Failure("Password is required");
    }
    if (repo.usernameExists(username.trim())) {
      return new AuthResult.Failure("Username already taken");
    }

    String salt = PasswordHasher.generateSalt();
    String hash = PasswordHasher.hash(password, salt);
    String path = avatarPath == null ? "" : avatarPath;
    UserRecord saved = repo.save(username.trim(), hash, salt, path);
    loggedInUser = toProfile(saved);
    LOG.info("Account created for user '{}'", loggedInUser.username());
    return new AuthResult.Success(loggedInUser);
  }

  /**
   * Updates the avatar for whoever is currently logged in. Returns a failure if no player is logged
   * in.
   */
  @Override
  public AuthResult updateAvatar(String newAvatarPath) {
    if (loggedInUser == null) {
      return new AuthResult.Failure("Not logged in");
    }
    String path = newAvatarPath == null ? "" : newAvatarPath;
    repo.updateAvatarPath(loggedInUser.username(), path);
    loggedInUser = new ProfileRecord(loggedInUser.username(), path);
    return new AuthResult.Success(loggedInUser);
  }

  /**
   * Renames the currently logged-in player. Returns a failure if no player is logged in, the new
   * username is blank, or the new username is already taken.
   */
  @Override
  public AuthResult updateUsername(String newUsername) {
    if (loggedInUser == null) {
      return new AuthResult.Failure("Not logged in");
    }
    if (isBlank(newUsername)) {
      return new AuthResult.Failure("Username is required");
    }
    String trimmed = newUsername.trim();
    if (trimmed.equals(loggedInUser.username())) {
      return new AuthResult.Success(loggedInUser);
    }
    if (repo.usernameExists(trimmed)) {
      return new AuthResult.Failure("Username already taken");
    }
    repo.updateUsername(loggedInUser.username(), trimmed);
    loggedInUser = new ProfileRecord(trimmed, loggedInUser.avatarPath());
    return new AuthResult.Success(loggedInUser);
  }

  /**
   * Updates the password for the currently logged-in player. The new password is salted and hashed
   * before storage. Returns a failure if no player is logged in or the password is blank.
   */
  @Override
  public AuthResult updatePassword(String newPassword) {
    if (loggedInUser == null) {
      return new AuthResult.Failure("Not logged in");
    }
    if (isBlank(newPassword)) {
      return new AuthResult.Failure("Password is required");
    }
    String salt = PasswordHasher.generateSalt();
    String hash = PasswordHasher.hash(newPassword, salt);
    repo.updatePassword(loggedInUser.username(), hash, salt);
    return new AuthResult.Success(loggedInUser);
  }

  /**
   * Clears the current session. The player will need to log in again after this.
   */
  @Override
  public void logout() {
    if (loggedInUser != null) {
      LOG.info("User '{}' logged out", loggedInUser.username());
    }
    loggedInUser = null;
  }

  /**
   * Returns a live view of the current session. The returned object always reflects the latest
   * login/logout state.
   */
  @Override
  public SessionContext session() {
    return new SessionContext() {
      @Override
      public PlayerProfile currentUser() {
        return loggedInUser;
      }

      @Override
      public boolean isLoggedIn() {
        return loggedInUser != null;
      }
    };
  }
}
