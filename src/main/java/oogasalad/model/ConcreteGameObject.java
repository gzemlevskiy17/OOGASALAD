package oogasalad.model;

import java.util.Collection;
import java.util.List;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Vector;

public class ConcreteGameObject implements GameObject {

  private double mass;
  private Vector position;
  private Vector velocity;
  private Vector force;

  @Override
  public double getMass() {
    return 0;
  }

  @Override
  public Vector getPosition() {
    return null;
  }

  @Override
  public Vector getVelocity() {
    return null;
  }

  @Override
  public Vector getForce() {
    return null;
  }

  @Override
  public void setMass() {

  }

  @Override
  public void setPosition(Vector position) {

  }

  @Override
  public void setVelocity(Vector velocity) {

  }

  @Override
  public void setForce(Vector force) {

  }

  @Override
  public void updateVelocity(double elapsedTime) {
    Vector changeInVelocity = force.normalize().multiply(force.magnitude() / mass);
    velocity = velocity.add(changeInVelocity);
  }

  @Override
  public void move(double elapsedTime) {
    position = position.add(velocity.multiply(elapsedTime));
  }

  @Override
  public Collection<Object> getHitboxes() {
    return List.of();
  }

  @Override
  public double getX() {
    return position.x();
  }

  @Override
  public double getY() {
    return position.y();
  }

  @Override
  public double getRotation() {
    return 0;
  }

  @Override
  public String getSprite() {
    return "";
  }
}
