package oogasalad.model;

import java.util.List;
import oogasalad.model.engine.api.Vector;

public class ConvexPolygon {

  private List<Vector> points;

  public ConvexPolygon(List<Vector> points) {
    this.points = points;
  }

  public List<Vector> getPoints() { //in CW order
    return points;
  }
}
