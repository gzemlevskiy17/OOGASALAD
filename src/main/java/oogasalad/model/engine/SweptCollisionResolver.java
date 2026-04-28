package oogasalad.model.engine;

import java.util.HashMap;
import java.util.Map;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.DynamicComponent;
import oogasalad.model.engine.internal.components.StaticComponent;

/**
 * Detects and resolves tunnelling of dynamic components through static geometry using continuous
 * collision detection (swept AABB tests).
 *
 * @author Paulina Vvedenskaya
 */
class SweptCollisionResolver {

  private static final double AABB_MARGIN = 1e-6;
  private static final double CCD_SKIN = 1e-4;

  private final World world;

  SweptCollisionResolver(World world) {
    this.world = world;
  }

  Map<DynamicComponent, MotionSnapshot> captureMotion() {
    Map<DynamicComponent, MotionSnapshot> motion = new HashMap<>();
    for (GameObject object : world.getObjects()) {
      if (object instanceof DynamicComponent dynamicComponent) {
        motion.put(dynamicComponent, new MotionSnapshot(
            BoundingBox.from(dynamicComponent),
            dynamicComponent.getStaticState().getLinearPosition()));
      }
    }
    return motion;
  }

  void resolve(Map<DynamicComponent, MotionSnapshot> previousMotion) {
    for (GameObject dynamicObject : world.getObjects()) {
      if (!(dynamicObject instanceof DynamicComponent dynamicComponent)) {
        continue;
      }
      resolveForComponent(dynamicComponent, previousMotion);
    }
  }

  private void resolveForComponent(DynamicComponent dynamicComponent,
      Map<DynamicComponent, MotionSnapshot> previousMotion) {
    MotionSnapshot start = previousMotion.get(dynamicComponent);
    if (start == null) {
      return;
    }
    BoundingBox end = BoundingBox.from(dynamicComponent);
    Vector displacement = end.center().subtract(start.bounds().center());
    SweptHit earliestHit = findEarliestStaticHit(dynamicComponent, start.bounds(), end,
        displacement);
    if (earliestHit != null) {
      applySweptHit(dynamicComponent, start.position(), displacement, earliestHit);
    }
  }

  private SweptHit findEarliestStaticHit(DynamicComponent dynamicComponent,
      BoundingBox startBounds, BoundingBox endBounds, Vector displacement) {
    SweptHit earliestHit = null;
    for (GameObject staticObject : world.getObjects()) {
      if (staticObject == dynamicComponent || !(staticObject instanceof StaticComponent)) {
        continue;
      }
      BoundingBox staticBounds = BoundingBox.from(staticObject);
      if (endBounds.overlaps(staticBounds)) {
        continue;
      }
      SweptHit hit = sweepAgainstStatic(startBounds, staticBounds, displacement);
      if (hit != null && (earliestHit == null || hit.time() < earliestHit.time())) {
        earliestHit = hit;
      }
    }
    return earliestHit;
  }

  private SweptHit sweepAgainstStatic(BoundingBox movingStart, BoundingBox staticBounds,
      Vector displacement) {
    AxisSweep xSweep = sweepAxis(movingStart.minX(), movingStart.maxX(), staticBounds.minX(),
        staticBounds.maxX(), displacement.x());
    AxisSweep ySweep = sweepAxis(movingStart.minY(), movingStart.maxY(), staticBounds.minY(),
        staticBounds.maxY(), displacement.y());
    if (!xSweep.canHit() || !ySweep.canHit()) {
      return null;
    }

    double entryTime = Math.max(xSweep.entryTime(), ySweep.entryTime());
    double exitTime = Math.min(xSweep.exitTime(), ySweep.exitTime());
    if (entryTime < 0.0 || entryTime > 1.0 || entryTime > exitTime) {
      return null;
    }

    Vector normal;
    if (xSweep.entryTime() > ySweep.entryTime()) {
      normal = new Vector(-Math.signum(displacement.x()), 0);
    } else {
      normal = new Vector(0, -Math.signum(displacement.y()));
    }
    return new SweptHit(entryTime, normal);
  }

  private AxisSweep sweepAxis(double movingMin, double movingMax, double staticMin,
      double staticMax, double delta) {
    if (Math.abs(delta) <= AABB_MARGIN) {
      boolean alreadyOverlapping = movingMax >= staticMin && staticMax >= movingMin;
      if (alreadyOverlapping) {
        return new AxisSweep(true, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
      }
      return new AxisSweep(false, 0.0, 0.0);
    }
    double entryDistance;
    double exitDistance;
    if (delta > 0.0) {
      entryDistance = staticMin - movingMax;
      exitDistance = staticMax - movingMin;
    } else {
      entryDistance = staticMax - movingMin;
      exitDistance = staticMin - movingMax;
    }
    return new AxisSweep(true, entryDistance / delta, exitDistance / delta);
  }

  private void applySweptHit(DynamicComponent component, Vector startCenter, Vector displacement,
      SweptHit hit) {
    double safeTime = sweptContactTime(hit);
    component.getStaticState().setLinearPosition(startCenter.add(displacement.multiply(safeTime)));
    Vector velocity = component.getDynamicState().getLinearVelocity();
    double normalVelocity = velocity.dot(hit.normal());
    if (normalVelocity < 0.0 && isHorizontalHit(hit)) {
      component.getDynamicState().setLinearVelocity(
          velocity.subtract(hit.normal().multiply(normalVelocity)));
    }
    component.updatePolygon();
  }

  private double sweptContactTime(SweptHit hit) {
    double skinDirection;
    if (isHorizontalHit(hit)) {
      skinDirection = -CCD_SKIN;
    } else {
      skinDirection = CCD_SKIN;
    }
    return Math.max(0.0, Math.min(1.0, hit.time() + skinDirection));
  }

  private boolean isHorizontalHit(SweptHit hit) {
    return Math.abs(hit.normal().x()) > Math.abs(hit.normal().y());
  }

  record BoundingBox(double minX, double minY, double maxX, double maxY) {

    static BoundingBox from(GameObject object) {
      double minX = Double.POSITIVE_INFINITY;
      double minY = Double.POSITIVE_INFINITY;
      double maxX = Double.NEGATIVE_INFINITY;
      double maxY = Double.NEGATIVE_INFINITY;
      for (ConvexPolygon polygon : object.getCollisionPolygons()) {
        for (Vector point : polygon.points()) {
          minX = Math.min(minX, point.x());
          minY = Math.min(minY, point.y());
          maxX = Math.max(maxX, point.x());
          maxY = Math.max(maxY, point.y());
        }
      }
      return new BoundingBox(minX, minY, maxX, maxY);
    }

    boolean overlaps(BoundingBox other) {
      return maxX + AABB_MARGIN >= other.minX
          && other.maxX + AABB_MARGIN >= minX
          && maxY + AABB_MARGIN >= other.minY
          && other.maxY + AABB_MARGIN >= minY;
    }

    Vector center() {
      return new Vector((minX + maxX) / 2.0, (minY + maxY) / 2.0);
    }
  }

  record AxisSweep(boolean canHit, double entryTime, double exitTime) {

  }

  record SweptHit(double time, Vector normal) {

  }

  record MotionSnapshot(BoundingBox bounds, Vector position) {

  }
}
