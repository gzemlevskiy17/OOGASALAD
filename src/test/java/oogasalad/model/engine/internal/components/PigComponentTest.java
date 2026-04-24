package oogasalad.model.engine.internal.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import oogasalad.model.engine.api.DefaultTag;
import oogasalad.view.core.api.SpriteRenderable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PigComponentTest {

  private PigComponent pig;

  @BeforeEach
  void setUp() {
    pig = new PigComponent(100.0, 200.0, 0.0, 5.0, 0.5);
  }

  @Test
  void pig_isSpriteRenderable() {
    assertInstanceOf(SpriteRenderable.class, pig);
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
