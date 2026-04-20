package oogasalad.model.engine.internal.components;

import java.util.Objects;
import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.StaticState;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Vector;

public abstract class GameComponent implements GameObject {

  private StaticState staticState;
  private ConvexPolygon polygon;
  private boolean active; //TODO: depends on how people up the stack implement it
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
  }

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


  //TODO: should we expose this?
  @Override
  public void setStaticState(StaticState staticState) {
    this.staticState = staticState;
  }


  public boolean isActive() {
    return active;
  }

//  @Override
//  public boolean isTrigger() {
//    return trigger;
//  }

//  protected void setTrigger(boolean trigger) {
//    this.trigger = trigger;
//  }

  public void activate() {
    active = true;
  }

  public void deactivate() {
    active = false;
  }

  public void destroy() {
    deactivate();
  }

  @Override
  public double getX() {
    return staticState.getLinearPosition().x();
  }

  @Override
  public double getY() {
    return staticState.getLinearPosition().y();
  }

  @Override
  public double getRotation() {
    return staticState.getAngularPosition();
  }
}
