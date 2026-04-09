package oogasalad.model.auth.internal;

import java.util.Optional;
import oogasalad.model.auth.api.AuthResult;
import oogasalad.model.auth.api.AuthService;
import oogasalad.model.auth.api.PlayerProfile;
import oogasalad.model.auth.api.SessionContext;
import oogasalad.model.auth.api.UserRecord;
import oogasalad.model.auth.api.UserRepository;

/**
 * Authenticates players against a local SQLite database.
 *
 * Delegates password hashing to {@link PasswordHasher} and storage to
 * {@link UserRepository}, keeping this class focused on login/register logic only.
 */
public class LocalAuthService implements AuthService {

  private final UserRepository repo;
  private PlayerProfile loggedInUser;

  /** Creates a service that reads and writes accounts through the given repository. */
  public LocalAuthService(UserRepository repo) {
    this.repo = repo;
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
    if (record.isEmpty() || !PasswordHasher.matches(password, record.get().passwordHash(), record.get().salt())) {
      return new AuthResult.Failure("Invalid username or password");
    }
    loggedInUser = toProfile(record.get());
    return new AuthResult.Success(loggedInUser);
  }

  /**
   * Creates a new account with the given username, password, and optional avatar path.
   * The password is salted and hashed before storage — it is never saved in plain text.
   * Returns a failure if the username is already taken or if any required field is blank.
   */
  @Override
  public AuthResult register(String username, String password, String avatarPath) {
    if (isBlank(username)) return new AuthResult.Failure("Username is required");
    if (isBlank(password)) return new AuthResult.Failure("Password is required");
    if (repo.usernameExists(username.trim())) return new AuthResult.Failure("Username already taken");

    String salt = PasswordHasher.generateSalt();
    String hash = PasswordHasher.hash(password, salt);
    String path = avatarPath == null ? "" : avatarPath;
    UserRecord saved = repo.save(username.trim(), hash, salt, path);
    loggedInUser = toProfile(saved);
    return new AuthResult.Success(loggedInUser);
  }

  /**
   * Updates the avatar for whoever is currently logged in.
   * Returns a failure if no player is logged in.
   */
  @Override
  public AuthResult updateAvatar(String newAvatarPath) {
    if (loggedInUser == null) return new AuthResult.Failure("Not logged in");
    String path = newAvatarPath == null ? "" : newAvatarPath;
    repo.updateAvatarPath(loggedInUser.username(), path);
    loggedInUser = new ProfileRecord(loggedInUser.username(), path);
    return new AuthResult.Success(loggedInUser);
  }

  /**
   * Clears the current session. The player will need to log in again after this.
   */
  @Override
  public void logout() {
    loggedInUser = null;
  }

  /**
   * Returns a live view of the current session.
   * The returned object always reflects the latest login/logout state.
   */
  @Override
  public SessionContext session() {
    return new SessionContext() {
      @Override public PlayerProfile currentUser() { return loggedInUser; }
      @Override public boolean isLoggedIn() { return loggedInUser != null; }
    };
  }

  private static ProfileRecord toProfile(UserRecord r) {
    return new ProfileRecord(r.username(), r.avatarPath());
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }
}
