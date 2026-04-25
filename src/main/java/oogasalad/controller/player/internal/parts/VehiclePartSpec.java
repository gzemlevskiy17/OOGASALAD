package oogasalad.controller.player.internal.parts;

import oogasalad.model.engine.internal.components.GameComponent;

/**
 * @author Paulina Vvedenskaya
 */
public interface VehiclePartSpec {

  String paletteName();

  JointStyle jointStyle();

  double bodyWidth();

  double bodyHeight();

  GameComponent create(double x, double y);
}
