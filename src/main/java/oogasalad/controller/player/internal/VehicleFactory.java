package oogasalad.controller.player.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
 * Translates a VehicleLayout into GameComponent instances and joints in the World. Cardinal
 * neighbors (right / below) get {@link RevoluteJoint} (wheels) or {@link RigidJoint} (non-wheels).
 *
 * <p>Where two perpendicular cardinal neighbors of the same cell are both rigid links,
 * {@link #addRightAngleRigidChords} invokes {@link RigidJoint}'s triangle registration with stable
 * grid keys and {@link java.util.Set}-based edge deduplication so the cardinal pass and chord pass
 * share one physical edge set. {@link #addAntiRotationJoints} still adds longer braces where supported.
 *
 * @author Paulina Vvedenskaya
 */
public class VehicleFactory {

  private static final ResourceBundle PALETTE =
      ResourceBundle.getBundle("oogasalad.view.authoring.VehicleComponentPalette");

  private static final double CELL_SIZE = 60.0;
  private static final double DEFAULT_FRICTION = 0.3;
  private static final double DEFAULT_MASS_PIG = 2.0;
  private static final double DEFAULT_MASS_DRIVE = 5.0;
  private static final double DEFAULT_MASS_STRUCTURAL = 1.5;
  private static final double DEFAULT_MAX_HP = 100.0;

  private static final Set<String> WHEEL_TYPES = Set.of("Wheel", "Small Wheel", "Large Wheel");
  private static final Set<String> PIG_TYPES = Set.of("Pig", "Small Pig");

  public void buildVehicle(VehicleLayout layout, World world, double startX, double startY) {
    List<PlacedComponent> structuralPlacements = new ArrayList<>();
    List<PlacedComponent> pigPlacements = new ArrayList<>();
    for (PlacedComponent p : layout.placements()) {
      if (PIG_TYPES.contains(p.componentType())) {pigPlacements.add(p);
      } else {structuralPlacements.add(p);}
    }
    Map<String, GameComponent> placed = new HashMap<>();
    Map<String, String> placedTypes = new HashMap<>();

    for (PlacedComponent p : structuralPlacements) {
      double worldX = startX + p.col() * CELL_SIZE;
      double worldY = startY + p.row() * CELL_SIZE;
      GameComponent component = createComponent(p.componentType(), worldX, worldY);
      world.addComponent(component);
      String key = toKey(p.row(), p.col());
      placed.put(key, component);
      placedTypes.put(key, p.componentType());
    }

    Set<String> edgeKeys = new HashSet<>();
    for (PlacedComponent p : structuralPlacements) {
      GameComponent current = placed.get(toKey(p.row(), p.col()));
      GameComponent right = placed.get(toKey(p.row(), p.col() + 1));
      if (right != null) {
        String rightType = placedTypes.getOrDefault(toKey(p.row(), p.col() + 1), "");
        world.addJoint(buildJoint(current, right, p.componentType(), rightType));
        addWeldStabilizerIfRigidPair(world, current, right, p.componentType(), rightType, true);
        edgeKeys.add(canonicalPairKey(toKey(p.row(), p.col()), toKey(p.row(), p.col() + 1)));
      }
      GameComponent below = placed.get(toKey(p.row() + 1, p.col()));
      if (below != null) {
        String belowType = placedTypes.getOrDefault(toKey(p.row() + 1, p.col()), "");
        world.addJoint(buildJoint(current, below, p.componentType(), belowType));
        addWeldStabilizerIfRigidPair(world, current, below, p.componentType(), belowType, false);
        edgeKeys.add(canonicalPairKey(toKey(p.row(), p.col()), toKey(p.row() + 1, p.col())));
      }
    }
    addRightAngleRigidChords(structuralPlacements, world, placed, placedTypes, edgeKeys);
    addAntiRotationJoints(structuralPlacements, world, placed, placedTypes, edgeKeys);
    attachPigsToHostCells(pigPlacements, placed, world, startX, startY);
  }

  private void attachPigsToHostCells(List<PlacedComponent> pigPlacements,
      Map<String, GameComponent> placed, World world, double startX, double startY) {
    for (PlacedComponent p : pigPlacements) {
      double worldX = startX + p.col() * CELL_SIZE;
      double worldY = startY + p.row() * CELL_SIZE;
      GameComponent pig = createComponent(p.componentType(), worldX, worldY);
      world.addComponent(pig);
      GameComponent host = placed.get(toKey(p.row(), p.col()));
      if (host != null) {
        world.addJoint(new RigidJoint(pig, host));
      }
    }
  }

  private Joint buildJoint(GameComponent a, GameComponent b, String typeA, String typeB) {
    Vector posA = a.getState().getLinearPosition();
    Vector posB = b.getState().getLinearPosition();
    Vector delta = posB.subtract(posA);

    if (WHEEL_TYPES.contains(typeA) || WHEEL_TYPES.contains(typeB)) {
      double halfHa = halfHeight(typeA);
      double halfHb = halfHeight(typeB);
      Vector localAnchorA;
      Vector localAnchorB;
      if (Math.abs(delta.x()) < 1e-6) {
        double dir = Math.signum(delta.y());
        localAnchorA = new Vector(0, dir * halfHa);
        localAnchorB = new Vector(0, -dir * halfHb);
      } else {
        double dir = Math.signum(delta.x());
        localAnchorA = new Vector(dir * halfWidth(typeA), 0);
        localAnchorB = new Vector(-dir * halfWidth(typeB), 0);
      }return new RevoluteJoint(a, b, localAnchorA, localAnchorB);
    }

    Vector localAnchorA;
    Vector localAnchorB;
    if (Math.abs(delta.y()) < 1e-6) {
      double dir = Math.signum(delta.x());
      localAnchorA = new Vector(dir * halfWidth(typeA), 0);
      localAnchorB = new Vector(-dir * halfWidth(typeB), 0);
    } else if (Math.abs(delta.x()) < 1e-6) {
      double dir = Math.signum(delta.y());
      localAnchorA = new Vector(0, dir * halfHeight(typeA));
      localAnchorB = new Vector(0, -dir * halfHeight(typeB));
    } else {
      localAnchorA = new Vector(0, 0);
      localAnchorB = new Vector(0, 0);
    }
    return new RigidJoint(a, b, localAnchorA, localAnchorB, -1.0, -1.0);
  }

  /**
   * For each filled cell, if two perpendicular cardinal neighbors exist and both arms use rigid
   * distance joints (no wheel on either side of that arm), adds the closing joint between the two
   * outer cells — the hypotenuse of the isosceles right triangle.
   */
  private void addRightAngleRigidChords(List<PlacedComponent> placements, World world,
      Map<String, GameComponent> placed, Map<String, String> placedTypes,
      Set<String> edgeKeys) {
    for (PlacedComponent p : placements) {
      int r = p.row();
      int c = p.col();
      tryAddRigidTriangleChord(r, c, r, c + 1, r + 1, c, placed, placedTypes, world,
          edgeKeys);
      tryAddRigidTriangleChord(r, c, r, c - 1, r + 1, c, placed, placedTypes, world,
          edgeKeys);
      tryAddRigidTriangleChord(r, c, r, c + 1, r - 1, c, placed, placedTypes, world,
          edgeKeys);
      tryAddRigidTriangleChord(r, c, r, c - 1, r - 1, c, placed, placedTypes, world,
          edgeKeys);
    }
  }

  private void tryAddRigidTriangleChord(int cornerR, int cornerC, int n1r, int n1c, int n2r,
      int n2c,
      Map<String, GameComponent> placed, Map<String, String> placedTypes, World world,
      Set<String> edgeKeys) {
    String kCorner = toKey(cornerR, cornerC);
    String k1 = toKey(n1r, n1c);
    String k2 = toKey(n2r, n2c);
    if (!placed.containsKey(k1) || !placed.containsKey(k2)) {return;}
    String tCorner = placedTypes.get(kCorner);
    String t1 = placedTypes.get(k1);
    String t2 = placedTypes.get(k2);
    if (tCorner == null || t1 == null || t2 == null) {return;}
    if (!isRigidCardinalPair(tCorner, t1) || !isRigidCardinalPair(tCorner, t2)) {return;}
    RigidJoint.registerRigidTriangle(world, placed.get(kCorner), placed.get(k1), placed.get(k2),edgeKeys, kCorner, k1, k2);
  }

  private boolean isRigidCardinalPair(String typeA, String typeB) {
    return !WHEEL_TYPES.contains(typeA) && !WHEEL_TYPES.contains(typeB);
  }

  private void addWeldStabilizerIfRigidPair(World world, GameComponent a, GameComponent b,
      String typeA, String typeB, boolean horizontalNeighbor) {
    if (!isRigidCardinalPair(typeA, typeB)) {return;}
    double offset = horizontalNeighbor
        ? 0.35 * Math.min(halfHeight(typeA), halfHeight(typeB))
        : 0.35 * Math.min(halfWidth(typeA), halfWidth(typeB));
    Vector anchorA = horizontalNeighbor ? new Vector(0, offset) : new Vector(offset, 0);
    Vector anchorB = horizontalNeighbor ? new Vector(0, offset) : new Vector(offset, 0);
    world.addJoint(new RigidJoint(a, b, anchorA, anchorB, -1.0, -1.0));
  }

  private double halfWidth(String componentType) {return paletteDimension(componentType, "width") / 2.0;}

  private double halfHeight(String componentType) {return paletteDimension(componentType, "height") / 2.0;}

  private double paletteDimension(String componentType, String axis) {
    String key = componentType.replace(" ", "");
    String paletteKey = key + "." + axis;
    if (PALETTE.containsKey(paletteKey)) {
      return Double.parseDouble(PALETTE.getString(paletteKey));
    }
    return 40.0;
  }

  private void addAntiRotationJoints(List<PlacedComponent> placements, World world,
      Map<String, GameComponent> placed,
      Map<String, String> placedTypes, Set<String> edgeKeys) {
    int[][] offsets = {{0, 2}, {2, 0}, {1, 1}, {1, -1}};

    for (PlacedComponent component : placements) {
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
        if (!edgeKeys.add(pairKey)) {
          continue;
        }
        world.addJoint(new RigidJoint(placed.get(startKey), placed.get(targetKey)));
      }
    }
  }

  private boolean hasSupportCell(int rowA, int colA, int rowB, int colB,
      Map<String, String> placedTypes) {
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

  // Thrust direction is in +y-down world coords, so (0, -1) means "pushes up".
  // Magnitudes relative to drive mass × gravity (5 × 400 = 2000): lift types must exceed 2000.
  private static void configureThrust(DriveComponent drive, String componentType) {
    switch (componentType) {
      case "Engine" -> {
        drive.setThrustDirection(new Vector(1, 0));
        drive.setThrustForce(3000.0);
      }
      case "Fan" -> {
        drive.setThrustDirection(new Vector(1, 0));
        drive.setThrustForce(1500.0);
      }
      case "Balloon" -> {
        drive.setThrustDirection(new Vector(0, -1));
        drive.setThrustForce(2500.0);
      }
      case "Rocket" -> {
        drive.setThrustDirection(new Vector(0, -1));
        drive.setThrustForce(4500.0);
      }
      case "Propeller" -> {
        drive.setThrustDirection(new Vector(1, 0));
        drive.setThrustForce(2000.0);
      }
      default -> { /* keep DriveComponent constructor defaults */ }
    }
  }

  private GameComponent createComponent(String componentType, double x, double y) {
    String key = componentType.replace(" ", "");
    double width = PALETTE.containsKey(key + ".width")
        ? Double.parseDouble(PALETTE.getString(key + ".width")) : 40.0;
    double height = PALETTE.containsKey(key + ".height")
        ? Double.parseDouble(PALETTE.getString(key + ".height")) : 40.0;

    switch (componentType) {
      case "Pig":
      case "Small Pig":
        return new PigComponent(x, y, 0, DEFAULT_MASS_PIG, DEFAULT_FRICTION, componentType);
      case "Engine":
      case "Fan":
      case "Balloon":
      case "Rocket":
      case "Propeller":
        DriveComponent drive = new DriveComponent(x, y, 0, DEFAULT_MASS_DRIVE, DEFAULT_FRICTION,
            DEFAULT_MAX_HP, componentType, width, height);
        configureThrust(drive, componentType);
        return drive;
      default:
        return new StructuralComponent(x, y, 0, DEFAULT_MASS_STRUCTURAL, DEFAULT_FRICTION,
            DEFAULT_MAX_HP, componentType, width, height);
    }
  }
}
