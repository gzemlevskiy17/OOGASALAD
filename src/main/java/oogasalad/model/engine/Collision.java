package oogasalad.model.engine;

import java.util.Map;
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

  private final GameObject referenceObject;
  private final GameObject incidentObject;
  private Vector penetration;
  private Vector[] referenceEdge;
  private Vector[] incidentEdge;
  private Map<Vector, Double> collisionManifold;
  private double cachedNormalImpulse;
  private double cachedFrictionImpulse;
  private Vector cachedNormal;
  private double appliedNormalImpulse;
  private double appliedFrictionImpulse;

  /**
   * Creates a collision record for two objects.
   *
   * @param referenceObject first colliding object
   * @param incidentObject  second colliding object
   */
  public Collision(GameObject referenceObject, GameObject incidentObject) {
    this.referenceObject = referenceObject;
    this.incidentObject = incidentObject;
  }

  public GameObject getReferenceObject() {
    return referenceObject;
  }

  public GameObject getIncidentObject() {
    return incidentObject;
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

  public void setIncidentEdge(Vector[] incidentEdge) {
    this.incidentEdge = incidentEdge;
  }

  public Map<Vector, Double> getCollisionManifold() {
    return collisionManifold;
  }

  public void setManifold(Map<Vector, Double> collisionManifold) {
    this.collisionManifold = collisionManifold;
  }

  public double getCachedNormalImpulse() {
    return cachedNormalImpulse;
  }

  public void setCachedNormalImpulse(double cachedNormalImpulse) {
    this.cachedNormalImpulse = cachedNormalImpulse;
  }

  public double getCachedFrictionImpulse() {
    return cachedFrictionImpulse;
  }

  public void setCachedFrictionImpulse(double cachedFrictionImpulse) {
    this.cachedFrictionImpulse = cachedFrictionImpulse;
  }

  public Vector getCachedNormal() {
    return cachedNormal;
  }

  public void setCachedNormal(Vector cachedNormal) {
    this.cachedNormal = cachedNormal;
  }

  public double getAppliedNormalImpulse() {
    return appliedNormalImpulse;
  }

  public void setAppliedNormalImpulse(double appliedNormalImpulse) {
    this.appliedNormalImpulse = appliedNormalImpulse;
  }

  public double getAppliedFrictionImpulse() {
    return appliedFrictionImpulse;
  }

  public void setAppliedFrictionImpulse(double appliedFrictionImpulse) {
    this.appliedFrictionImpulse = appliedFrictionImpulse;
  }
}
