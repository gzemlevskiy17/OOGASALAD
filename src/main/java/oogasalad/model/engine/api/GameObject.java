package oogasalad.model.engine.api;

import java.util.Collection;
import oogasalad.view.core.api.Renderable; // Extends the view contract

/** Base entity for the simulation. Exposes physical properties. */
public interface GameObject extends Renderable {
  double getMass();
  Vector getVelocity();

  /** @return A collection of shapes/bounds used for physical interaction. */
  Collection<Object> getHitboxes();

  Pose getPose();
  void addForce(Vector force);
  Vector getForce();
}
