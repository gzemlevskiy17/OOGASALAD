package oogasalad.data.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Walks the levels directory tree and reports what's on disk.
 *
 * <p>Convention:
 * <ul>
 *   <li>Each immediate subdirectory under the root is one world, and each {@code .json} file
 *       inside it whose basename appears in {@link LevelProgression#LEVEL_ORDER} is one level.
 *       Level files whose names don't match the convention are ignored.</li>
 *   <li>The reserved subdirectory named {@value #OTHERS_DIRECTORY_NAME} is NOT treated as a
 *       world. Its {@code .json} contents are collected into {@link ScanResult#others()} with
 *       no convention filter — sandbox files for one-off plays.</li>
 *   <li>Loose {@code .json} files at the root (not inside any subdirectory) are ignored. Drop
 *       them into the {@code others/} folder if they should appear as sandbox levels.</li>
 *   <li>Non-JSON files anywhere are ignored.</li>
 * </ul>
 *
 * <p>A missing root directory returns an empty result rather than throwing. Per-call sort
 * order is alphabetical so repeated scans produce stable output.
 */
public class LevelScanner {

  private static final String JSON_SUFFIX = ".json";

  /** Reserved directory name whose contents become {@link ScanResult#others()}. */
  public static final String OTHERS_DIRECTORY_NAME = "others";

  private final boolean filterByProgression;

  /**
   * Creates a scanner that filters level files by {@link LevelProgression#LEVEL_ORDER}.
   */
  public LevelScanner() {
    this(true);
  }

  /**
   * @param filterByProgression if {@code true}, only level files whose basename (minus
   *                            {@code .json}) appears in {@link LevelProgression#LEVEL_ORDER} are
   *                            included. If {@code false}, all JSON files in a world directory are
   *                            listed.
   */
  public LevelScanner(boolean filterByProgression) {
    this.filterByProgression = filterByProgression;
  }

  /**
   * Output of a scan.
   *
   * @param worldsToLevels world-key → level-keys (no extensions), ready to plug into
   *     {@link LevelProgression}'s constructor. The "others" directory is never a world.
   * @param others filenames (with {@code .json} extension) of files inside the "others"
   *     directory. Empty if the directory doesn't exist or has no JSON files.
   */
  public record ScanResult(Map<String, List<String>> worldsToLevels, List<String> others) {}

  /** Scans the given root directory. Returns an empty result if the directory doesn't exist. */
  public ScanResult scan(Path rootDir) {
    if (!Files.isDirectory(rootDir)) {
      return new ScanResult(Map.of(), List.of());
    }
    Map<String, List<String>> worlds = new LinkedHashMap<>();
    List<String> others = new ArrayList<>();
    try (Stream<Path> entries = Files.list(rootDir)) {
      entries.sorted().forEach(entry -> {
        if (!Files.isDirectory(entry)) {
          // Loose root-level files (JSON or otherwise) are ignored — drop them into others/.
          return;
        }
        String name = entry.getFileName().toString();
        if (OTHERS_DIRECTORY_NAME.equals(name)) {
          others.addAll(listJsonFilenames(entry));
        } else {
          worlds.put(name, scanWorld(entry));
        }
      });
    } catch (IOException e) {
      throw new RuntimeException("Failed to scan levels root " + rootDir, e);
    }
    return new ScanResult(Collections.unmodifiableMap(worlds), List.copyOf(others));
  }

  /** Returns every {@code .json} filename (with extension) directly inside {@code dir}. */
  private List<String> listJsonFilenames(Path dir) {
    List<String> filenames = new ArrayList<>();
    try (Stream<Path> entries = Files.list(dir)) {
      entries.sorted().forEach(entry -> {
        if (isJsonFile(entry)) {
          filenames.add(entry.getFileName().toString());
        }
      });
    } catch (IOException e) {
      throw new RuntimeException("Failed to scan others directory " + dir, e);
    }
    return filenames;
  }

  private List<String> scanWorld(Path worldDir) {
    List<String> levelKeys = new ArrayList<>();
    try (Stream<Path> entries = Files.list(worldDir)) {
      entries.sorted().forEach(entry -> {
        if (!isJsonFile(entry)) {
          return;
        }
        String filename = entry.getFileName().toString();
        if (LevelPaths.GAME_FILE_NAME.equals(filename)) {
          return; // game catalog lives alongside levels but is not itself a level
        }
        String levelKey = filename.substring(0, filename.length() - JSON_SUFFIX.length());
        if (!filterByProgression || LevelProgression.LEVEL_ORDER.contains(levelKey)) {
          levelKeys.add(levelKey);
        }
      });
    } catch (IOException e) {
      throw new RuntimeException("Failed to scan world directory " + worldDir, e);
    }
    return List.copyOf(levelKeys);
  }

  private static boolean isJsonFile(Path p) {
    return Files.isRegularFile(p) && p.getFileName().toString().endsWith(JSON_SUFFIX);
  }
}
