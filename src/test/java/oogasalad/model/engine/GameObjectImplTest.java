package oogasalad.model.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import oogasalad.model.engine.api.DefaultTag;
import oogasalad.model.engine.api.Vector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GameObjectImplTest {

  private GameObjectImpl gameObject;

  @BeforeEach
  void setUp() {
    gameObject = new GameObjectImpl();
  }

  @Test
  void massForceInertiaAndRestitution_roundTripThroughSetters() {
    Vector force = new Vector(3.0, -2.0);

    gameObject.setMass(5.5);
    gameObject.setForce(force);
    gameObject.setMomentOfInertia(1.25);
    gameObject.setRestitution(0.35);

    assertEquals(5.5, gameObject.getMass(), 1e-9);
    assertSame(force, gameObject.getForce());
    assertEquals(1.25, gameObject.getMomentOfInertia(), 1e-9);
    assertEquals(0.35, gameObject.getRestitution(), 1e-9);
  }

  @Test
  void state_roundTripsThroughSetterAndGetter() {
    ObjectState state = new ObjectState(
        new Vector(7.0, 11.0),
        new Vector(-1.0, 4.0),
        new Vector(0.5, -0.25),
        0.75,
        2.0,
        1.0);

    gameObject.setState(state);

    assertSame(state, gameObject.getState());
  }

  @Test
  void renderCoordinates_reflectLinearPositionStoredInState() {
    gameObject.setState(new ObjectState(
        new Vector(12.5, -3.25),
        new Vector(0.0, 0.0),
        new Vector(0.0, 0.0),
        1.5,
        0.0,
        0.0));

    assertEquals(12.5, gameObject.getX(), 1e-9);
    assertEquals(-3.25, gameObject.getY(), 1e-9);
  }

  @Test
  void placeholderInterfaceMethods_matchCurrentImplementation() {
    assertEquals(DefaultTag.GAME_OBJECT.value(), gameObject.getTag());
    assertNull(gameObject.getPolygon());
    assertEquals(0.0, gameObject.getFriction(), 1e-9);
    assertFalse(gameObject.isActive());
    assertFalse(gameObject.isAffectedByGravity());
    assertFalse(gameObject.isTrigger());
    assertEquals(0.0, gameObject.getRotation(), 1e-9);
  }

  @Test
  void tag_roundTripsThroughSetter() {
    gameObject.setTag("custom-author-tag");
    assertEquals("custom-author-tag", gameObject.getTag());
  }

  @Test
  void setTag_rejectsNull() {
    assertThrows(NullPointerException.class, () -> gameObject.setTag(null));
  }
}
