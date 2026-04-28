package oogasalad.controller.player.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import oogasalad.controller.player.internal.parts.JointStyle;
import oogasalad.controller.player.internal.parts.VehiclePartRegistry;
import oogasalad.controller.player.internal.parts.VehiclePartSpec;
import oogasalad.model.engine.api.Joint;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.RigidBody;
import oogasalad.model.engine.internal.joints.RevoluteJoint;
import oogasalad.model.engine.internal.joints.RigidJoint;
import oogasalad.model.player.api.PlacedComponent;
import oogasalad.model.player.api.VehicleLayout;

/**
 * Translates a VehicleLayout into RigidBody instances and joints in the World. Cardinal
 * neighbors (right / below) get {@link RevoluteJoint} (wheels) or {@link RigidJoint} (non-wheels),
 * dispatched by each part's {@link JointStyle}.
 *
 * @author Paulina Vvedenskaya
 */
public class VehicleFactory {

  static final double CELL_SIZE = 60.0;

  private final VehiclePartRegistry registry;

  public VehicleFactory() {
    this(new VehiclePartRegistry());
  }

  public VehicleFactory(VehiclePartRegistry registry) {
    this.registry = registry;
  }

  public VehiclePartRegistry getRegistry() {
    return registry;
  }

  /**
   * Builds the vehicle and returns each placement mapped to its created component, in placement
   * order (structurals first, pigs last), so HUD/UI code can look up per-cell HP without
   * re-deriving the mapping. Returned map is a {@link LinkedHashMap} so iteration order is
   * deterministic.
   */
  public Map<PlacedComponent, RigidBody> buildVehicle(VehicleLayout layout, World world,
      double startX, double startY) {
    List<PlacedComponent> structuralPlacements = new ArrayList<>();
    List<PlacedComponent> pigPlacements = new ArrayList<>();
    for (PlacedComponent p : layout.placements()) {
      if (registry.get(p.componentType()).jointStyle() == JointStyle.PIG) {
        pigPlacements.add(p);
      } else {
        structuralPlacements.add(p);
      }
    }
    Map<String, RigidBody> placed = new HashMap<>();
    Map<String, VehiclePartSpec> specByKey = new HashMap<>();
    Map<PlacedComponent, RigidBody> placementToComponent = new LinkedHashMap<>();

    for (PlacedComponent p : structuralPlacements) {
      double worldX = startX + p.col() * CELL_SIZE;
      double worldY = startY + p.row() * CELL_SIZE;
      VehiclePartSpec spec = registry.get(p.componentType());
      RigidBody component = spec.create(worldX, worldY);
      world.addComponent(component);
      String key = toKey(p.row(), p.col());
      placed.put(key, component);
      specByKey.put(key, spec);
      placementToComponent.put(p, component);
    }

    Set<String> edgeKeys = new HashSet<>();
    for (PlacedComponent p : structuralPlacements) {
      String currentKey = toKey(p.row(), p.col());
      RigidBody current = placed.get(currentKey);
      VehiclePartSpec currentSpec = specByKey.get(currentKey);
      addCardinalNeighbor(world, placed, specByKey, edgeKeys,
          current, currentSpec, currentKey, toKey(p.row(), p.col() + 1), true);
      addCardinalNeighbor(world, placed, specByKey, edgeKeys,
          current, currentSpec, currentKey, toKey(p.row() + 1, p.col()), false);
    }
    addRightAngleRigidChords(structuralPlacements, world, placed, specByKey, edgeKeys);
    addAntiRotationJoints(structuralPlacements, world, placed, specByKey, edgeKeys);
    attachPigsToHostCells(pigPlacements, placed, world, startX, startY, placementToComponent);
    return placementToComponent;
  }

  private void addCardinalNeighbor(World world, Map<String, RigidBody> placed,
      Map<String, VehiclePartSpec> specByKey, Set<String> edgeKeys,
      RigidBody current, VehiclePartSpec currentSpec, String currentKey,
      String neighborKey, boolean horizontalNeighbor) {
    RigidBody neighbor = placed.get(neighborKey);
    if (neighbor == null) {
      return;
    }
    VehiclePartSpec neighborSpec = specByKey.get(neighborKey);
    world.addJoint(buildJoint(current, neighbor, currentSpec, neighborSpec));
    addWeldStabilizerIfRigidPair(world, current, neighbor, currentSpec, neighborSpec,
        horizontalNeighbor);
    edgeKeys.add(canonicalPairKey(currentKey, neighborKey));
  }

