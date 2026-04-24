package oogasalad.data.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import oogasalad.data.internal.LevelScanner.ScanResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LevelScannerTest {

  private LevelScanner scanner;

  @TempDir
  Path tempRoot;

  @BeforeEach
  void setUp() {
    scanner = new LevelScanner();
  }

  private void mkWorld(String name, String... levelJsonFilenames) throws IOException {
    Path worldDir = Files.createDirectory(tempRoot.resolve(name));
    for (String filename : levelJsonFilenames) {
      Files.createFile(worldDir.resolve(filename));
    }
  }

  @Test
  void scan_nonExistentDir_returnsEmptyResult() {
    ScanResult result = scanner.scan(tempRoot.resolve("does-not-exist"));

    assertTrue(result.worldsToLevels().isEmpty());
    assertTrue(result.others().isEmpty());
  }

  @Test
  void scan_emptyDir_returnsEmptyResult() {
    ScanResult result = scanner.scan(tempRoot);

    assertTrue(result.worldsToLevels().isEmpty());
    assertTrue(result.others().isEmpty());
  }

  @Test
  void scan_treatsSubdirectoriesAsWorlds_andJsonFilesAsLevels() throws IOException {
    mkWorld("forest", "first.json", "second.json");
    mkWorld("desert", "first.json");

    ScanResult result = scanner.scan(tempRoot);

    assertEquals(2, result.worldsToLevels().size());
    assertTrue(result.worldsToLevels().containsKey("forest"));
    assertTrue(result.worldsToLevels().containsKey("desert"));
  }

  @Test
  void scan_stripsJsonExtensionFromLevelKeys() throws IOException {
    mkWorld("forest", "first.json", "second.json");

    ScanResult result = scanner.scan(tempRoot);

    assertEquals(java.util.List.of("first", "second"),
        result.worldsToLevels().get("forest"));
  }

  @Test
  void scan_handlesWorldsWithDifferentLevelCounts() throws IOException {
    mkWorld("forest", "first.json", "second.json", "third.json");
    mkWorld("desert", "first.json");
    mkWorld("moon", "first.json", "second.json");

    ScanResult result = scanner.scan(tempRoot);

    assertEquals(3, result.worldsToLevels().get("forest").size());
    assertEquals(1, result.worldsToLevels().get("desert").size());
    assertEquals(2, result.worldsToLevels().get("moon").size());
  }

  @Test
  void scan_jsonAtRoot_isCapturedInOthers_notWorlds() throws IOException {
    mkWorld("forest", "first.json");
    Files.createFile(tempRoot.resolve("level.json"));
    Files.createFile(tempRoot.resolve("scratch.json"));

    ScanResult result = scanner.scan(tempRoot);

    assertEquals(1, result.worldsToLevels().size());
    assertEquals(java.util.List.of("level.json", "scratch.json"), result.others());
  }

  @Test
  void scan_ignoresNonJsonFiles_anywhere() throws IOException {
    mkWorld("forest", "first.json", "README.md", "notes.txt");
    Files.createFile(tempRoot.resolve("README.md"));

    ScanResult result = scanner.scan(tempRoot);

    assertEquals(java.util.List.of("first"), result.worldsToLevels().get("forest"));
    assertTrue(result.others().isEmpty());
  }

  @Test
  void scan_worldWithoutJsonFiles_yieldsEmptyLevelList() throws IOException {
    mkWorld("forest");  // directory but no level files

    ScanResult result = scanner.scan(tempRoot);

    assertTrue(result.worldsToLevels().containsKey("forest"));
    assertTrue(result.worldsToLevels().get("forest").isEmpty());
  }

  @Test
  void scan_worldDirectory_ignoresLevelFilesThatDontMatchTheConvention() throws IOException {
    mkWorld("forest", "first.json", "second.json", "A.json", "B.json", "scratch.json");

    ScanResult result = scanner.scan(tempRoot);

    // Only the canonical names survive. A.json, B.json, and scratch.json are dropped.
    assertEquals(java.util.List.of("first", "second"),
        result.worldsToLevels().get("forest"));
  }

  @Test
  void scan_othersAtRoot_areNotFilteredByLevelConvention() throws IOException {
    // Root-level JSONs land in `others` regardless of whether their names are canonical levels —
    // "others" is the catch-all for non-world JSON files, with no naming rules applied.
    Files.createFile(tempRoot.resolve("A.json"));
    Files.createFile(tempRoot.resolve("first.json"));
    Files.createFile(tempRoot.resolve("random-thing.json"));

    ScanResult result = scanner.scan(tempRoot);

    assertEquals(
        java.util.List.of("A.json", "first.json", "random-thing.json"),
        result.others());
  }
}
