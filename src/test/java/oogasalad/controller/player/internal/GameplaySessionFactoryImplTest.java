package oogasalad.controller.player.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.Map;
import oogasalad.model.engine.WorldImpl;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.StructuralComponent;
import oogasalad.model.engine.internal.components.TerrainSpline;
import oogasalad.model.engine.internal.spline.ParabolaSpline;
import oogasalad.model.player.api.PlacedComponent;
import oogasalad.model.player.api.VehicleLayout;
import oogasalad.view.core.api.Renderable;
import org.junit.jupiter.api.Test;

/**
 * Tests gameplay session assembly helpers.
 */
class GameplaySessionFactoryImplTest {

  private static final double ASSERTION_DELTA = 1e-6;

  @Test
  void WheelConnectorRotationSources_WheelConnectedToFrame_MapsWheelToConnectedFrame() {
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

  @Test
  void AdjustedSpawnYForVehicleGrounding_VehicleWouldIntersectTerrain_MovesSpawnAboveSurface() {
    VehicleLayout layout = new VehicleLayout(List.of(new PlacedComponent(4, 0, "Frame")));
    WorldImpl world = worldWithFlatTerrain(800.0);

    double adjusted = GameplaySessionFactoryImpl.adjustedSpawnYForVehicleGrounding(
        layout, world, 100.0, 780.0);

    assertEquals(490.0, adjusted, ASSERTION_DELTA);
  }

  @Test
  void AdjustedSpawnYForVehicleGrounding_SpawnAlreadyClearsTerrain_KeepsSpawnY() {
    VehicleLayout layout = new VehicleLayout(List.of(new PlacedComponent(4, 0, "Frame")));
    WorldImpl world = worldWithFlatTerrain(800.0);

    double adjusted = GameplaySessionFactoryImpl.adjustedSpawnYForVehicleGrounding(
        layout, world, 100.0, 400.0);

    assertEquals(400.0, adjusted, ASSERTION_DELTA);
  }

  @Test
  void AdjustedSpawnYForVehicleGrounding_NoTerrainUnderSpawnX_KeepsSpawnY() {
    VehicleLayout layout = new VehicleLayout(List.of(new PlacedComponent(4, 0, "Frame")));
    WorldImpl world = worldWithFlatTerrain(800.0);

    double adjusted = GameplaySessionFactoryImpl.adjustedSpawnYForVehicleGrounding(
        layout, world, 1200.0, 780.0);

    assertEquals(780.0, adjusted, ASSERTION_DELTA);
  }

  private StructuralComponent component(WorldImpl world, String displayName) {
    return world.getComponents().stream()
        .filter(StructuralComponent.class::isInstance)
        .map(StructuralComponent.class::cast)
        .filter(component -> displayName.equals(component.getDisplayName()))
        .findFirst()
        .orElseThrow();
  }

  private WorldImpl worldWithFlatTerrain(double surfaceY) {
    WorldImpl world = new WorldImpl();
    world.addComponent(new TerrainSpline(
        new ParabolaSpline(List.of(
            new Vector(0.0, surfaceY),
            new Vector(1000.0, surfaceY)), surfaceY + 200.0), 0.5));
    return world;
  }
}
