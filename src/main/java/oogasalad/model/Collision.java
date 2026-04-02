package oogasalad.model;

import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Vector;

public class Collision {

  private GameObject objectA;
  private GameObject objectB;
  private boolean collided;
  private Vector penetration;
  private Vector[] referenceEdge;
  private Vector[] incidentEdge;

  public Collision(GameObject objectA, GameObject objectB, boolean collided, Vector penetration) {
    this.objectA = objectA;
    this.objectB = objectB;
    this.collided = collided;
    this.penetration = penetration;
  }

  public boolean isCollided() {
    return collided;
  }

  public void setCollided(boolean collided) {
    this.collided = collided;
  }

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

  public void setIncidentEdge(Vector[] incidentEdge) {
    this.incidentEdge = incidentEdge;
  }
}
