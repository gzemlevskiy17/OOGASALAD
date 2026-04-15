package oogasalad.model.engine;

import java.util.Objects;
import oogasalad.model.engine.api.DefaultTag;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Vector;

/**
 * Mutable concrete implementation of a physics/render game object.
 *
 * <p>Use this class to store per-object physical properties (mass, inertia, restitution, force)
 * plus pose/velocity state and a collision polygon.
 *
 * <p><strong>Assumptions:</strong> Callers initialize state and polygon before using render or
 * collision methods. Mass/inertia should be positive for dynamic bodies; non-positive values are
 * treated as immovable by parts of the engine.
 *
 * <p><strong>Dependencies:</strong> Implements {@link GameObject} and uses {@link ObjectState},
 * {@link ConvexPolygon}, and {@link Vector}.
 *
 * <p><strong>Example:</strong>
 *
 * <pre>{@code
 * GameObjectImpl body = new GameObjectImpl();
 * body.setMass(2.0);
 * body.setMomentOfInertia(1.5);
 * body.setRestitution(0.2);
 * body.setState(new ObjectState(pos, vel, new Vector(0, 0), 0.0, 0.0, 0.0));
 * }</pre>
 *
 * @author Georgiy Zemlevskiy
 */
public class GameObjectImpl implements GameObject {

  private double mass;
  private double momentOfInertia;
  private double restitution;

  //TODO: move to interface?
  private ObjectState state;
  private ConvexPolygon polygon;

  private Vector force;
  private String tag = DefaultTag.GAME_OBJECT.value();

  @Override
  public String getTag() {return tag;}

  @Override
  public void setTag(String tag) {
    this.tag = Objects.requireNonNull(tag);
  }

  @Override
  public double getMass() {
    return mass;
  }

  @Override
  public Vector getForce() {
    return force;
  }

  @Override
  public void setMass(double mass) {
    this.mass = mass;
  }

  @Override
  public void setForce(Vector force) {
    this.force = force;
  }

  @Override
  public ConvexPolygon getPolygon() {
    return polygon;
  }

  @Override
  public ObjectState getState() {
    return state;
  }

  @Override
  public void setState(ObjectState state) {
    this.state = state;
  }

  @Override
  public double getMomentOfInertia() {
    return momentOfInertia;
  }

  @Override
  public void setMomentOfInertia(double momentOfInertia) {
    this.momentOfInertia = momentOfInertia;
  }

  @Override
  public double getRestitution() {
    return restitution;
  }

  @Override
  public void setRestitution(double restitution) {
    this.restitution = restitution;
  }


  //TODO: deal with new interface methods
  @Override
  public double getFriction() {
    return 0;
  }

  @Override
  public boolean isActive() {
    return false;
  }

  @Override
  public boolean isAffectedByGravity() {
    return false;
  }

  @Override
  public boolean isTrigger() {
    return false;
  }

  @Override
  public void addForce(Vector force) {

  }

  @Override
  public double getX() {
    return state.getLinearPosition().x();
  }

  @Override
  public double getY() {
    return state.getLinearPosition().y();
  }

  @Override
  public double getRotation() {
    return 0;
  }
}
