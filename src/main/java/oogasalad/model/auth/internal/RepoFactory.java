package oogasalad.model.auth.internal;

/**
 * Creates {@link UserRepository} instances.
 *
 * Use this instead of constructing repositories directly so the rest of the
 * codebase stays decoupled from the SQLite implementation.
 */
public class RepoFactory {

  /** Returns a repository that stores accounts in a local SQLite database. */
  public static UserRepository createLocalRepository() {
    return new SqliteUserRepo();
  }
}
