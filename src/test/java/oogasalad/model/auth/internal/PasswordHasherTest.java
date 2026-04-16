package oogasalad.model.auth.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordHasherTest {

  // ── generateSalt ──────────────────────────────────────────────────────────

  @Test
  void generateSalt_returnsNonNull() {
    assertNotNull(PasswordHasher.generateSalt());
  }

  @Test
  void generateSalt_returnsHexStringOf32Chars() {
    // 16 bytes → 32 hex characters
    String salt = PasswordHasher.generateSalt();
    assertEquals(32, salt.length(), "Expected 32-char hex string for 16 bytes");
    assertTrue(salt.matches("[0-9a-f]+"), "Salt should be lowercase hex");
  }

  @Test
  void generateSalt_producesUniqueValues() {
    // Two consecutive salts must not be equal (collision probability is negligible)
    String salt1 = PasswordHasher.generateSalt();
    String salt2 = PasswordHasher.generateSalt();
    assertNotEquals(salt1, salt2, "Two generated salts should not be equal");
  }

  // ── hash ─────────────────────────────────────────────────────────────────

  @Test
  void hash_isDeterministic() {
    String salt = "deadbeef00112233deadbeef00112233";
    String h1 = PasswordHasher.hash("password", salt);
    String h2 = PasswordHasher.hash("password", salt);
    assertEquals(h1, h2, "Same password + salt must always produce the same hash");
  }

  @Test
  void hash_returnsHexStringOf64Chars() {
    // SHA-256 produces 32 bytes → 64 hex characters
    String hash = PasswordHasher.hash("password", PasswordHasher.generateSalt());
    assertEquals(64, hash.length());
    assertTrue(hash.matches("[0-9a-f]+"), "Hash should be lowercase hex");
  }

  @Test
  void hash_differentPasswordsProduceDifferentHashes() {
    String salt = PasswordHasher.generateSalt();
    String h1 = PasswordHasher.hash("password1", salt);
    String h2 = PasswordHasher.hash("password2", salt);
    assertNotEquals(h1, h2);
  }

  @Test
  void hash_differentSaltsProduceDifferentHashes() {
    String h1 = PasswordHasher.hash("password", "salt1111111111111111111111111111");
    String h2 = PasswordHasher.hash("password", "salt2222222222222222222222222222");
    assertNotEquals(h1, h2, "Same password with different salts must not collide");
  }

  @Test
  void hash_isCaseSensitiveOnPassword() {
    String salt = PasswordHasher.generateSalt();
    assertNotEquals(PasswordHasher.hash("Secret", salt), PasswordHasher.hash("secret", salt));
  }

  // ── matches ───────────────────────────────────────────────────────────────

  @Test
  void matches_correctPassword_returnsTrue() {
    String salt = PasswordHasher.generateSalt();
    String hash = PasswordHasher.hash("mypassword", salt);
    assertTrue(PasswordHasher.matches("mypassword", hash, salt));
  }

  @Test
  void matches_wrongPassword_returnsFalse() {
    String salt = PasswordHasher.generateSalt();
    String hash = PasswordHasher.hash("mypassword", salt);
    assertFalse(PasswordHasher.matches("wrongpassword", hash, salt));
  }

  @Test
  void matches_wrongSalt_returnsFalse() {
    String salt1 = PasswordHasher.generateSalt();
    String salt2 = PasswordHasher.generateSalt();
    String hash = PasswordHasher.hash("mypassword", salt1);
    assertFalse(PasswordHasher.matches("mypassword", hash, salt2));
  }

  @Test
  void matches_isCaseSensitive() {
    String salt = PasswordHasher.generateSalt();
    String hash = PasswordHasher.hash("Secret", salt);
    assertFalse(PasswordHasher.matches("secret", hash, salt));
  }

  @Test
  void matches_roundTripAfterRegisterAndLogin() {
    // Simulate what LocalAuthService does end-to-end
    String password = "hunter2";
    String salt = PasswordHasher.generateSalt();
    String stored = PasswordHasher.hash(password, salt);
    assertTrue(PasswordHasher.matches(password, stored, salt));
    assertFalse(PasswordHasher.matches("hunter3", stored, salt));
  }
}
