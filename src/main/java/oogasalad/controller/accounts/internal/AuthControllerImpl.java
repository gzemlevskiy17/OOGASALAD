package oogasalad.controller.accounts.internal;

import java.util.function.Consumer;
import oogasalad.controller.accounts.api.AuthController;
import oogasalad.controller.core.api.Outcome;
import oogasalad.model.auth.api.AuthResult;
import oogasalad.model.auth.api.AuthService;
import oogasalad.view.accounts.api.AccountOutcome;

/**
 * Routes account-screen intents through the authentication service and dispatches navigation or
 * error callbacks based on the result.
 *
 * @author Paulina Vvedenskaya
 */
public class AuthControllerImpl implements AuthController {

  private final AuthService authService;
  private final Consumer<Outcome> navigationHandler;
  private final Consumer<String> errorHandler;

  public AuthControllerImpl(AuthService authService,
      Consumer<Outcome> navHandler,
      Consumer<String> errHandler) {
    this.authService = authService;
    this.navigationHandler = navHandler;
    this.errorHandler = errHandler;
  }

  @Override
  public void handle(AccountOutcome intent) {
    AuthResult result = switch (intent) {
      case AccountOutcome.LoginRequested r -> authService.login(r.user(), r.pass());
      case AccountOutcome.RegisterRequested r ->
          authService.register(r.user(), r.pass(), r.avatar());
      case AccountOutcome.UpdateAvatarRequested r -> authService.updateAvatar(r.path());
      case AccountOutcome.UpdateUsernameRequested r -> authService.updateUsername(r.newUsername());
      case AccountOutcome.UpdatePasswordRequested r -> authService.updatePassword(r.newPassword());
      case AccountOutcome.LogoutRequested r -> {
        authService.logout();
        yield new AuthResult.Success(null);
      }
    };

    processResult(result);
  }

  private void processResult(AuthResult result) {
    if (result instanceof AuthResult.Success) {
      navigationHandler.accept(Outcome.BACK_TO_MENU);
    } else if (result instanceof AuthResult.Failure(String reason)) {
      errorHandler.accept(reason);
    }
  }
}
