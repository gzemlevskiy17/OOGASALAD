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
  void scan_jsonAtRoot_isIgnored_neitherWorldNorOthers() throws IOException {
    mkWorld("forest", "first.json");
    Files.createFile(tempRoot.resolve("level.json"));
    Files.createFile(tempRoot.resolve("scratch.json"));

    ScanResult result = scanner.scan(tempRoot);

    // Loose root-level JSONs are dropped entirely under the new convention.
    assertEquals(1, result.worldsToLevels().size());
    assertTrue(result.others().isEmpty());
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
  void scan_othersDirectory_filenamesAreCollectedWithoutConventionFilter() throws IOException {
    // The reserved "others" directory is not a world. Its JSON files become the others list,
    // with no LEVEL_ORDER filter applied — names like "A" or "random-thing" are kept.
    mkWorld(LevelScanner.OTHERS_DIRECTORY_NAME,
        "A.json", "first.json", "random-thing.json");

    ScanResult result = scanner.scan(tempRoot);

    assertEquals(
        java.util.List.of("A.json", "first.json", "random-thing.json"),
        result.others());
    // And it does NOT show up as a world.
    assertTrue(result.worldsToLevels().isEmpty()
        || !result.worldsToLevels().containsKey(LevelScanner.OTHERS_DIRECTORY_NAME));
  }

  @Test
  void scan_missingOthersDirectory_yieldsEmptyOthersList() throws IOException {
    mkWorld("forest", "first.json");
    // No "others" directory created.

    ScanResult result = scanner.scan(tempRoot);

    assertTrue(result.others().isEmpty());
  }

  @Test
  void scan_emptyOthersDirectory_yieldsEmptyOthersList() throws IOException {
    mkWorld("forest", "first.json");
    mkWorld(LevelScanner.OTHERS_DIRECTORY_NAME);  // exists but no files inside

    ScanResult result = scanner.scan(tempRoot);

    assertTrue(result.others().isEmpty());
  }

  @Test
  void scan_othersDirectory_ignoresNonJsonFiles() throws IOException {
    mkWorld(LevelScanner.OTHERS_DIRECTORY_NAME, "level.json", "notes.txt", "README.md");

    ScanResult result = scanner.scan(tempRoot);

    assertEquals(java.util.List.of("level.json"), result.others());
  }
}
