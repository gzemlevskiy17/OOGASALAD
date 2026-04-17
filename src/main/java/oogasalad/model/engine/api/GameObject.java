package oogasalad.model.engine.api;

import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.StaticState;
import oogasalad.view.core.api.Renderable;

//TODO: is the interface needed?
//TODO: more detailed comments

/**
 * Base entity for the simulation. Exposes physical properties.
 */
public interface GameObject extends Renderable {

  String getTag();

  void setTag(String tag);

  ConvexPolygon getPolygon();

  StaticState getStaticState();

  void setStaticState(StaticState state);
}
