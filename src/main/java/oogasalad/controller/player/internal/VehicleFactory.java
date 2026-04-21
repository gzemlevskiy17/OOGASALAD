package oogasalad.controller.player.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.DriveComponent;
import oogasalad.model.engine.internal.components.GameComponent;
import oogasalad.model.engine.internal.components.PigComponent;
import oogasalad.model.engine.internal.components.StructuralComponent;
import oogasalad.model.engine.internal.joints.RigidJoint;
import oogasalad.model.player.api.PlacedComponent;
import oogasalad.model.player.api.VehicleLayout;

/**
 * Translates a VehicleLayout into GameComponent instances and joints in the World. Cardinal
 * neighbors are pinned at their shared edge midpoint with {@link RigidJoint} (zero rest distance)
 * so the whole vehicle behaves as a single rigid body. Triangle chord and anti-rotation joints
 * provide additional bracing for cells with only one neighbor. Wheel cells are plain structural
 * bodies — the tire spin is a cosmetic view-side animation driven off linear speed.
 *
 * @author Paulina Vvedenskaya
 */
public class VehicleFactory {

  private static final ResourceBundle PALETTE =
      ResourceBundle.getBundle("oogasalad.view.authoring.VehicleComponentPalette");

  private static final double CELL_SIZE = 60.0;
  private static final double HALF_CELL = CELL_SIZE / 2.0;
  private static final double DEFAULT_FRICTION = 0.3;
  private static final double DEFAULT_MASS_PIG = 2.0;
  private static final double DEFAULT_MASS_DRIVE = 20.0;
  private static final double DEFAULT_MASS_STRUCTURAL = 1.5;
  private static final double DEFAULT_MAX_HP = 100.0;

  public void buildVehicle(VehicleLayout layout, World world, double startX, double startY) {
    List<PlacedComponent> resolvedPlacements = compactVerticalGaps(layout);
    Map<String, GameComponent> placed = new HashMap<>();

    for (PlacedComponent p : resolvedPlacements) {
      double worldX = startX + p.col() * CELL_SIZE;
      double worldY = startY + p.row() * CELL_SIZE;
      GameComponent body = createComponent(p.componentType(), worldX, worldY);
      world.addComponent(body);
      placed.put(toKey(p.row(), p.col()), body);
    }

    Set<String> edgeKeys = new HashSet<>();
    for (PlacedComponent p : resolvedPlacements) {
      GameComponent current = placed.get(toKey(p.row(), p.col()));

      GameComponent right = placed.get(toKey(p.row(), p.col() + 1));
      if (right != null) {
        world.addJoint(new RigidJoint(current, right,
            new Vector(HALF_CELL, 0), new Vector(-HALF_CELL, 0), 0.0, 0.0));
        edgeKeys.add(canonicalPairKey(toKey(p.row(), p.col()), toKey(p.row(), p.col() + 1)));
      }
      GameComponent below = placed.get(toKey(p.row() + 1, p.col()));
      if (below != null) {
        world.addJoint(new RigidJoint(current, below,
            new Vector(0, HALF_CELL), new Vector(0, -HALF_CELL), 0.0, 0.0));
        edgeKeys.add(canonicalPairKey(toKey(p.row(), p.col()), toKey(p.row() + 1, p.col())));
      }
    }
    addRightAngleRigidChords(resolvedPlacements, world, placed, edgeKeys);
    addAntiRotationJoints(resolvedPlacements, world, placed, edgeKeys);
  }

  /**
   * For each filled cell, if two perpendicular cardinal neighbors exist, adds the closing joint
   * between the two outer cells — the hypotenuse of the isosceles right triangle.
   */
  private void addRightAngleRigidChords(List<PlacedComponent> placements, World world,
      Map<String, GameComponent> placed, Set<String> edgeKeys) {
    for (PlacedComponent p : placements) {
      int r = p.row();
      int c = p.col();
      tryAddRigidTriangleChord(r, c, r, c + 1, r + 1, c, placed, world, edgeKeys);
      tryAddRigidTriangleChord(r, c, r, c - 1, r + 1, c, placed, world, edgeKeys);
      tryAddRigidTriangleChord(r, c, r, c + 1, r - 1, c, placed, world, edgeKeys);
      tryAddRigidTriangleChord(r, c, r, c - 1, r - 1, c, placed, world, edgeKeys);
    }
  }

