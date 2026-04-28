package oogasalad.controller.authoring.api;

/**
 * Handles the high-level orchestration of the level editor.
 */
public interface AuthoringController {
  /** Registers mouse handlers (hover, click, drag) on the canvas's visual root. */
  void initializeCanvasHandlers();

  /** Commands the data layer to save the current progress. */
  void saveLevel(String fileName);

  /** Commands the data layer to load an existing level. */
  void loadLevel(String fileName);

  /** Reverses the most recently recorded canvas action, if any. */
  void undo();

  /** Re-applies the most recently undone canvas action, if any. */
  void redo();

  /** Clears the undo/redo history. */
  void clearHistory();

  /** Clears all placed objects, resets history, and refreshes the canvas. */
  void clearLevel();
}