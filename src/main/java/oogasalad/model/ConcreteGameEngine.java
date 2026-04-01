package oogasalad.model;

import oogasalad.model.engine.api.GameEngine;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Joint;
import oogasalad.model.engine.api.World;

public class ConcreteGameEngine implements GameEngine {

  private World world;
  public static double elapsedTime;

  public ConcreteGameEngine() {
  }

  @Override
  public void step() {
//    for (Joint joint :  world.getJoints()) {
//
//    }

    for (GameObject gameObject : world.getObjects()) {
      gameObject.updateVelocity(elapsedTime);
      gameObject.move(elapsedTime);
    }


  }
}
