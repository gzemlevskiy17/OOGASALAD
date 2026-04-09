package oogasalad.view.accounts.api;

/**
 * Represents user actions taken on account screens.
 */
public sealed interface AccountOutcome
    permits AccountOutcome.LoginRequested, AccountOutcome.RegisterRequested,
    AccountOutcome.LogoutRequested, AccountOutcome.UpdateAvatarRequested {

  record LoginRequested(String user, String pass) implements AccountOutcome {

  }

  record RegisterRequested(String user, String pass, String avatar) implements AccountOutcome {

  }

  record LogoutRequested() implements AccountOutcome {

  }

  record UpdateAvatarRequested(String path) implements AccountOutcome {

  }
}
