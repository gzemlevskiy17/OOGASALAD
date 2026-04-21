package oogasalad.controller.accounts.internal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;
import oogasalad.controller.core.api.Outcome;
import oogasalad.model.auth.api.AuthResult;
import oogasalad.model.auth.api.AuthService;
import oogasalad.model.auth.api.PlayerProfile;
import oogasalad.view.accounts.api.AccountOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthControllerImplTest {

  private final PlayerProfile fakeProfile = mock(PlayerProfile.class);
  private final AuthResult success = new AuthResult.Success(fakeProfile);
  private final AuthResult failure = new AuthResult.Failure("bad credentials");
  private AuthService authService;
  private Consumer<Outcome> navHandler;
  private Consumer<String> errHandler;
  private AuthControllerImpl controller;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    authService = mock(AuthService.class);
    navHandler = mock(Consumer.class);
    errHandler = mock(Consumer.class);
    controller = new AuthControllerImpl(authService, navHandler, errHandler);
  }

  // ── LoginRequested ────────────────────────────────────────────────────────

  @Test
  void loginRequested_success_callsNavHandler() {
    when(authService.login("alice", "pass")).thenReturn(success);
    controller.handle(new AccountOutcome.LoginRequested("alice", "pass"));
    verify(navHandler).accept(Outcome.BACK_TO_MENU);
    verify(errHandler, never()).accept(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void loginRequested_failure_callsErrorHandlerWithReason() {
    when(authService.login("alice", "wrong")).thenReturn(failure);
    controller.handle(new AccountOutcome.LoginRequested("alice", "wrong"));
    verify(errHandler).accept("bad credentials");
    verify(navHandler, never()).accept(org.mockito.ArgumentMatchers.any());
  }

  // ── RegisterRequested ─────────────────────────────────────────────────────

  @Test
  void registerRequested_success_callsNavHandler() {
    when(authService.register("bob", "pw", "avatar.png")).thenReturn(success);
    controller.handle(new AccountOutcome.RegisterRequested("bob", "pw", "avatar.png"));
    verify(authService).register("bob", "pw", "avatar.png");
    verify(navHandler).accept(Outcome.BACK_TO_MENU);
  }

  @Test
  void registerRequested_failure_callsErrorHandler() {
    when(authService.register("bob", "pw", "")).thenReturn(failure);
    controller.handle(new AccountOutcome.RegisterRequested("bob", "pw", ""));
    verify(errHandler).accept("bad credentials");
  }

  // ── LogoutRequested ───────────────────────────────────────────────────────

  @Test
  void logoutRequested_callsAuthServiceLogout() {
    controller.handle(new AccountOutcome.LogoutRequested());
    verify(authService).logout();
  }

  @Test
  void logoutRequested_navigatesAfterLogout() {
    controller.handle(new AccountOutcome.LogoutRequested());
    verify(navHandler).accept(Outcome.BACK_TO_MENU);
  }

  // ── UpdateAvatarRequested ─────────────────────────────────────────────────

  @Test
  void updateAvatarRequested_success_navigates() {
    when(authService.updateAvatar("new.png")).thenReturn(success);
    controller.handle(new AccountOutcome.UpdateAvatarRequested("new.png"));
    verify(authService).updateAvatar("new.png");
    verify(navHandler).accept(Outcome.BACK_TO_MENU);
  }

  @Test
  void updateAvatarRequested_failure_sendsError() {
    when(authService.updateAvatar("new.png")).thenReturn(failure);
    controller.handle(new AccountOutcome.UpdateAvatarRequested("new.png"));
    verify(errHandler).accept("bad credentials");
  }

  // ── UpdateUsernameRequested ───────────────────────────────────────────────

  @Test
  void updateUsernameRequested_success_navigates() {
    when(authService.updateUsername("newname")).thenReturn(success);
    controller.handle(new AccountOutcome.UpdateUsernameRequested("newname"));
    verify(authService).updateUsername("newname");
    verify(navHandler).accept(Outcome.BACK_TO_MENU);
  }

  @Test
  void updateUsernameRequested_failure_sendsError() {
    when(authService.updateUsername("taken")).thenReturn(failure);
    controller.handle(new AccountOutcome.UpdateUsernameRequested("taken"));
    verify(errHandler).accept("bad credentials");
  }

  // ── UpdatePasswordRequested ───────────────────────────────────────────────

  @Test
  void updatePasswordRequested_success_navigates() {
    when(authService.updatePassword("newpw")).thenReturn(success);
    controller.handle(new AccountOutcome.UpdatePasswordRequested("newpw"));
    verify(authService).updatePassword("newpw");
    verify(navHandler).accept(Outcome.BACK_TO_MENU);
  }

  @Test
  void updatePasswordRequested_failure_sendsError() {
    when(authService.updatePassword("")).thenReturn(failure);
    controller.handle(new AccountOutcome.UpdatePasswordRequested(""));
    verify(errHandler).accept("bad credentials");
  }
}
