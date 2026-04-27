package oogasalad.model.engine.api;

import java.util.List;
import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.StaticState;
import oogasalad.view.core.api.Renderable;

//TODO: more detailed comments

/**
 * Base entity for the simulation. Exposes physical properties.
 */
public interface GameObject extends Renderable {

  String getTag();

  void setTag(String tag);

  ConvexPolygon getPolygon();

  /**
   * Returns the convex polygons that make up this object's collision surface. The default returns a
   * single-element list wrapping {@link #getPolygon()}; objects whose collision surface is composed
   * of multiple convex pieces (e.g. tessellated splines) override this to expose all of them. Both
   * AABB broad-phase and SAT narrow-phase iterate the union of these polygons.
   */
  default List<ConvexPolygon> getCollisionPolygons() {
    return List.of(getPolygon());
  }

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

  default void updatePolygon() {
  }
}
