package oogasalad.data.internal;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resolves on-disk data directories independent of the JVM working directory. Anchors on the
 * classpath root (Maven's {@code target/classes/} or {@code target/test-classes/}) and walks up
 * two levels to the project root. Maven-only.
 */
public final class DataPaths {

  private static final Path LEVELS_ROOT = resolveLevelsRoot();

  private DataPaths() {}

  public static Path levelsRoot() {
    return LEVELS_ROOT;
  }

  private static Path resolveLevelsRoot() {
    try {
      Path classesDir = Paths.get(DataPaths.class.getResource("/").toURI());
      return classesDir.getParent().getParent().resolve("data").resolve("levels");
    } catch (URISyntaxException e) {
      throw new IllegalStateException("Cannot resolve classpath root", e);
    }
  }
}
