package oogasalad.controller.player.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import oogasalad.model.engine.api.Joint;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.DriveComponent;
import oogasalad.model.engine.internal.components.GameComponent;
import oogasalad.model.engine.internal.components.PigComponent;
import oogasalad.model.engine.internal.components.StructuralComponent;
import oogasalad.model.engine.internal.joints.RevoluteJoint;
import oogasalad.model.engine.internal.joints.RigidJoint;
import oogasalad.model.player.api.PlacedComponent;
import oogasalad.model.player.api.VehicleLayout;

/**
 * Translates a VehicleLayout into GameComponent instances and RigidJoints in the World. Each filled
 * grid cell becomes one component positioned in world space. Adjacent filled cells are connected
 * with a RigidJoint so the vehicle moves as one body.
 *
 * @author Paulina Vvedenskaya
 */
public class VehicleFactory {

  private static final ResourceBundle PALETTE =
      ResourceBundle.getBundle("oogasalad.view.authoring.VehicleComponentPalette");

  private static final double CELL_SIZE = 60.0;
  private static final double DEFAULT_FRICTION = 0.3;
  private static final double DEFAULT_MASS_PIG = 2.0;
  private static final double DEFAULT_MASS_DRIVE = 20.0;
  private static final double DEFAULT_MASS_STRUCTURAL = 1.5;
  private static final double DEFAULT_MAX_HP = 100.0;

  private static final Set<String> WHEEL_TYPES = Set.of(
      "Wheel", "Small Wheel", "Large Wheel"
  );

  public void buildVehicle(VehicleLayout layout, World world, double startX, double startY) {
    Map<String, GameComponent> placed = new HashMap<>();
    Map<String, String> placedTypes = new HashMap<>();

    for (PlacedComponent p : layout.placements()) {
      double worldX = startX + p.col() * CELL_SIZE;
      double worldY = startY + p.row() * CELL_SIZE;
      GameComponent component = createComponent(p.componentType(), worldX, worldY);
      world.addComponent(component);
      String key = toKey(p.row(), p.col());
      placed.put(key, component);
      placedTypes.put(key, p.componentType());
    }

    for (PlacedComponent p : layout.placements()) {
      GameComponent current = placed.get(toKey(p.row(), p.col()));

      GameComponent right = placed.get(toKey(p.row(), p.col() + 1));
      if (right != null) {
        world.addJoint(buildJoint(current, right, p.componentType(),
            layout.getComponentAtIndex(p.row(), p.col() + 1).orElse("")));
      }

      GameComponent below = placed.get(toKey(p.row() + 1, p.col()));
      if (below != null) {
        world.addJoint(buildJoint(current, below, p.componentType(),
            layout.getComponentAtIndex(p.row() + 1, p.col()).orElse("")));
      }
    }

    addAntiRotationJoints(layout, world, placed, placedTypes);
  }

  private Joint buildJoint(GameComponent a, GameComponent b, String typeA, String typeB) {
    if (WHEEL_TYPES.contains(typeA) || WHEEL_TYPES.contains(typeB)) {
      Vector posA = a.getState().getLinearPosition();
      Vector posB = b.getState().getLinearPosition();
      Vector localAnchorA = posB.subtract(posA).multiply(0.5);
      Vector localAnchorB = posA.subtract(posB).multiply(0.5);
      return new RevoluteJoint(a, b, localAnchorA, localAnchorB);
    }
    return new RigidJoint(a, b);
  }

  private void addAntiRotationJoints(VehicleLayout layout, World world, Map<String, GameComponent> placed,
      Map<String, String> placedTypes) {
    Set<String> createdPairs = new HashSet<>();
    int[][] offsets = {{0, 2}, {2, 0}, {1, 1}, {1, -1}};

    for (PlacedComponent component : layout.placements()) {
      for (int[] offset : offsets) {
        int targetRow = component.row() + offset[0];
        int targetCol = component.col() + offset[1];
        String startKey = toKey(component.row(), component.col());
        String targetKey = toKey(targetRow, targetCol);
        if (!placed.containsKey(targetKey) || !isNonWheelPair(placedTypes, startKey, targetKey)) {
          continue;
        }
        if (!hasSupportCell(component.row(), component.col(), targetRow, targetCol, placedTypes)) {
          continue;
        }
        String pairKey = canonicalPairKey(startKey, targetKey);
        if (createdPairs.add(pairKey)) {
          world.addJoint(new RigidJoint(placed.get(startKey), placed.get(targetKey)));
        }
      }
    }
  }

  private boolean hasSupportCell(int rowA, int colA, int rowB, int colB, Map<String, String> placedTypes) {
    int rowDelta = Math.abs(rowA - rowB);
    int colDelta = Math.abs(colA - colB);

    if (rowDelta == 2 && colDelta == 0) {
      return isNonWheelCell(placedTypes, toKey((rowA + rowB) / 2, colA));
    }
    if (rowDelta == 0 && colDelta == 2) {
      return isNonWheelCell(placedTypes, toKey(rowA, (colA + colB) / 2));
    }
    if (rowDelta == 1 && colDelta == 1) {
      return isNonWheelCell(placedTypes, toKey(rowA, colB))
          || isNonWheelCell(placedTypes, toKey(rowB, colA));
    }
    return false;
  }

  private boolean isNonWheelPair(Map<String, String> placedTypes, String keyA, String keyB) {
    return isNonWheelCell(placedTypes, keyA) && isNonWheelCell(placedTypes, keyB);
  }

  private boolean isNonWheelCell(Map<String, String> placedTypes, String key) {
    String type = placedTypes.get(key);
    return type != null && !WHEEL_TYPES.contains(type);
  }

  private String canonicalPairKey(String keyA, String keyB) {
    return keyA.compareTo(keyB) < 0 ? keyA + "|" + keyB : keyB + "|" + keyA;
  }

  private String toKey(int row, int col) {
    return row + "," + col;
  }

  private GameComponent createComponent(String componentType, double x, double y) {
    String key = componentType.replace(" ", "");
    String sprite = PALETTE.containsKey(key + ".sprite") ? PALETTE.getString(key + ".sprite") : "";
    double width =
        PALETTE.containsKey(key + ".width") ? Double.parseDouble(PALETTE.getString(key + ".width"))
            : 40.0;
    double height = PALETTE.containsKey(key + ".height") ? Double.parseDouble(
        PALETTE.getString(key + ".height")) : 40.0;

    switch (componentType) {
      case "Pig":
        return new PigComponent(x, y, 0, DEFAULT_MASS_PIG, DEFAULT_FRICTION);
      case "Motor":
      case "Fan":
      case "Balloon":
      case "Rocket":
      case "Propeller":
        return new DriveComponent(x, y, 0, DEFAULT_MASS_DRIVE, DEFAULT_FRICTION,
            DEFAULT_MAX_HP, componentType, sprite, width, height);
      default:
        return new StructuralComponent(x, y, 0, DEFAULT_MASS_STRUCTURAL, DEFAULT_FRICTION,
            DEFAULT_MAX_HP, componentType, sprite, width, height);
    }
  }
}
