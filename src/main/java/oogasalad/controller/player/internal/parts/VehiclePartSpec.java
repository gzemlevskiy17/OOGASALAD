package oogasalad.controller.player.internal.parts;

import oogasalad.model.engine.internal.components.RigidBody;

/**
 * @author Paulina Vvedenskaya
 */
public interface VehiclePartSpec {

  String paletteName();

  JointStyle jointStyle();

  double bodyWidth();

  double bodyHeight();

  RigidBody create(double x, double y);
}
