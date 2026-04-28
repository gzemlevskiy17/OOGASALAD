package oogasalad.view.authoring.internal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import oogasalad.data.api.ButtonInfo;
import oogasalad.data.api.CollisionAction;
import oogasalad.data.api.ComponentInfo;
import oogasalad.data.api.StaticComponentInfo;
import oogasalad.data.api.TerrainInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GameCatalogValidatorTest {

  private GameCatalogDraft draft;
  private GameCatalogValidator validator;

  @BeforeEach
  void setUp() {
    draft = new GameCatalogDraft();
    validator = new GameCatalogValidator();
    draft.components().add(new ComponentInfo("body", "data/assets/body.png", 1.0,
        List.of(new ButtonInfo("drive", 1.0, 0.0))));
    draft.staticComponents().add(new StaticComponentInfo("coin", "data/assets/coin.png", 0.0,
        new CollisionAction("score", 10.0)));
    draft.terrainTypes().add(new TerrainInfo("grass", "", "#00aa00", 0.7));
  }

  @Test
  void validate_validCatalog_doesNotThrow() {
    assertDoesNotThrow(() -> validator.validate("game", "body", draft));
  }

  @Test
  void validate_zeroVehicleComponentMass_throws() {
    draft.components().clear();
    draft.components().add(new ComponentInfo("body", "", 0.0, List.of()));

    assertThrows(IllegalArgumentException.class,
        () -> validator.validate("game", "body", draft));
  }

  @Test
  void validate_duplicateStaticIds_throws() {
    draft.staticComponents().add(new StaticComponentInfo("coin", "", 0.0, null));

    assertThrows(IllegalArgumentException.class,
        () -> validator.validate("game", "body", draft));
  }

  @Test
  void validate_unsupportedStaticAction_throws() {
    draft.staticComponents().clear();
    draft.staticComponents().add(new StaticComponentInfo("mystery", "", 0.0,
        new CollisionAction("teleport", 1.0)));

    assertThrows(IllegalArgumentException.class,
        () -> validator.validate("game", "body", draft));
  }

  @Test
  void validate_blankMainCharacter_throws() {
    assertThrows(IllegalArgumentException.class,
        () -> validator.validate("game", "", draft));
  }

  @Test
  void validate_gameIdWithPathSeparator_throws() {
    assertThrows(IllegalArgumentException.class,
        () -> validator.validate("../game", "body", draft));
  }
}
