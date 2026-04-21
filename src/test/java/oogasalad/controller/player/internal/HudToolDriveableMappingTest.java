package oogasalad.controller.player.internal;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import oogasalad.model.engine.WorldImpl;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.DriveComponent;
import oogasalad.model.engine.internal.components.GameComponent;
import oogasalad.model.player.api.PlacedComponent;
import oogasalad.model.player.api.VehicleLayout;
import org.junit.jupiter.api.Test;

/**
 * Cross-checks the HUD tools list in {@code GamePlayAppScreen.properties} against the
 * {@link VehicleFactory} switch: every HUD tool must correspond to a placeable component type that
 * VehicleFactory instantiates as a {@link DriveComponent} whose {@code displayName.toLowerCase()}
 * equals the tool id. Otherwise the HUD button is a silent no-op — bindDriveActions has nothing to
 * bind.
 *
 * <p>A failure here means the HUD config and the factory's component types have drifted. Either
 * remove the orphan tool from the properties file, or add the missing case to VehicleFactory's
 * switch (and, if needed, to the palette).
 */
class HudToolDriveableMappingTest {

  private static final double START_X = 380.0;
  private static final double START_Y = 330.0;

  private List<String> hudTools() {
    ResourceBundle props = ResourceBundle.getBundle("oogasalad.view.player.GamePlayAppScreen");
    return Arrays.stream(props.getString("tools").split(","))
        .map(String::trim)
        .toList();
  }

  // Capitalize first letter to match VehicleFactory's switch case strings ("Fan", "Balloon", ...).
  private String capitalize(String tool) {
    if (tool.isEmpty()) {
      return tool;
    }
    return Character.toUpperCase(tool.charAt(0)) + tool.substring(1);
  }

  private boolean worldHasDriveWithDisplayNameIgnoreCase(World world, String toolId) {
    for (GameComponent c : world.getAllComponents()) {
      if (c instanceof DriveComponent dc && dc.getDisplayName().equalsIgnoreCase(toolId)) {
        return true;
      }
    }
    return false;
  }

  @Test
  void fanTool_mapsToDriveComponent() {
    World world = new WorldImpl();
    new VehicleFactory().buildVehicle(
        new VehicleLayout(List.of(new PlacedComponent(0, 0, "Fan"))),
        world, START_X, START_Y);

    assertTrue(worldHasDriveWithDisplayNameIgnoreCase(world, "fan"),
        "placing 'Fan' should create a DriveComponent");
  }

  @Test
  void balloonTool_mapsToDriveComponent() {
    World world = new WorldImpl();
    new VehicleFactory().buildVehicle(
        new VehicleLayout(List.of(new PlacedComponent(0, 0, "Balloon"))),
        world, START_X, START_Y);

    assertTrue(worldHasDriveWithDisplayNameIgnoreCase(world, "balloon"),
        "placing 'Balloon' should create a DriveComponent");
  }

  /**
   * Bug A: palette + HUD use "Engine", but VehicleFactory's switch only recognizes "Motor", so
   * placing "Engine" falls through to the default branch and becomes a StructuralComponent.
   * Expected to FAIL until either the HUD tool is renamed to "motor" or VehicleFactory's switch
   * gains an "Engine" case.
   */
  @Test
  void engineTool_mapsToDriveComponent() {
    World world = new WorldImpl();
    new VehicleFactory().buildVehicle(
        new VehicleLayout(List.of(new PlacedComponent(0, 0, "Engine"))),
        world, START_X, START_Y);

    assertTrue(worldHasDriveWithDisplayNameIgnoreCase(world, "engine"),
        "placing 'Engine' should create a DriveComponent — currently fails because "
            + "VehicleFactory.createComponent only recognizes 'Motor', not 'Engine'");
  }

  /**
   * Bug B: HUD lists "blower" but neither the palette nor VehicleFactory has that type.
   */
  @Test
  void blowerTool_mapsToDriveComponent() {
    World world = new WorldImpl();
    new VehicleFactory().buildVehicle(
        new VehicleLayout(List.of(new PlacedComponent(0, 0, "Blower"))),
        world, START_X, START_Y);

    assertTrue(worldHasDriveWithDisplayNameIgnoreCase(world, "blower"),
        "placing 'Blower' should create a DriveComponent — currently fails because "
            + "neither the palette nor VehicleFactory knows 'Blower'");
  }

  /**
   * Summary test: every tool in the HUD properties file must resolve to a placeable DriveComponent.
   * Fails with a single aggregated list of orphan tool ids.
   */
  @Test
  void everyHudTool_resolvesToADriveComponent() {
    List<String> orphans = new java.util.ArrayList<>();
    for (String tool : hudTools()) {
      World world = new WorldImpl();
      try {
        new VehicleFactory().buildVehicle(
            new VehicleLayout(List.of(new PlacedComponent(0, 0, capitalize(tool)))),
            world, START_X, START_Y);
      } catch (RuntimeException ex) {
        orphans.add(tool + " (threw " + ex.getClass().getSimpleName() + ")");
        continue;
      }
      if (!worldHasDriveWithDisplayNameIgnoreCase(world, tool)) {
        orphans.add(tool);
      }
    }
    if (!orphans.isEmpty()) {
      fail("HUD tools with no matching DriveComponent: " + orphans
          + ". Either rename the tool, remove it from GamePlayAppScreen.properties, or add a "
          + "case to VehicleFactory.createComponent.");
    }
  }
}
