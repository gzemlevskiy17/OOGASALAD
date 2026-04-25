package oogasalad.model.engine.api;

import java.util.ArrayList;
import java.util.List;
import oogasalad.model.engine.EngineError;
import oogasalad.model.engine.internal.components.GameComponent;

/**
 * Memory container for simulation components and physical joints.
 *
 * <p>A world tracks all registered components, the subset that is currently active, and every
 * joint participating in constraint solving.
 */
public interface World {
  /**
   * Returns the active components currently exposed to simulation and rendering in registration
   * order.
   */
  List<GameComponent> getComponents();

  /**
   * Returns every registered component, including inactive ones, in registration order.
   */
  List<GameComponent> getAllComponents();

  /**
   * Returns every registered component whose active flag is currently true, in registration order.
   */
  List<GameComponent> getActiveComponents();

  /**
   * Returns every registered component whose tag matches the requested tag, in registration order.
   */
  default List<GameComponent> getComponentsByTag(String tag) {
    List<GameComponent> taggedComponents = new ArrayList<>();
    for (GameComponent component : getAllComponents()) {
      if (component.getTag().equals(tag)) {taggedComponents.add(component);}
    }return List.copyOf(taggedComponents);
  }

  /**
   * Returns all joints currently registered with the world in registration order.
   */
  List<Joint> getJoints();

  /**
   * Registers a component with the world.
   *
   * <p>Implementations may choose whether duplicate registrations are ignored or replaced.
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
    component.destroy();
    removeComponent(component);
  }

  /**
   * Activates a registered component.
   */
  default void activateComponent(GameComponent component) {
    component.activate();
  }

  /**
   * Deactivates a registered component.
   */
  default void deactivateComponent(GameComponent component) {
    component.deactivate();
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

  /**
   * Registers a joint with the world.
   */
  void addJoint(Joint joint);

  /** Removes a joint from the world. */
  void removeJoint(Joint joint);

  /**
   * Compatibility alias for older code still using {@code GameObject} naming.
   */
  default List<GameObject> getObjects() {
    return new ArrayList<>(getComponents());
  }

  /**
   * Compatibility alias for older code still using {@code GameObject} naming.
   */
  default List<GameObject> getObjectsByTag(String tag) {
    return new ArrayList<>(getComponentsByTag(tag));
  }

  /**
   * Compatibility alias for older code still using {@code GameObject} naming.
   *
   * <p>Only {@link GameComponent} instances are supported by this world abstraction.
   */
  default void addObject(GameObject object) {
    if (!(object instanceof GameComponent component)) {
      throw new IllegalArgumentException(EngineError.WORLD_ONLY_SUPPORTS_GAME_COMPONENTS.message());
    }
    addComponent(component);
  }

  /**
   * Compatibility alias for older code still using {@code GameObject} naming.
   */
  default void removeObject(GameObject object) {
    if (object instanceof GameComponent component) {
      removeComponent(component);
    }
  }

  /**
   * Compatibility alias for older code still using {@code GameObject} naming.
   */
  default void destroyObject(GameObject object) {
    if (object instanceof GameComponent component) {
      destroyComponent(component);
    }
  }

  double getDragCoefficient();
}
