package oogasalad.controller.core.api;

/** Represents any terminal event from a screen or controller that requires navigation. */
public enum Outcome {
  // Game results
  GAME_WON("RESULT"),
  GAME_LOST("RESULT"),

  // going forwards
  OPEN_PLAYER_ENTRY("PLAYER_ENTRY"),
  OPEN_AUTHORING_ENTRY("AUTHORING_ENTRY"),
  OPEN_AUTHORING("AUTHORING_SCREEN"),
  OPEN_VEHICLE_BUILDER("VEHICLE_BUILDER_SCREEN"),
  OPEN_GAMEPLAY("GAMEPLAY_SCREEN"),
  OPEN_LOGIN("LOGIN"),
  OPEN_CREATE_ACCOUNT("CREATE_ACCOUNT"),
  OPEN_PROFILE("PROFILE"),
  OPEN_UPDATE_PROFILE("UPDATE_PROFILE"),

  // going back
  BACK_TO_MENU("MENU"),
  BACK_TO_PLAYER_ENTRY("PLAYER_ENTRY"),
  BACK_TO_VEHICLE_BUILDER("VEHICLE_BUILDER_SCREEN"),
  BACK_TO_AUTHORING_ENTRY("AUTHORING_ENTRY");

  public final String screenKey;

  Outcome(String screenKey) {
    this.screenKey = screenKey;
  }
}
