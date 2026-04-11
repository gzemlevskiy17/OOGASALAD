package oogasalad.model.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
    if (!allComponents.contains(component)) {
      allComponents.add(component);
    }
    synchronizeActiveMembership(component);
  }

  @Override
  public void removeComponent(GameComponent component) {
    activeComponents.remove(component);
    allComponents.remove(component);
  }

  @Override
  public void destroyComponent(GameComponent component) {
    if (!allComponents.contains(component)) {
      return;
    }
    component.destroy();
    removeComponent(component);
  }

  @Override
  public void activateComponent(GameComponent component) {
    if (!allComponents.contains(component)) {
      return;
    }
    component.activate();
    synchronizeActiveMembership(component);
  }

  @Override
  public void deactivateComponent(GameComponent component) {
    if (!allComponents.contains(component)) {
      return;
    }
    component.deactivate();
    activeComponents.remove(component);
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
    if (!joints.contains(joint)) {
      joints.add(joint);
    }
  }

  @Override
  public void removeJoint(Joint joint) {
    joints.remove(joint);
  }

  private void synchronizeActiveMembership(GameComponent component) {
    if (component.isActive()) {
      if (!activeComponents.contains(component)) {activeComponents.add(component);}
    } else {activeComponents.remove(component);}
  }
}
