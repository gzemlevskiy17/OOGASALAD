package oogasalad.model.engine.api;

import java.util.Collection;
import oogasalad.view.core.api.Renderable; // Extends the view contract

/** Base entity for the simulation. Exposes physical properties. */
public interface GameObject extends Renderable {
  //TODO: should these be returned immutably?
  double getMass();
  Vector getPosition();
  Vector getVelocity();
  Vector getForce();

  void setMass();
  void setPosition(Vector position);
  void setVelocity(Vector velocity);
  void setForce(Vector force);
  void updateVelocity(double elapsedTime);

  void move(double elapsedTime);

  /** @return A collection of shapes/bounds used for physical interaction. */
  Collection<Object> getHitboxes();

}
