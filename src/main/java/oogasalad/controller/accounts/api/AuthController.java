package oogasalad.controller.accounts.api;

import oogasalad.view.accounts.api.AccountOutcome;

/**
 * Translates account-screen intents into authentication operations.
 * <p>
 * Sits between the view layer (account screens) and the model layer
 * ({@link oogasalad.model.auth.api.AuthService}), keeping both sides decoupled from each other.
 */
public interface AuthController {

  /**
   * Handles an intent fired by an account screen.
   */
  void handle(AccountOutcome intent);
}
