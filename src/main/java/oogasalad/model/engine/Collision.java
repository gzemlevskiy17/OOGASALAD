package oogasalad.model.engine;

import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Vector;

/**
 * Stores collision data for a single pair of {@link GameObject} instances.
 *
 * <p>Use this class to pass narrow-phase collision results (penetration axis/depth and selected
 * clipping edges) into later resolution phases such as impulse application and positional
 * correction.
 *
 * <p><strong>Assumptions:</strong> This object is mutable and intended for a single simulation
 * step. Edge arrays are expected to contain exactly two points when present.
 *
 * <p><strong>Dependencies:</strong> Depends on {@link GameObject} for object references and
 * {@link Vector} for geometric data.
 *
 * <p><strong>Example:</strong>
 *
 * <pre>{@code
 * Collision collision = new Collision(a, b, true, new Vector(0, 1));
 * collision.setReferenceEdge(new Vector[] {r0, r1});
 * collision.setIncidentEdge(new Vector[] {i0, i1});
 * }</pre>
 *
 * @author Georgiy Zemlevskiy
 */
public class Collision {

  private GameObject objectA;
  private GameObject objectB;
  private Vector penetration;
  private Vector[] referenceEdge;
  private Vector[] incidentEdge;

  /**
   * Creates a collision record for two objects.
   *
   * @param objectA first colliding object
   * @param objectB second colliding object
   * @param penetration penetration vector (direction and magnitude); often zero when no overlap
   */
  public Collision(GameObject objectA, GameObject objectB, Vector penetration) {
    this.objectA = objectA;
    this.objectB = objectB;
    this.penetration = penetration;
  }
  //TODO: rename to incident & reference objects?
  public GameObject getObjectA() {
    return objectA;
  }

  public GameObject getObjectB() {
    return objectB;
  }

  public Vector getPenetration() {
    return penetration;
  }

  public void setPenetration(Vector penetration) {
    this.penetration = penetration;
  }

  public Vector[] getReferenceEdge() {
    return referenceEdge;
  }

  public void setReferenceEdge(Vector[] referenceEdge) {
    this.referenceEdge = referenceEdge;
  }

  public Vector[] getIncidentEdge() {
    return incidentEdge;
  }

  //TODO: actually use and move into constructor
  public void setIncidentEdge(Vector[] incidentEdge) {
    this.incidentEdge = incidentEdge;
  }
}
