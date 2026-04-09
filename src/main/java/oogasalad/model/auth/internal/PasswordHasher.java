package oogasalad.model.auth.internal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Handles password hashing and verification.
 * <p>
 * Each password is combined with a unique random salt before hashing, so two users with the same
 * password will always produce different stored hashes. Passwords are hashed with SHA-256 and are
 * never stored in plain text.
 */
class PasswordHasher {

  private static final int SALT_BYTES = 16;

  private PasswordHasher() {
  }

  /**
   * Generates a fresh random salt. Call this once when creating a new account and store the result
   * alongside the hashed password.
   */
  static String generateSalt() {
    byte[] bytes = new byte[SALT_BYTES];
    new SecureRandom().nextBytes(bytes);
    return HexFormat.of().formatHex(bytes);
  }

  /**
   * Hashes a password together with its salt. Store this result in the database — never store the
   * raw password.
   */
  static String hash(String password, String salt) {
    try {
      byte[] bytes = MessageDigest.getInstance("SHA-256")
          .digest((salt + password).getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(bytes);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 is unavailable on this JVM", e);
    }
  }

  /**
   * Returns true if the given password, when hashed with the stored salt, matches the stored hash.
   * Use this to verify a login attempt.
   */
  static boolean matches(String password, String storedHash, String salt) {
    return hash(password, salt).equals(storedHash);
  }
}
