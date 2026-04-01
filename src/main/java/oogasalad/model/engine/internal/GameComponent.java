package oogasalad.model.engine.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import oogasalad.model.engine.EngineError;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Pose;
import oogasalad.model.engine.api.Vector;

public abstract class GameComponent implements GameObject {
  private Pose pose;
  private Vector velocity;
  private double mass;
  private double friction;
  private final List<Object> hitboxes;
  private Vector force;
  private boolean active;
  private boolean affectedByGravity;
  private boolean trigger;

  public GameComponent(Pose pose, double mass, double friction) {
    this.pose = Objects.requireNonNull(pose, EngineError.NULL_POSE.message());
    setMass(mass);
    setFriction(friction);
    this.velocity = new Vector(0, 0);
    this.force = new Vector(0, 0);
    this.hitboxes = new ArrayList<>();
    this.active = true;
    this.affectedByGravity = true;
    this.trigger = false;
  }

  @Override
  public Pose getPose() {
    return pose;
  }

  public void setPosition(Pose position) {
    this.pose = Objects.requireNonNull(position, EngineError.NULL_POSE.message());
  }

  @Override
  public Vector getVelocity() {
    return velocity;
  }

  public void setVelocity(Vector velocity) {
    this.velocity = Objects.requireNonNull(velocity, EngineError.NULL_VECTOR.message());
  }

  @Override
  public double getMass() {
    return mass;
  }

  public void setMass(double mass) {
    if (mass < 0) {
      throw new IllegalArgumentException(EngineError.MASS_MUST_BE_NON_NEGATIVE.message());
    }
    this.mass = mass;
  }

  @Override
  public double getFriction() {
    return friction;
  }

  public void setFriction(double friction) {
    if (friction < 0) {
      throw new IllegalArgumentException(EngineError.FRICTION_MUST_BE_NON_NEGATIVE.message());
    }
    this.friction = friction;
  }

  @Override
  public List<Object> getHitboxes() {
    return Collections.unmodifiableList(hitboxes);
  }

  protected void addHitbox(Object hitbox) {
    hitboxes.add(Objects.requireNonNull(hitbox, EngineError.NULL_HITBOX.message()));
  }

  protected void removeHitbox(Object hitbox) {
    hitboxes.remove(hitbox);
  }

  protected void clearHitboxes() {
    hitboxes.clear();
  }

  @Override
  public Vector getForce() {
    return force;
  }

  @Override
  public void addForce(Vector force) {
    this.force = this.force.add(Objects.requireNonNull(force, EngineError.NULL_VECTOR.message()));
  }

  @Override
  public boolean isActive() {
    return active;
  }

  @Override
  public boolean isAffectedByGravity() {
    return affectedByGravity;
  }

  @Override
  public boolean isTrigger() {
    return trigger;
  }

  public void activate() {
    active = true;
  }

  public void deactivate() {
    active = false;
  }

  public void destroy() {
    deactivate();
  }

  public void setAffectedByGravity(boolean affectedByGravity) {
    this.affectedByGravity = affectedByGravity;
  }

  protected void setTrigger(boolean trigger) {
    this.trigger = trigger;
  }

  protected void resetForce() {
    this.force = new Vector(0, 0);
  }
}
