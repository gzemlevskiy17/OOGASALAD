package oogasalad.model.engine.api;

import java.util.Collection;

/** Memory container for all active objects and physical joints. */
public interface World {
  Collection<GameObject> getObjects();
  Collection<Joint> getJoints();

  void addObject(GameObject object);
  void addJoint(Joint joint);
}