  private void attachPigsToHostCells(List<PlacedComponent> pigPlacements,
      Map<String, RigidBody> placed, World world, double startX, double startY,
      Map<PlacedComponent, RigidBody> placementToComponent) {
    for (PlacedComponent p : pigPlacements) {
      double worldX = startX + p.col() * CELL_SIZE;
      double worldY = startY + p.row() * CELL_SIZE;
      RigidBody pig = registry.get(p.componentType()).create(worldX, worldY);
      world.addComponent(pig);
      placementToComponent.put(p, pig);
      RigidBody host = resolvePigHost(p, placed);
      if (host == null) {
        throw new IllegalStateException(
            "No vehicle part to attach pig part '%s' at grid (%d,%d); layout is invalid"
                .formatted(p.componentType(), p.row(), p.col()));
      }
      world.addJoint(new RigidJoint(pig, host));
    }
  }

  /**
   * Same-cell structural host first; else first cardinal neighbor with a part (N, E, S, W).
   */
  private RigidBody resolvePigHost(PlacedComponent p, Map<String, RigidBody> placed) {
    RigidBody host = placed.get(toKey(p.row(), p.col()));
    if (host != null) {
      return host;
    }
    int[][] deltas = {{-1, 0}, {0, 1}, {1, 0}, {0, -1}};
    for (int[] d : deltas) {
      host = placed.get(toKey(p.row() + d[0], p.col() + d[1]));
      if (host != null) {
        return host;
      }
    }
    return null;
  }

  private Joint buildJoint(RigidBody a, RigidBody b,
      VehiclePartSpec specA, VehiclePartSpec specB) {
    Vector posA = a.getStaticState().getLinearPosition();
    Vector posB = b.getStaticState().getLinearPosition();
    Vector delta = posB.subtract(posA);

    boolean wheelInvolved = specA.jointStyle() == JointStyle.WHEEL
        || specB.jointStyle() == JointStyle.WHEEL;

    if (wheelInvolved) {
      RigidBody wheel = specA.jointStyle() == JointStyle.WHEEL ? a : b;
      RigidBody chassis = wheel == a ? b : a;
      Vector wheelPosition = wheel.getStaticState().getLinearPosition();
      Vector chassisPosition = chassis.getStaticState().getLinearPosition();
      return new RevoluteJoint(wheel, chassis, new Vector(0, 0),
          wheelPosition.subtract(chassisPosition));
    }

    Vector localAnchorA;
    Vector localAnchorB;
    if (Math.abs(delta.y()) < 1e-6) {
      double dir = Math.signum(delta.x());
      localAnchorA = new Vector(dir * specA.bodyWidth() / 2.0, 0);
      localAnchorB = new Vector(-dir * specB.bodyWidth() / 2.0, 0);
    } else if (Math.abs(delta.x()) < 1e-6) {
      double dir = Math.signum(delta.y());
      localAnchorA = new Vector(0, dir * specA.bodyHeight() / 2.0);
      localAnchorB = new Vector(0, -dir * specB.bodyHeight() / 2.0);
    } else {
      localAnchorA = new Vector(0, 0);
      localAnchorB = new Vector(0, 0);
    }
    return new RigidJoint(a, b, localAnchorA, localAnchorB, -1.0, -1.0);
  }

  private void addRightAngleRigidChords(List<PlacedComponent> placements, World world,
      Map<String, RigidBody> placed, Map<String, VehiclePartSpec> specByKey,
      Set<String> edgeKeys) {
    for (PlacedComponent p : placements) {
      int r = p.row();
      int c = p.col();
      tryAddRigidTriangleChord(r, c, r, c + 1, r + 1, c, placed, specByKey, world, edgeKeys);
      tryAddRigidTriangleChord(r, c, r, c - 1, r + 1, c, placed, specByKey, world, edgeKeys);
      tryAddRigidTriangleChord(r, c, r, c + 1, r - 1, c, placed, specByKey, world, edgeKeys);
      tryAddRigidTriangleChord(r, c, r, c - 1, r - 1, c, placed, specByKey, world, edgeKeys);
    }
  }

