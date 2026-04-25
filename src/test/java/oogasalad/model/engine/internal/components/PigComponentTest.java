package oogasalad.model.engine.internal.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.api.DefaultTag;
import oogasalad.model.engine.api.TypedComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PigComponentTest {

  private PigComponent pig;

  @BeforeEach
  void setUp() {
    double w = 40.0;
    double h = 40.0;
    double mass = 5.0;
    pig = new PigComponent(
        100.0,
        200.0,
        0.0,
        ConvexPolygon.axisAlignedBox(100.0, 200.0, w, h),
        "",
        0.5,
        mass,
        mass * (w * w + h * h) / 12.0,
        0.5,
        true,
        100.0,
        "Pig");
  }

  @Test
  void pig_isTypedComponent() {
    assertInstanceOf(TypedComponent.class, pig);
  }

  @Test
  void getComponentType_returnsPig() {
    assertEquals("Pig", pig.getComponentType());
  }

  @Test
  void constructor_tagsComponentAsPigs() {
    assertEquals(DefaultTag.PIGS.value(), pig.getTag());
  }

  @Test
  void updatePolygon_centersBoxAtPosition() {
    pig.updatePolygon();
    var points = pig.getPolygon().getPoints();
    double minX = points.stream().mapToDouble(p -> p.x()).min().orElseThrow();
    double maxX = points.stream().mapToDouble(p -> p.x()).max().orElseThrow();
    double minY = points.stream().mapToDouble(p -> p.y()).min().orElseThrow();
    double maxY = points.stream().mapToDouble(p -> p.y()).max().orElseThrow();
    // 40x40 box centered at (100, 200) → corners at (80, 180) .. (120, 220)
    assertEquals(80.0, minX, 1e-9);
    assertEquals(180.0, minY, 1e-9);
    assertEquals(120.0, maxX, 1e-9);
    assertEquals(220.0, maxY, 1e-9);
  }
}
