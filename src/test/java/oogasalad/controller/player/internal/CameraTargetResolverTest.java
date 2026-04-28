package oogasalad.controller.player.internal;

import static org.junit.jupiter.api.Assertions.assertSame;

import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.WorldImpl;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.FixedDirectionDriveComponent;
import oogasalad.model.engine.internal.components.PigComponent;
import org.junit.jupiter.api.Test;

class CameraTargetResolverTest {

  private final CameraTargetResolver resolver = new CameraTargetResolver();

  @Test
  void resolve_prefersConfiguredDisplayName() {
    WorldImpl world = new WorldImpl();
    FixedDirectionDriveComponent fan = new FixedDirectionDriveComponent(20, 0, 0, 5.0, 0.3, 100.0,
        "Fan", 60.0, 60.0, new Vector(1, 0));
    PigComponent pig = new PigComponent(0, 0, 0,
        ConvexPolygon.axisAlignedBox(-20, -20, 40, 40),
        "", 0, 1.0, 1.0, 0.1, true, 100, "pig");
    world.addComponent(pig);
    world.addComponent(fan);

    GameObject target = resolver.resolve(world, "fan");

    assertSame(fan, target);
  }

  @Test
  void resolve_unknownConfiguredTargetFallsBackToPig() {
    WorldImpl world = new WorldImpl();
    PigComponent pig = new PigComponent(0, 0, 0,
        ConvexPolygon.axisAlignedBox(-20, -20, 40, 40),
        "", 0, 1.0, 1.0, 0.1, true, 100, "pig");
    FixedDirectionDriveComponent fan = new FixedDirectionDriveComponent(20, 0, 0, 5.0, 0.3, 100.0,
        "Fan", 60.0, 60.0, new Vector(1, 0));
    world.addComponent(pig);
    world.addComponent(fan);

    GameObject target = resolver.resolve(world, "missing");

    assertSame(pig, target);
  }
}