  private void tryAddRigidTriangleChord(int cornerR, int cornerC, int n1r, int n1c, int n2r,
      int n2c, Map<String, RigidBody> placed, Map<String, VehiclePartSpec> specByKey,
      World world, Set<String> edgeKeys) {
    String kCorner = toKey(cornerR, cornerC);
    String k1 = toKey(n1r, n1c);
    String k2 = toKey(n2r, n2c);
    if (!placed.containsKey(k1) || !placed.containsKey(k2)) {
      return;
    }
    if (!isStructuralPair(specByKey.get(kCorner), specByKey.get(k1))
        || !isStructuralPair(specByKey.get(kCorner), specByKey.get(k2))) {
      return;
    }
    RigidJoint.registerRigidTriangle(world, placed.get(kCorner), placed.get(k1), placed.get(k2),
        edgeKeys, kCorner, k1, k2);
  }

  private boolean isStructuralPair(VehiclePartSpec a, VehiclePartSpec b) {
    return a != null && b != null
        && a.jointStyle() == JointStyle.STRUCTURAL
        && b.jointStyle() == JointStyle.STRUCTURAL;
  }

  private void addWeldStabilizerIfRigidPair(World world, RigidBody a, RigidBody b,
      VehiclePartSpec specA, VehiclePartSpec specB, boolean horizontalNeighbor) {
    if (!isStructuralPair(specA, specB)) {
      return;
    }
    double offset = horizontalNeighbor
        ? 0.35 * Math.min(specA.bodyHeight() / 2.0, specB.bodyHeight() / 2.0)
        : 0.35 * Math.min(specA.bodyWidth() / 2.0, specB.bodyWidth() / 2.0);
    Vector anchorA = horizontalNeighbor ? new Vector(0, offset) : new Vector(offset, 0);
    Vector anchorB = horizontalNeighbor ? new Vector(0, offset) : new Vector(offset, 0);
    world.addJoint(new RigidJoint(a, b, anchorA, anchorB, -1.0, -1.0));
  }

  private void addAntiRotationJoints(List<PlacedComponent> placements, World world,
      Map<String, RigidBody> placed, Map<String, VehiclePartSpec> specByKey,
      Set<String> edgeKeys) {
    int[][] offsets = {{0, 2}, {2, 0}, {1, 1}, {1, -1}};

    for (PlacedComponent component : placements) {
      for (int[] offset : offsets) {
        int targetRow = component.row() + offset[0];
        int targetCol = component.col() + offset[1];
        String startKey = toKey(component.row(), component.col());
        String targetKey = toKey(targetRow, targetCol);
        if (!placed.containsKey(targetKey)
            || !isStructuralPair(specByKey.get(startKey), specByKey.get(targetKey))) {
          continue;
        }
        if (!hasStructuralSupportCell(component.row(), component.col(), targetRow, targetCol,
            specByKey)) {
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

  private boolean hasStructuralSupportCell(int rowA, int colA, int rowB, int colB,
      Map<String, VehiclePartSpec> specByKey) {
    int rowDelta = Math.abs(rowA - rowB);
    int colDelta = Math.abs(colA - colB);

    if (rowDelta == 2 && colDelta == 0) {
      return isStructural(specByKey.get(toKey((rowA + rowB) / 2, colA)));
    }
    if (rowDelta == 0 && colDelta == 2) {
      return isStructural(specByKey.get(toKey(rowA, (colA + colB) / 2)));
    }
    if (rowDelta == 1 && colDelta == 1) {
      return isStructural(specByKey.get(toKey(rowA, colB)))
          || isStructural(specByKey.get(toKey(rowB, colA)));
    }
    return false;
  }

  private boolean isStructural(VehiclePartSpec spec) {
    return spec != null && spec.jointStyle() == JointStyle.STRUCTURAL;
  }

  private String canonicalPairKey(String keyA, String keyB) {
    return keyA.compareTo(keyB) < 0 ? keyA + "|" + keyB : keyB + "|" + keyA;
  }

  private String toKey(int row, int col) {
    return row + "," + col;
  }
}
