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
 * <p>Convention: each immediate subdirectory under the root is one world, and each
 * {@code .json} file inside a world directory whose basename appears in
 * {@link LevelProgression#LEVEL_ORDER} is one level. Level files whose names don't match the
 * convention (e.g. {@code A.json}, {@code scratch.json}) are ignored — they exist on disk but
 * are not reported. Files placed directly at the root (not inside a world) are collected under
 * {@link ScanResult#others()} without any convention filter, as a placeholder for future
 * handling. Non-JSON files anywhere are ignored.
 *
 * <p>A missing root directory returns an empty result rather than throwing. Per-call sort
 * order is alphabetical so repeated scans produce stable output; downstream consumers (e.g.
 * {@code LevelProgression}) re-sort against their own canonical orders anyway.
 */
public class LevelScanner {

  private static final String JSON_SUFFIX = ".json";

  /**
   * Output of a scan.
   *
   * @param worldsToLevels world-key → level-keys (no extensions), ready to plug into
   *     {@link LevelProgression}'s constructor.
   * @param others filenames of JSON files found directly at the root (no world) — placeholder
   *     category for items that don't fit the world/level shape; currently unused downstream.
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
        if (Files.isDirectory(entry)) {
          worlds.put(entry.getFileName().toString(), scanWorld(entry));
        } else if (isJsonFile(entry)) {
          others.add(entry.getFileName().toString());
        }
      });
    } catch (IOException e) {
      throw new RuntimeException("Failed to scan levels root " + rootDir, e);
    }
    return new ScanResult(Collections.unmodifiableMap(worlds), List.copyOf(others));
  }

  private List<String> scanWorld(Path worldDir) {
    List<String> levelKeys = new ArrayList<>();
    try (Stream<Path> entries = Files.list(worldDir)) {
      entries.sorted().forEach(entry -> {
        if (!isJsonFile(entry)) {
          return;
        }
        String filename = entry.getFileName().toString();
        String levelKey = filename.substring(0, filename.length() - JSON_SUFFIX.length());
        if (LevelProgression.LEVEL_ORDER.contains(levelKey)) {
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
