package oogasalad.model.engine;

import oogasalad.model.engine.api.CollisionSolver;
import oogasalad.model.engine.internal.components.DynamicComponent;
import oogasalad.model.engine.internal.components.PassThroughComponent;

public class DynamicPassthroughSolver {

  private DynamicComponent referenceObject;
  private PassThroughComponent incidentObject;

  public void solve(DynamicComponent referenceObject, PassThroughComponent incidentObject, Collision collision) {
    this.referenceObject = referenceObject;
    this.incidentObject = incidentObject;
  }
}
