package oogasalad.model.engine.api;

import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.StaticState;
import oogasalad.view.core.api.Renderable;

//TODO: is the interface needed?
//TODO: more detailed comments

/**
 * Base entity for the simulation. Exposes physical properties.
 */
public interface GameObject extends Renderable {

  String getTag();

  void setTag(String tag);

  ConvexPolygon getPolygon();

  StaticState getStaticState();

  void setRestitution(double restitution);

  double getFriction();
  boolean isActive();
  boolean isAffectedByGravity();
  boolean isTrigger();

  default int getCollisionCategoryBits() {
    return 0x0001;
  }

  default int getCollisionMaskBits() {
    return 0xFFFF_FFFF;
  }

  default void setCollisionFilter(int categoryBits, int maskBits) {
  }

  void addForce(Vector force);

  default void updatePolygon() {}
}
