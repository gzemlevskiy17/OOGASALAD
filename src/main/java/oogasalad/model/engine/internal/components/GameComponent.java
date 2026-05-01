package oogasalad.model.engine.internal.components;

import java.util.Objects;
import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.StaticState;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Vector;

/**
 * Base implementation of {@link GameObject} that stores shared state like position, tag, and
 * collision shape.
 *
 * <p>Assumes callers provide valid positions, rotations, and collision geometry; this class does
 * not validate them.
 *
 * <p>Depends on {@link StaticState} for world transform storage, {@link ConvexPolygon} for collision
 * shape, and {@link Vector} for position updates.
 *
 * <p>Example:
 *
 * <pre>
 * class Crate extends GameComponent {
 *   Crate() {
 *     super(0, 0, 0, cratePolygon, "crate");
 *   }
 * }
 * </pre>
 *
 * @author Xuye Chen
 * @author Georgiy Zemlevskiy
 */
public abstract class GameComponent implements GameObject {

  private StaticState staticState;
  private ConvexPolygon polygon;
  private boolean active;
  private String tag;

  /**
   *
   * @param x
   * @param y
   * @param rotation
   */
  public GameComponent(double x, double y, double rotation, ConvexPolygon polygon, String tag) {
    this.staticState = new StaticState(new Vector(x, y), rotation);
    this.polygon = polygon;
    this.tag = tag;
    this.active = true;
  }

  /**
   * Updates the object's position and rotation in world coordinates.
   */
  public void setPosition(double x, double y, double rotation) {
    staticState.setLinearPosition(new Vector(x, y));
    staticState.setAngularPosition(rotation);
  }

  @Override
  public String getTag() {
    return tag;
  }

  @Override
  public void setTag(String tag) {
    this.tag = Objects.requireNonNull(tag);
  }

  @Override
  public ConvexPolygon getPolygon() {
    return polygon;
  }

  protected void setPolygon(ConvexPolygon polygon) {
    this.polygon = polygon;
  }

  @Override
  public StaticState getStaticState() {
    return staticState;
  }

  public boolean isActive() {
    return active;
  }

  public void activate() {
    active = true;
  }

  public void deactivate() {
    active = false;
  }

  /**
   * Deactivates this component as part of destruction.
   */
  public void destroy() {
    deactivate();
  }

  /**
   * Returns the x-coordinate of this component's position.
   */
  @Override
  public double x() {
    return staticState.getLinearPosition().x();
  }

  /**
   * Returns the y-coordinate of this component's position.
   */
  @Override
  public double y() {
    return staticState.getLinearPosition().y();
  }

  /**
   * Returns this component's rotation in world coordinates.
   */
  @Override
  public double rotation() {
    return staticState.getAngularPosition();
  }
}
