package oogasalad.model.player.api;

import java.util.Collection;
import oogasalad.model.engine.api.GameObject;

/** Defines the rules and constraints for the vehicle assembly sandbox. */
public interface VehicleWorkspace {

  /** @return true if the part is permitted within this specific workspace. */
  boolean isValidPart(GameObject part);

  /**
   * Validates the structural integrity or legal constraints of a built vehicle.
   * @param parts The collection of objects making up the vehicle.
   * @return true if the assembly meets all sandbox rules.
   */
  boolean isAssemblyValid(Collection<GameObject> parts);
}