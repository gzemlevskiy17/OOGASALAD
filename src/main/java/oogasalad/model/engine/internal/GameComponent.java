package oogasalad.model.engine.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Pose;
import oogasalad.model.engine.api.Vector;

public abstract class GameComponent implements GameObject {
  private Pose pose;
  private Vector velocity;
  private double mass;
  private final List<Object> hitboxes;
  private Vector force;

  public GameComponent(Pose pose, double mass) {
    this.pose = Objects.requireNonNull(pose);
    setMass(mass);
    this.velocity = new Vector(0, 0);
    this.force = new Vector(0, 0);
    this.hitboxes = new ArrayList<>();
  }

  @Override
  public Pose getPose() {
    return pose;
  }

  public void setPosition(Pose position) {
    this.pose = Objects.requireNonNull(position);
  }

  @Override
  public Vector getVelocity() {
    return velocity;
  }

  public void setVelocity(Vector velocity) {
    this.velocity = Objects.requireNonNull(velocity);
  }

  @Override
  public double getMass() {
    return mass;
  }

  public void setMass(double mass) {
    if (mass < 0) {
      throw new IllegalArgumentException("mass must be non-negative");
    }
    this.mass = mass;
  }

  @Override
  public List<Object> getHitboxes() {
    return Collections.unmodifiableList(hitboxes);
  }

  protected void addHitbox(Object hitbox) {
    hitboxes.add(Objects.requireNonNull(hitbox));
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
    this.force = this.force.add(Objects.requireNonNull(force));
  }

  protected void resetForce() {
    this.force = new Vector(0, 0);
  }
}
