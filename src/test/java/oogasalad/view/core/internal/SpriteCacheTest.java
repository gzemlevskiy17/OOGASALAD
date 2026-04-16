package oogasalad.view.core.internal;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Field;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
  void get_emptyPath_returnsNull() {
    assertNull(cache.get(""));
  }

  @Test
  void get_repeatedMissingPath_alwaysReturnsNull() {
    assertNull(cache.get("ghost.png"));
    assertNull(cache.get("ghost.png"));
    assertNull(cache.get("ghost.png"));
  }

  @Test
  void get_nullIsNotStoredInCache() throws Exception {
    // HashMap.computeIfAbsent does not cache null return values, so the cache
    // map should remain empty after a miss. This verifies that behavior.
    cache.get("missing.png");

    Field cacheField = SpriteCache.class.getDeclaredField("cache");
    cacheField.setAccessible(true);
    @SuppressWarnings("unchecked")
    Map<String, ?> internalMap = (Map<String, ?>) cacheField.get(cache);

    assertNull(internalMap.get("missing.png"),
        "A null-returning load must not be persisted in the cache map");
  }

  @Test
  void get_twoDifferentMissingPaths_bothReturnNull() {
    assertNull(cache.get("path/a.png"));
    assertNull(cache.get("path/b.png"));
  }
}
