package oogasalad.model.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import oogasalad.model.engine.api.Joint;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.GameComponent;

/**
 * Default in-memory {@link World} implementation.
 *
 * <p>Maintains both the full set of registered components and the active subset exposed to the
 * engine/render loop.
 */
public class WorldImpl implements World {

  private final List<GameComponent> allComponents;
  private final List<GameComponent> activeComponents;
  private final List<Joint> joints;

  public WorldImpl() {
    this.allComponents = new ArrayList<>();
    this.activeComponents = new ArrayList<>();
    this.joints = new ArrayList<>();
  }

  @Override
  public Collection<GameComponent> getComponents() {
    return Collections.unmodifiableList(new ArrayList<>(activeComponents));
  }

  @Override
  public Collection<GameComponent> getAllComponents() {
    return Collections.unmodifiableList(new ArrayList<>(allComponents));
  }

  @Override
  public Collection<GameComponent> getActiveComponents() {
    return Collections.unmodifiableList(new ArrayList<>(activeComponents));
  }

  @Override
  public Collection<Joint> getJoints() {
    return Collections.unmodifiableList(new ArrayList<>(joints));
  }

  @Override
  public void addComponent(GameComponent component) {
    GameComponent componentToAdd = Objects.requireNonNull(component);
    if (!allComponents.contains(componentToAdd)) {
      allComponents.add(componentToAdd);
    }
    synchronizeActiveMembership(componentToAdd);
  }

  @Override
  public void removeComponent(GameComponent component) {
    GameComponent componentToRemove = Objects.requireNonNull(component);
    activeComponents.remove(componentToRemove);
    allComponents.remove(componentToRemove);
  }

  @Override
  public void destroyComponent(GameComponent component) {
    GameComponent componentToDestroy = Objects.requireNonNull(component);
    if (!allComponents.contains(componentToDestroy)) {
      return;
    }
    componentToDestroy.destroy();
    removeComponent(componentToDestroy);
  }

  @Override
  public void activateComponent(GameComponent component) {
    GameComponent componentToActivate = Objects.requireNonNull(component);
    if (!allComponents.contains(componentToActivate)) {
      return;
    }
    componentToActivate.activate();
    synchronizeActiveMembership(componentToActivate);
  }

  @Override
  public void deactivateComponent(GameComponent component) {
    GameComponent componentToDeactivate = Objects.requireNonNull(component);
    if (!allComponents.contains(componentToDeactivate)) {
      return;
    }
    componentToDeactivate.deactivate();
    activeComponents.remove(componentToDeactivate);
  }

  @Override
  public void activateAll() {
    for (GameComponent component : new ArrayList<>(allComponents)) {
      component.activate();
      synchronizeActiveMembership(component);
    }
  }

  @Override
  public void deactivateAll() {
    for (GameComponent component : new ArrayList<>(allComponents)) {
      component.deactivate();
    }
    activeComponents.clear();
  }

  @Override
  public void clear() {
    activeComponents.clear();
    allComponents.clear();
    joints.clear();
  }

  @Override
  public void addJoint(Joint joint) {
    Joint jointToAdd = Objects.requireNonNull(joint);
    if (!joints.contains(jointToAdd)) {
      joints.add(jointToAdd);
    }
  }

  @Override
  public void removeJoint(Joint joint) {
    joints.remove(Objects.requireNonNull(joint));
  }

  private void synchronizeActiveMembership(GameComponent component) {
    if (component.isActive()) {
      if (!activeComponents.contains(component)) {
        activeComponents.add(component);
      }
    } else {
      activeComponents.remove(component);
    }
  }
}
