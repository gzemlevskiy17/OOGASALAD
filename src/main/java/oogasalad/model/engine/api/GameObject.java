package oogasalad.model.engine.api;

import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.ObjectState;
import oogasalad.view.core.api.Renderable; // Extends the view contract

//TODO: complete comments
/**
 * Base entity for the simulation. Exposes physical properties.
 */
public interface GameObject extends Renderable {
  //TODO: should these be returned immutably?
  double getMass();

  Vector getForce();

  void setForce(Vector force);

  void setMass(double mass);

  ConvexPolygon getPolygon();

  ObjectState getState();

  void setState(ObjectState state);

  double getMomentOfInertia();

  void setMomentOfInertia(double momentOfInertia);

  double getRestitution();

  void setRestitution(double restitution);

  double getFriction();
  boolean isActive();
  boolean isAffectedByGravity();
  boolean isTrigger();

  void addForce(Vector force);
}
