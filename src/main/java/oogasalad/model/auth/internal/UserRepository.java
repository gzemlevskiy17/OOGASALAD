package oogasalad.model.auth.internal;

import java.util.Optional;

/**
 * Reads and writes user account records to persistent storage.
 * <p>
 * This interface deals with raw database records (including hashed passwords). It should only be
 * used by the auth service — never exposed to the view layer.
 */
public interface UserRepository {

  /**
   * Looks up an account by username. Returns empty if no account exists with that name.
   */
  Optional<UserRecord> findByUsername(String username);

  /**
   * Returns true if an account already exists with the given username.
   */
  boolean usernameExists(String username);

  /**
   * Saves a new account and returns the stored record. The password must already be hashed before
   * calling this — never pass a plain-text password.
   */
  UserRecord save(String username, String passwordHash, String salt, String avatarPath);

  /**
   * Updates the avatar path for an existing account.
   */
  void updateAvatarPath(String username, String avatarPath);
}
