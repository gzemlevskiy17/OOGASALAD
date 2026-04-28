package oogasalad.controller.player.internal;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import oogasalad.data.api.GameConfig;
import oogasalad.data.api.GameLoader;
import oogasalad.data.api.LevelLoader;
import oogasalad.data.internal.LevelAttributes;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.WorldImpl;
import oogasalad.model.engine.internal.components.GameComponent;
import oogasalad.model.engine.internal.components.StructuralComponent;
import oogasalad.model.player.api.PlacedComponent;
import oogasalad.model.player.api.VehicleLayout;
import oogasalad.view.core.api.Renderable;
import org.junit.jupiter.api.Test;

class GameplaySessionFactoryImplTest {

  private static final double FALLBACK_WORLD_WIDTH = 2600.0;
  private static final double FALLBACK_WORLD_HEIGHT = 1600.0;

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

  @Test
  void buildWithoutLevelPathCreatesEmptyFallbackWorld() {
    GameplayWorldBuilder builder = new GameplayWorldBuilder(
        new ThrowingLevelLoader(), new ThrowingGameLoader(), () -> null);

    GameplayWorldBuilder.BuiltWorld built = builder.build(new VehicleLayout(List.of()));

    assertTrue(built.world().getObjects().isEmpty());
    assertTrue(built.placements().isEmpty());
    assertEquals(FALLBACK_WORLD_WIDTH, built.bounds().width());
    assertEquals(FALLBACK_WORLD_HEIGHT, built.bounds().height());
  }

  private StructuralComponent component(WorldImpl world, String displayName) {
    for (GameComponent component : world.getComponents()) {
      if (component instanceof StructuralComponent structuralComponent) {
        if (displayName.equals(structuralComponent.getDisplayName())) {
          return structuralComponent;
        }
      }
    }
    throw new IllegalStateException("Missing component: " + displayName);
  }

  private static final class ThrowingLevelLoader implements LevelLoader {

    @Override
    public void loadLevel(String fileName, GameConfig game, World world,
        LevelAttributes levelAttributes) {
      throw new IllegalStateException("Level loader should not be called");
    }

    @Override
    public LevelAttributes loadAttributes(String fileName) {
      throw new IllegalStateException("Level attributes should not be called");
    }
  }

  private static final class ThrowingGameLoader implements GameLoader {

    @Override
    public GameConfig loadGame(String fileName) {
      throw new IllegalStateException("Game loader should not be called");
    }
  }
}
