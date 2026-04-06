package oogasalad.model.engine.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import oogasalad.model.engine.internal.GameComponent;

/** Memory container for all simulation components and physical joints. */
public interface World {
  /**
   * Returns the active components currently exposed to simulation and rendering.
   */
  Collection<GameComponent> getComponents();

  /**
   * Returns every registered component, including inactive ones.
   */
  Collection<GameComponent> getAllComponents();

  /**
   * Returns every registered component whose active flag is currently true.
   */
  Collection<GameComponent> getActiveComponents();

  Collection<Joint> getJoints();

  /**
   * Registers a component with the world.
   */
  void addComponent(GameComponent component);

  /**
   * Removes a component from the world without calling destroy on it.
   */
  void removeComponent(GameComponent component);

  /**
   * Destroys and unregisters a component from the world.
   */
  default void destroyComponent(GameComponent component) {
    Objects.requireNonNull(component).destroy();
    removeComponent(component);
  }

  /**
   * Activates a registered component.
   */
  default void activateComponent(GameComponent component) {
    Objects.requireNonNull(component).activate();
  }

  /**
   * Deactivates a registered component.
   */
  default void deactivateComponent(GameComponent component) {
    Objects.requireNonNull(component).deactivate();
  }

  /**
   * Activates every registered component.
   */
  default void activateAll() {
    for (GameComponent component : new ArrayList<>(getAllComponents())) {
      activateComponent(component);
    }
  }

  /**
   * Deactivates every registered component.
   */
  default void deactivateAll() {
    for (GameComponent component : new ArrayList<>(getAllComponents())) {
      deactivateComponent(component);
    }
  }

  /**
   * Removes all components and joints from the world.
   */
  void clear();

  void addJoint(Joint joint);

  void removeJoint(Joint joint);

  /**
   * Compatibility alias for older code still using GameObject naming.
   */
  default Collection<GameObject> getObjects() {
    return new ArrayList<>(getComponents());
  }

  /**
   * Compatibility alias for older code still using GameObject naming.
   */
  default void addObject(GameObject object) {
    if (!(Objects.requireNonNull(object) instanceof GameComponent component)) {
      throw new IllegalArgumentException("World only supports GameComponent objects");
    }
    addComponent(component);
  }

  /**
   * Compatibility alias for older code still using GameObject naming.
   */
  default void removeObject(GameObject object) {
    if (object instanceof GameComponent component) {
      removeComponent(component);
    }
  }

  /**
   * Compatibility alias for older code still using GameObject naming.
   */
  default void destroyObject(GameObject object) {
    if (object instanceof GameComponent component) {
      destroyComponent(component);
    }
  }
}
