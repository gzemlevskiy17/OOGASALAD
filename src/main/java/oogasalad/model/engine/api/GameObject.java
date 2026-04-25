package oogasalad.model.engine.api;

import java.util.Collections;
import java.util.List;
import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.ObjectState;
import oogasalad.view.core.api.Renderable;

//TODO: complete comments
/**
 * Base entity for the simulation. Exposes physical properties.
 */
public interface GameObject extends Renderable {
  String getTag();
  void setTag(String tag);

  //TODO: should these be returned immutably?
  double getMass();

  Vector getForce();

  void setForce(Vector force);

  void setMass(double mass);

  ConvexPolygon getPolygon();

  /**
   * Returns every convex polygon that participates in collision detection for this object, in
   * arbitrary order. The default implementation returns a singleton list of {@link #getPolygon()}
   * when it is non-null and an empty list otherwise, which preserves the historical single-polygon
   * contract for every existing {@link GameObject} implementation.
   *
   * <p>Multi-polygon shapes (for example a spline-based terrain consisting of a strip of slabs)
   * override this method to expose every sub-polygon. The collision detector then iterates the
   * Cartesian product of sub-polygon pairs, keeping the deepest penetrating collision per pair of
   * objects. Because the default funnels through {@link #getPolygon()}, single-polygon callers
   * continue to work unchanged.
   */
  default List<ConvexPolygon> getCollisionPolygons() {
    ConvexPolygon polygon = getPolygon();
    if (polygon == null) {
      return Collections.emptyList();
    }
    return Collections.singletonList(polygon);
  }

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

  default void updatePolygon() {}
}
