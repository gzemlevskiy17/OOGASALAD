package oogasalad.view.core.internal;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for SpriteCache miss paths and idempotent null returns for missing assets. */
class SpriteCacheTest {

  private SpriteCache cache;

  @BeforeEach
  void setUp() {
    cache = new SpriteCache();
  }

  @Test
  void get_nonExistentPath_returnsNull() {
    assertNull(cache.get("does/not/exist.png"));
  }

  @Test
  void get_repeatedMissingPath_alwaysReturnsNull() {
    assertNull(cache.get("ghost.png"));
    assertNull(cache.get("ghost.png"));
    assertNull(cache.get("ghost.png"));
  }

  @Test
  void get_twoDifferentMissingPaths_bothReturnNull() {
    assertNull(cache.get("path/a.png"));
    assertNull(cache.get("path/b.png"));
  }
}
