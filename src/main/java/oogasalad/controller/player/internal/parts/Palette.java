package oogasalad.controller.player.internal.parts;

import java.util.ResourceBundle;

/**
 * @author Paulina Vvedenskaya
 */
final class Palette {

  private static final ResourceBundle BUNDLE =
      ResourceBundle.getBundle("oogasalad.view.authoring.VehicleComponentPalette");

  private static final double DEFAULT_DIMENSION = 40.0;

  private Palette() {
  }

  static double width(String paletteName) {
    return dimension(paletteName, "width");
  }

  static double height(String paletteName) {
    return dimension(paletteName, "height");
  }

  private static double dimension(String paletteName, String axis) {
    String key = paletteName.replace(" ", "") + "." + axis;
    if (BUNDLE.containsKey(key)) {
      return Double.parseDouble(BUNDLE.getString(key));
    }
    return DEFAULT_DIMENSION;
  }
}
