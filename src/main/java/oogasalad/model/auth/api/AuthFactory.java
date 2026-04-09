package oogasalad.model.auth.api;

import oogasalad.model.auth.internal.LocalAuthService;
import oogasalad.model.auth.internal.RepoFactory;

/**
 * Creates {@link AuthService} instances.
 * <p>
 * Use this instead of constructing services directly so the rest of the codebase stays decoupled
 * from the local SQLite implementation.
 */
public class AuthFactory {

  /**
   * Returns an AuthService backed by a local SQLite database.
   */
  public static AuthService createService() {
    return new LocalAuthService(RepoFactory.createLocalRepository());
  }
}
