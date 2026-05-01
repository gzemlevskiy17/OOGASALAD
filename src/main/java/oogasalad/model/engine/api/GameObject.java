package oogasalad.model.engine.api;

import java.util.List;
import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.StaticState;
import oogasalad.view.core.api.Renderable;

/**
 * Core simulation contract for anything the engine can render, collide, and simulate.
 *
 * <p>Implementations depend on {@link Renderable}, {@link ConvexPolygon}, and
 * {@link StaticState} to expose their visible shape and physical state to the engine.
 */
public interface GameObject extends Renderable {

  /**
   * Returns this object's tag, which is the string used to identify or categorize it in the
   * engine.
   */
  String getTag();

  /**
   * Updates this object's tag so the engine can treat it as a different category or identifier.
   */
  void setTag(String tag);

  /**
   * Returns this object's collision polygon, which defines the shape the engine uses for collision
   * checks and rendering layout.
   */
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

  /**
   * Returns this object's static state, which stores its world position and rotation.
   */
  StaticState getStaticState();

  /**
   * Updates this object's restitution, or how bouncy it is during collisions.
   */
  void setRestitution(double restitution);

  /**
   * Returns this object's friction coefficient, which controls how strongly it resists sliding
   * along other surfaces.
   */
  double getFriction();

  /**
   * Returns whether this object is active and should participate in simulation steps.
   */
  boolean isActive();

  /**
   * Returns whether gravity should be applied to this object.
   */
  boolean isAffectedByGravity();

  /**
   * Returns whether this object behaves as a trigger, meaning it detects overlap without acting as
   * solid collision geometry.
   */
  boolean isTrigger();

  /**
   * Applies a force that contributes to this object's motion.
   */
  void addForce(Vector force);

  /**
   * Refreshes this object's polygon representation after its state changes.
   */
  default void updatePolygon() {
  }
}
