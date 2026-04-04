package oogasalad.model.engine.internal;

import oogasalad.model.engine.api.Pose;

public abstract class StaticComponent extends GameComponent{
  public StaticComponent(Pose pose, double mass, double friction) {
    super(pose, mass, friction);
  }}