  private void tryAddRigidTriangleChord(int cornerR, int cornerC, int n1r, int n1c, int n2r,
      int n2c, Map<String, GameComponent> placed, World world, Set<String> edgeKeys) {
    String kCorner = toKey(cornerR, cornerC);
    String k1 = toKey(n1r, n1c);
    String k2 = toKey(n2r, n2c);
    if (!placed.containsKey(k1) || !placed.containsKey(k2)) {
      return;
    }
    RigidJoint.registerRigidTriangle(world, placed.get(kCorner), placed.get(k1), placed.get(k2),
        edgeKeys, kCorner, k1, k2);
  }

  private void addAntiRotationJoints(List<PlacedComponent> placements, World world,
      Map<String, GameComponent> placed, Set<String> edgeKeys) {
    int[][] offsets = {{0, 2}, {2, 0}, {1, 1}, {1, -1}};

    for (PlacedComponent component : placements) {
      for (int[] offset : offsets) {
        int targetRow = component.row() + offset[0];
        int targetCol = component.col() + offset[1];
        String startKey = toKey(component.row(), component.col());
        String targetKey = toKey(targetRow, targetCol);
        if (!placed.containsKey(targetKey)) {
          continue;
        }
        if (!hasSupportCell(component.row(), component.col(), targetRow, targetCol, placed)) {
          continue;
        }
        String pairKey = canonicalPairKey(startKey, targetKey);
        if (!edgeKeys.add(pairKey)) {
          continue;
        }
        world.addJoint(new RigidJoint(placed.get(startKey), placed.get(targetKey)));
      }
    }
  }

  private boolean hasSupportCell(int rowA, int colA, int rowB, int colB,
      Map<String, GameComponent> placed) {
    int rowDelta = Math.abs(rowA - rowB);
    int colDelta = Math.abs(colA - colB);

    if (rowDelta == 2 && colDelta == 0) {
      return placed.containsKey(toKey((rowA + rowB) / 2, colA));
    }
    if (rowDelta == 0 && colDelta == 2) {
      return placed.containsKey(toKey(rowA, (colA + colB) / 2));
    }
    if (rowDelta == 1 && colDelta == 1) {
      return placed.containsKey(toKey(rowA, colB)) || placed.containsKey(toKey(rowB, colA));
    }
    return false;
  }

  private String canonicalPairKey(String keyA, String keyB) {
    return keyA.compareTo(keyB) < 0 ? keyA + "|" + keyB : keyB + "|" + keyA;
  }

  private String toKey(int row, int col) {
    return row + "," + col;
  }

  private List<PlacedComponent> compactVerticalGaps(VehicleLayout layout) {
    Map<Integer, List<PlacedComponent>> byColumn = new HashMap<>();
    for (PlacedComponent placement : layout.placements()) {
      byColumn.computeIfAbsent(placement.col(), ignored -> new ArrayList<>()).add(placement);
    }

    List<PlacedComponent> compacted = new ArrayList<>();
    for (List<PlacedComponent> columnPlacements : byColumn.values()) {
      columnPlacements.sort((a, b) -> Integer.compare(b.row(), a.row()));
      int targetRow = columnPlacements.get(0).row();
      for (PlacedComponent placement : columnPlacements) {
        compacted.add(new PlacedComponent(targetRow, placement.col(), placement.componentType()));
        targetRow--;
      }
    }
    return compacted;
  }

  private GameComponent createComponent(String componentType, double x, double y) {
    double[] wh = paletteDims(componentType);
    double width = wh[0];
    double height = wh[1];

    switch (componentType) {
      case "Pig":
        return new PigComponent(x, y, 0, DEFAULT_MASS_PIG, DEFAULT_FRICTION);
      case "Motor":
      case "Fan":
      case "Balloon":
      case "Rocket":
      case "Propeller":
        return new DriveComponent(x, y, 0, DEFAULT_MASS_DRIVE, DEFAULT_FRICTION,
            DEFAULT_MAX_HP, componentType, width, height);
      default:
        return new StructuralComponent(x, y, 0, DEFAULT_MASS_STRUCTURAL, DEFAULT_FRICTION,
            DEFAULT_MAX_HP, componentType, width, height);
    }
  }

  private double[] paletteDims(String displayName) {
    String key = displayName.replace(" ", "");
    double width = PALETTE.containsKey(key + ".width")
        ? Double.parseDouble(PALETTE.getString(key + ".width")) : 40.0;
    double height = PALETTE.containsKey(key + ".height")
        ? Double.parseDouble(PALETTE.getString(key + ".height")) : 40.0;
    return new double[]{width, height};
  }
}
