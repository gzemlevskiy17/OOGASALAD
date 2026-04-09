package oogasalad.view.accounts.api;

/**
 * Represents user actions taken on account screens.
 */
public interface AccountOutcome {

  record LoginRequested(String user, String pass) implements AccountOutcome {

  }

  record RegisterRequested(String user, String pass, String avatar) implements AccountOutcome {

  }

  record LogoutRequested() implements AccountOutcome {

  }

  record UpdateAvatarRequested(String path) implements AccountOutcome {

  }
}
