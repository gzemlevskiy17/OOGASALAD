package oogasalad.controller.player.internal;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.Map;
import oogasalad.model.engine.WorldImpl;
import oogasalad.model.engine.internal.components.StructuralComponent;
import oogasalad.model.player.api.PlacedComponent;
import oogasalad.model.player.api.VehicleLayout;
import oogasalad.view.core.api.Renderable;
import org.junit.jupiter.api.Test;

class GameplaySessionFactoryImplTest {

  @Test
  void wheelConnectorRotationSources_mapsWheelToConnectedFrame() {
    WorldImpl world = new WorldImpl();
    new VehicleFactory().buildVehicle(new VehicleLayout(List.of(
        new PlacedComponent(0, 0, "Frame"),
        new PlacedComponent(1, 0, "Wood Wheel")
    )), world, 100, 100);

    StructuralComponent frame = component(world, "Frame");
    StructuralComponent wheel = component(world, "Wood Wheel");

    Map<Renderable, Renderable> sources = GameplaySessionFactoryImpl.wheelConnectorRotationSources(
        world);

    assertSame(frame, sources.get(wheel));
  }

  private StructuralComponent component(WorldImpl world, String displayName) {
    return world.getComponents().stream()
        .filter(StructuralComponent.class::isInstance)
        .map(StructuralComponent.class::cast)
        .filter(component -> displayName.equals(component.getDisplayName()))
        .findFirst()
        .orElseThrow();
  }
}
