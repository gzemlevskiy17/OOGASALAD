package oogasalad.controller.core.internal;

import java.util.HashMap;
import java.util.Map;
import oogasalad.controller.core.api.ScreenManager;
import oogasalad.view.authoring.internal.AuthoringEntryScreenImpl;
import oogasalad.view.authoring.internal.AuthoringScreenImpl;
import oogasalad.view.core.api.AppScreen;
import oogasalad.view.core.api.GameMenuScreen;
import oogasalad.view.player.internal.GamePlayAppScreen;
import oogasalad.view.player.internal.PlayerEntryAppScreen;
import oogasalad.view.player.internal.VehicleBuilderAppScreen;

/**
 * Creates and collects application screens for registration with the ScreenManager.
 */
public class ScreenFactory {

  public static final String MENU_SCREEN_KEY = "MENU";
  public static final String AUTHORING_ENTRY_SCREEN_KEY = "AUTHORING_ENTRY";
  public static final String PLAYER_ENTRY_APP_SCREEN_KEY = "PLAYER_ENTRY";
  public static final String AUTHORING_SCREEN_KEY = "AUTHORING_SCREEN";
  public static final String VEHICLE_BUILDER_APP_SCREEN_KEY = "VEHICLE_BUILDER_SCREEN";
  public static final String GAMEPLAY_APP_SCREEN_KEY = "GAMEPLAY_SCREEN";

  private final ScreenManager screenManager;

  public ScreenFactory(ScreenManager screenManager) {
    this.screenManager = screenManager;
  }

  /**
   * Eagerly instantiates the current set of application screens.
   *
   * @return a map from screen key to screen instance
   */
  public Map<String, AppScreen> createScreens() {
    Map<String, AppScreen> screens = new HashMap<>();
    screens.put(MENU_SCREEN_KEY, new GameMenuScreen(screenManager::onOutcome));
    screens.put(AUTHORING_ENTRY_SCREEN_KEY, new AuthoringEntryScreenImpl(screenManager::onOutcome));
    screens.put(AUTHORING_SCREEN_KEY, new AuthoringScreenImpl(screenManager::onOutcome));
    screens.put(PLAYER_ENTRY_APP_SCREEN_KEY, new PlayerEntryAppScreen(screenManager::onOutcome));
    screens.put(VEHICLE_BUILDER_APP_SCREEN_KEY, new VehicleBuilderAppScreen(screenManager::onOutcome));
    screens.put(GAMEPLAY_APP_SCREEN_KEY, new GamePlayAppScreen(screenManager::onOutcome));
    return screens;
  }
}
