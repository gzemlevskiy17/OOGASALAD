package oogasalad.view.authoring.internal.game;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SpriteImportServiceTest {

  @Test
  void normalizeSpritePath_replacesPlatformSeparatorsWithForwardSlashes() {
    String normalized = SpriteImportService.normalizeSpritePath(
        Path.of("data", "assets", "vehicle", "sprite.png"));

    assertFalse(normalized.contains("\\"));
  }
}
