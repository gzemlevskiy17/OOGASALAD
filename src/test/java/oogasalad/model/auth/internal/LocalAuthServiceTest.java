package oogasalad.model.auth.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import oogasalad.model.auth.api.AuthResult;
import oogasalad.model.auth.api.SessionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LocalAuthServiceTest {

  private FakeRepo repo;
  private LocalAuthService service;

  @BeforeEach
  void setUp() {
    repo = new FakeRepo();
    service = new LocalAuthService(repo);
  }

  @Test
  void login_blankUsername_returnsFailure() {
    AuthResult result = service.login("  ", "password");
    assertInstanceOf(AuthResult.Failure.class, result);
  }

  // ── login ────────────────────────────────────────────────────────────────

  @Test
  void login_blankPassword_returnsFailure() {
    AuthResult result = service.login("alice", "");
    assertInstanceOf(AuthResult.Failure.class, result);
  }

  @Test
  void login_nullUsername_returnsFailure() {
    AuthResult result = service.login(null, "password");
    assertInstanceOf(AuthResult.Failure.class, result);
  }

  @Test
  void login_userNotFound_returnsFailure() {
    AuthResult result = service.login("nobody", "password");
    assertInstanceOf(AuthResult.Failure.class, result);
    assertEquals("Invalid username or password", ((AuthResult.Failure) result).reason());
  }

  @Test
  void login_wrongPassword_returnsFailure() {
    repo.seed("alice", "correct", "");
    AuthResult result = service.login("alice", "wrong");
    assertInstanceOf(AuthResult.Failure.class, result);
  }

  @Test
  void login_correctCredentials_returnsSuccessWithProfile() {
    repo.seed("alice", "secret", "avatar.png");
    AuthResult result = service.login("alice", "secret");
    assertInstanceOf(AuthResult.Success.class, result);
    assertEquals("alice", ((AuthResult.Success) result).profile().username());
  }

  @Test
  void login_trimsUsernameBeforeLookup() {
    repo.seed("alice", "secret", "");
    // "  alice  " should match the stored "alice"
    AuthResult result = service.login("  alice  ", "secret");
    assertInstanceOf(AuthResult.Success.class, result);
  }

  @Test
  void login_updatesSessionOnSuccess() {
    repo.seed("alice", "secret", "");
    assertFalse(service.session().isLoggedIn());
    service.login("alice", "secret");
    assertTrue(service.session().isLoggedIn());
    assertEquals("alice", service.session().currentUser().username());
  }

  @Test
  void register_blankUsername_returnsFailure() {
    AuthResult result = service.register("", "password", "");
    assertInstanceOf(AuthResult.Failure.class, result);
    assertEquals("Username is required", ((AuthResult.Failure) result).reason());
  }

  // ── register ─────────────────────────────────────────────────────────────

  @Test
  void register_blankPassword_returnsFailure() {
    AuthResult result = service.register("alice", "   ", "");
    assertInstanceOf(AuthResult.Failure.class, result);
    assertEquals("Password is required", ((AuthResult.Failure) result).reason());
  }

  @Test
  void register_duplicateUsername_returnsFailure() {
    repo.seed("alice", "pw", "");
    AuthResult result = service.register("alice", "newpw", "");
    assertInstanceOf(AuthResult.Failure.class, result);
    assertEquals("Username already taken", ((AuthResult.Failure) result).reason());
  }

  @Test
  void register_validCredentials_returnsSuccessAndStoresUser() {
    AuthResult result = service.register("bob", "pass123", "avatar.png");
    assertInstanceOf(AuthResult.Success.class, result);
    assertEquals("bob", ((AuthResult.Success) result).profile().username());
    assertEquals(1, repo.saveCount);
    assertTrue(repo.usernameExists("bob"));
  }

  @Test
  void register_nullAvatarPath_storedAsEmptyString() {
    service.register("carol", "pass", null);
    UserRecord stored = repo.findByUsername("carol").orElseThrow();
    assertEquals("", stored.avatarPath());
  }

  @Test
  void register_trimsUsername() {
    service.register("  dave  ", "pass", "");
    assertTrue(repo.usernameExists("dave"));
    assertFalse(repo.usernameExists("  dave  "));
  }

  @Test
  void register_passwordIsNotStoredInPlainText() {
    service.register("eve", "mypassword", "");
    UserRecord stored = repo.findByUsername("eve").orElseThrow();
    // The stored hash must not equal the raw password
    assertNotEquals("mypassword", stored.passwordHash());
  }

  @Test
  void register_setsSessionOnSuccess() {
    service.register("frank", "pass", "");
    assertTrue(service.session().isLoggedIn());
  }

  @Test
  void updateAvatar_notLoggedIn_returnsFailure() {
    AuthResult result = service.updateAvatar("new.png");
    assertInstanceOf(AuthResult.Failure.class, result);
    assertEquals("Not logged in", ((AuthResult.Failure) result).reason());
  }

  // ── updateAvatar ─────────────────────────────────────────────────────────

  @Test
  void updateAvatar_loggedIn_updatesProfile() {
    repo.seed("grace", "pw", "old.png");
    service.login("grace", "pw");
    AuthResult result = service.updateAvatar("new.png");
    assertInstanceOf(AuthResult.Success.class, result);
    assertEquals("new.png", ((AuthResult.Success) result).profile().avatarPath());
    assertEquals(1, repo.updateAvatarCount);
  }

  @Test
  void updateAvatar_nullPath_storedAsEmpty() {
    repo.seed("grace", "pw", "old.png");
    service.login("grace", "pw");
    service.updateAvatar(null);
    assertEquals("", service.session().currentUser().avatarPath());
  }

  @Test
  void updateUsername_notLoggedIn_returnsFailure() {
    assertInstanceOf(AuthResult.Failure.class, service.updateUsername("new"));
  }

  // ── updateUsername ────────────────────────────────────────────────────────

  @Test
  void updateUsername_blankName_returnsFailure() {
    repo.seed("helen", "pw", "");
    service.login("helen", "pw");
    AuthResult result = service.updateUsername("  ");
    assertInstanceOf(AuthResult.Failure.class, result);
    assertEquals("Username is required", ((AuthResult.Failure) result).reason());
  }

  @Test
  void updateUsername_sameName_returnsSuccessWithoutRepoCall() {
    repo.seed("helen", "pw", "");
    service.login("helen", "pw");
    AuthResult result = service.updateUsername("helen");
    assertInstanceOf(AuthResult.Success.class, result);
    assertEquals(0, repo.updateUsernameCount);
  }

  @Test
  void updateUsername_takenName_returnsFailure() {
    repo.seed("helen", "pw", "");
    repo.seed("ivan", "pw2", "");
    service.login("helen", "pw");
    AuthResult result = service.updateUsername("ivan");
    assertInstanceOf(AuthResult.Failure.class, result);
    assertEquals("Username already taken", ((AuthResult.Failure) result).reason());
  }

  @Test
  void updateUsername_validChange_updatesSession() {
    repo.seed("helen", "pw", "");
    service.login("helen", "pw");
    service.updateUsername("newname");
    assertEquals("newname", service.session().currentUser().username());
    assertEquals(1, repo.updateUsernameCount);
  }

  @Test
  void updatePassword_notLoggedIn_returnsFailure() {
    assertInstanceOf(AuthResult.Failure.class, service.updatePassword("new"));
  }

  // ── updatePassword ────────────────────────────────────────────────────────

  @Test
  void updatePassword_blankPassword_returnsFailure() {
    repo.seed("jack", "pw", "");
    service.login("jack", "pw");
    AuthResult result = service.updatePassword("");
    assertInstanceOf(AuthResult.Failure.class, result);
    assertEquals("Password is required", ((AuthResult.Failure) result).reason());
  }

  @Test
  void updatePassword_validNewPassword_hashIsUpdatedInRepo() {
    repo.seed("jack", "oldpass", "");
    service.login("jack", "oldpass");
    service.updatePassword("newpass");
    assertEquals(1, repo.updatePasswordCount);
    // The new stored hash should verify against the new password
    UserRecord stored = repo.findByUsername("jack").orElseThrow();
    assertTrue(PasswordHasher.matches("newpass", stored.passwordHash(), stored.salt()));
    assertFalse(PasswordHasher.matches("oldpass", stored.passwordHash(), stored.salt()));
  }

  @Test
  void logout_clearsSession() {
    repo.seed("kate", "pw", "");
    service.login("kate", "pw");
    assertTrue(service.session().isLoggedIn());
    service.logout();
    assertFalse(service.session().isLoggedIn());
  }

  // ── logout ────────────────────────────────────────────────────────────────

  @Test
  void session_isLiveViewOfLoginState() {
    repo.seed("leo", "pw", "");
    SessionContext ctx = service.session();
    assertFalse(ctx.isLoggedIn());
    service.login("leo", "pw");
    // same session object now reflects the logged-in state
    assertTrue(ctx.isLoggedIn());
    service.logout();
    assertFalse(ctx.isLoggedIn());
  }

  // ── session ────────────────────────────────────────────────────────────────

  /**
   * Minimal in-memory UserRepository used across all tests. Stores salted-hashed passwords so
   * LocalAuthService's password verification runs for real.
   */
  private static class FakeRepo implements UserRepository {

    private final Map<String, UserRecord> store = new HashMap<>();
    private int saveCount = 0;
    private int updateAvatarCount = 0;
    private int updateUsernameCount = 0;
    private int updatePasswordCount = 0;

    void seed(String username, String plainPassword, String avatarPath) {
      String salt = PasswordHasher.generateSalt();
      String hash = PasswordHasher.hash(plainPassword, salt);
      store.put(username, new UserRecord("id-" + username, username, hash, salt, avatarPath));
    }

    @Override
    public Optional<UserRecord> findByUsername(String username) {
      return Optional.ofNullable(store.get(username));
    }

    @Override
    public boolean usernameExists(String username) {
      return store.containsKey(username);
    }

    @Override
    public UserRecord save(String username, String passwordHash, String salt, String avatarPath) {
      saveCount++;
      UserRecord r = new UserRecord("id-" + username, username, passwordHash, salt, avatarPath);
      store.put(username, r);
      return r;
    }

    @Override
    public void updateAvatarPath(String username, String avatarPath) {
      updateAvatarCount++;
      UserRecord old = store.get(username);
      store.put(username,
          new UserRecord(old.id(), old.username(), old.passwordHash(), old.salt(), avatarPath));
    }

    @Override
    public void updateUsername(String oldUsername, String newUsername) {
      updateUsernameCount++;
      UserRecord old = store.remove(oldUsername);
      store.put(newUsername,
          new UserRecord(old.id(), newUsername, old.passwordHash(), old.salt(), old.avatarPath()));
    }

    @Override
    public void updatePassword(String username, String passwordHash, String salt) {
      updatePasswordCount++;
      UserRecord old = store.get(username);
      store.put(username,
          new UserRecord(old.id(), old.username(), passwordHash, salt, old.avatarPath()));
    }
  }
}
