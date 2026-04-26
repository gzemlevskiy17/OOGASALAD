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
  private int collisionCategoryBits;
  private int collisionMaskBits;


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
    this.collisionCategoryBits = 0x0001;
    this.collisionMaskBits = 0xFFFF_FFFF;
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
  public int getCollisionCategoryBits() {
    return collisionCategoryBits;
  }

  @Override
  public int getCollisionMaskBits() {
    return collisionMaskBits;
  }

  @Override
  public void setCollisionFilter(int categoryBits, int maskBits) {
    this.collisionCategoryBits = categoryBits;
    this.collisionMaskBits = maskBits;
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
  public double x() {
    return staticState.getLinearPosition().x();
  }

  @Override
  public double y() {
    return staticState.getLinearPosition().y();
  }

  @Override
  public double rotation() {
    return staticState.getAngularPosition();
  }
}
