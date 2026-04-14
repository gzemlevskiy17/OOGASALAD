package oogasalad.model.engine.api;

import java.util.ResourceBundle;

public enum DefaultTag {
  GAME_OBJECT,
  DYNAMIC,
  STATIC,
  PASSTHROUGH,
  TERRAIN,
  VEHICLE,
  FINISH_LINE;

  private static final ResourceBundle TAGS =
      ResourceBundle.getBundle("oogasalad.model.engine.DefaultTag");

  public String value() {
    return TAGS.getString(name());
  }
}
