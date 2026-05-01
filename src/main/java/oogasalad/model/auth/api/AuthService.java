package oogasalad.model.auth.api;

/**
 * The main entry point for all authentication and account management operations.
 * <p>
 * Call {@link #login} or {@link #register} to start a session, then use {@link #session} to read
 * the currently logged-in player. Call {@link #logout} to end the session.
 *
 * @author Paulina Vvedenskaya
 */
public interface AuthService {

  /** Attempts to log in with the given username and password. */
  AuthResult login(String username, String password);

  /** Creates a new account and logs the player in immediately on success. */
  AuthResult register(String username, String password, String avatarPath);

  /** Changes the avatar for the currently logged-in player. */
  AuthResult updateAvatar(String newAvatarPath);

  /**
   * Changes the username for the currently logged-in player.
   */
  AuthResult updateUsername(String newUsername);

  /**
   * Changes the password for the currently logged-in player.
   */
  AuthResult updatePassword(String newPassword);

  /**
   * Ends the current session. The player will need to log in again.
   */
  void logout();

  /**
   * Returns a live view of who is currently logged in (if anyone).
   */
  SessionContext session();
}
