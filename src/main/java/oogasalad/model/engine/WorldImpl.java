package oogasalad.model.engine;

import java.util.ArrayList;
import java.util.List;
import oogasalad.model.engine.api.Joint;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.GameComponent;

/**
 * Default in-memory {@link World} implementation.
 *
 * <p>Maintains both the full set of registered components and the active subset exposed to the
 * engine/render loop. Lists returned to callers are defensive, unmodifiable snapshots that
 * preserve registration order.
 */
public class WorldImpl implements World {

  private final List<GameComponent> allComponents;
  private final List<GameComponent> activeComponents;
  private final List<Joint> joints;

  /**
   * Creates an empty world with no components or joints registered.
   */
  public WorldImpl() {
    this.allComponents = new ArrayList<>();
    this.activeComponents = new ArrayList<>();
    this.joints = new ArrayList<>();
  }

  /** Returns a read-only active components list. */
  @Override
  public List<GameComponent> getComponents() {
    synchronizeAllActiveMembership();
    return List.copyOf(activeComponents);
  }


  /** Returns a read-only snapshot of every registered component. */
  @Override
  public List<GameComponent> getAllComponents() {
    return List.copyOf(allComponents);
  }


  /** Returns a read-only snapshot of components whose active flag is currently true. */
  @Override
  public List<GameComponent> getActiveComponents() {
    synchronizeAllActiveMembership();
    return List.copyOf(activeComponents);
  }


  /** Returns a read-only snapshot of the registered joint list. */
  @Override
  public List<Joint> getJoints() {
    return List.copyOf(joints);
  }

  /** Adds the component if absent, then syncs its active membership. */
  @Override
  public void addComponent(GameComponent component) {
    if (!allComponents.contains(component)) {
      allComponents.add(component);
    }
    synchronizeActiveMembership(component);
  }


  /** Removes the component from both the master and active collections. */
  @Override
  public void removeComponent(GameComponent component) {
    activeComponents.remove(component);
    allComponents.remove(component);
  }


  /** Destroys the component and unregisters it when it is present in the world. */
  @Override
  public void destroyComponent(GameComponent component) {
    if (!allComponents.contains(component)) {
      return;
    }
    component.destroy();
    removeComponent(component);
  }


  /** Marks a registered component active and syncs it into the active collection. */
  @Override
  public void activateComponent(GameComponent component) {
    if (!allComponents.contains(component)) {
      return;
    }
    component.activate();
    synchronizeActiveMembership(component);
  }


  /** Marks a registered component inactive and removes it from the active collection. */
  @Override
  public void deactivateComponent(GameComponent component) {
    if (!allComponents.contains(component)) {
      return;
    }
    component.deactivate();
    activeComponents.remove(component);
  }


  /** Activates every registered component and rebuilds active membership accordingly. */
  @Override
  public void activateAll() {
    for (GameComponent component : new ArrayList<>(allComponents)) {
      component.activate();
      synchronizeActiveMembership(component);
    }
  }


  /** Deactivates every component and clears the active collection. */
  @Override
  public void deactivateAll() {
    for (GameComponent component : new ArrayList<>(allComponents)) {
      component.deactivate();}
    activeComponents.clear();
  }


  /** Removes every component and joint from the world. */
  @Override
  public void clear() {
    activeComponents.clear();
    allComponents.clear();
    joints.clear();
  }


  /** Adds the joint if it is not already registered. */
  @Override
  public void addJoint(Joint joint) {
    if (!joints.contains(joint)) {
      joints.add(joint);
    }
  }

  /** Removes the joint if it is currently registered. */
  @Override
  public void removeJoint(Joint joint) {
    joints.remove(joint);
  }

  /** Active state toggle, if object is in active list, delete it, if not add it to active list. */
  private void synchronizeActiveMembership(GameComponent component) {
    if (component.isActive()) {
      if (!activeComponents.contains(component)) {
        activeComponents.add(component);
      }
    } else {
      activeComponents.remove(component);}
  }

  private void synchronizeAllActiveMembership() {
    activeComponents.removeIf(component -> !allComponents.contains(component)
        || !component.isActive());
    for (GameComponent component : allComponents) {
      synchronizeActiveMembership(component);
    }
  }
}
