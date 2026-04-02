package oogasalad.model;

import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Vector;

public class ConcreteGameObject implements GameObject {

  private double mass;
  private double momentOfInertia;
  private double restitution;

  //TODO: move to interface?
  private ObjectState state;
  private ConvexPolygon polygon;

  private Vector force;
//  private Vector acceleration;
//  private Vector

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
