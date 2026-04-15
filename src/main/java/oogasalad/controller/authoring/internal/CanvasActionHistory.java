package oogasalad.controller.authoring.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ordered log of canvas modifications. Provides a foundation for
 * future undo/redo or change-tracking features.
 */
public class CanvasActionHistory {

  private final List<CanvasAction> actions = new ArrayList<>();

  /** Appends an action to the history. */
  public void record(CanvasAction action) {
    actions.add(action);
  }

  /** Returns an unmodifiable view of all recorded actions. */
  public List<CanvasAction> getActions() {
    return Collections.unmodifiableList(actions);
  }

  /** Removes all recorded actions. */
  public void clear() {
    actions.clear();
  }
}
