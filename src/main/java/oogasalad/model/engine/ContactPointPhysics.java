package oogasalad.model.engine;

import oogasalad.model.engine.api.Vector;

/**
 * Physics data for a contact point in a collision manifold used while resolving a single
 * collision.
 */
public class ContactPointPhysics {

  Vector contactPoint;
  Vector positionFromIncident;
  Vector positionFromReference;
  Vector relativeVelocity;
  double depth;

  /**
   * Creates contact data for a clipped collision point.
   *
   * @param contactPoint world-space contact point
   * @param depth        penetration depth at the contact point
   */
  public ContactPointPhysics(Vector contactPoint, double depth) {
    this.contactPoint = contactPoint;
    this.depth = depth;
  }
}