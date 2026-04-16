package oogasalad.controller.authoring.internal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * Two-stack undo/redo log for canvas modifications. Actions are grouped into
 * batches: a single mouse stroke, flood fill, or autofill records many
 * {@link CanvasAction}s but collapses to one undo step. Callers wrap related
 * work in {@link #beginBatch()} / {@link #endBatch()}; calls made outside a
 * batch are recorded as a single-element batch.
 *
 * <p>Recording a new batch clears the redo stack. Callers are responsible for
 * applying the inverse of returned actions against the world model.
 */
public class CanvasActionHistory {

  private final Deque<List<CanvasAction>> undoStack = new ArrayDeque<>();
  private final Deque<List<CanvasAction>> redoStack = new ArrayDeque<>();
  private List<CanvasAction> pendingBatch;
  private int batchDepth;

  /**
   * Starts grouping subsequent {@link #record(CanvasAction)} calls into one
   * undo step. Nested calls are counted so the outermost pair controls commit;
   * inner methods (autofill, flood-fill) can safely wrap their own work
   * without collapsing the surrounding stroke batch.
   */
  public void beginBatch() {
    if (batchDepth == 0) {
      pendingBatch = new ArrayList<>();
    }
    batchDepth++;
  }

  /**
   * Closes the current batch. The batch is only pushed onto the undo stack
   * when the outermost {@code endBatch} call balances the first {@code
   * beginBatch}. Empty batches are discarded.
   */
  public void endBatch() {
    if (batchDepth == 0) {
      return;
    }
    batchDepth--;
    if (batchDepth > 0) {
      return;
    }
    if (!pendingBatch.isEmpty()) {
      undoStack.push(List.copyOf(pendingBatch));
      redoStack.clear();
    }
    pendingBatch = null;
  }

  /**
   * Records an action. Inside an open batch it accumulates; outside a batch it
   * is committed immediately as a single-element batch.
   */
  public void record(CanvasAction action) {
    if (batchDepth > 0) {
      pendingBatch.add(action);
      return;
    }
    undoStack.push(List.of(action));
    redoStack.clear();
  }

  /**
   * Pops the most recent batch and moves it to the redo stack. The caller
   * should apply the inverse of each action in reverse order. Returns empty
   * if nothing can be undone.
   */
  public Optional<List<CanvasAction>> undo() {
    if (undoStack.isEmpty()) {
      return Optional.empty();
    }
    List<CanvasAction> batch = undoStack.pop();
    redoStack.push(batch);
    return Optional.of(batch);
  }

  /**
   * Pops the most recently undone batch and moves it back to the undo stack.
   * The caller should re-apply each action in forward order. Returns empty if
   * nothing can be redone.
   */
  public Optional<List<CanvasAction>> redo() {
    if (redoStack.isEmpty()) {
      return Optional.empty();
    }
    List<CanvasAction> batch = redoStack.pop();
    undoStack.push(batch);
    return Optional.of(batch);
  }

  public boolean canUndo() {
    return !undoStack.isEmpty();
  }

  public boolean canRedo() {
    return !redoStack.isEmpty();
  }

  /** Returns an unmodifiable flat log of recorded actions, oldest first. */
  public List<CanvasAction> getActions() {
    List<List<CanvasAction>> batches = new ArrayList<>(undoStack);
    Collections.reverse(batches);
    List<CanvasAction> flat = new ArrayList<>();
    for (List<CanvasAction> batch : batches) {
      flat.addAll(batch);
    }
    return Collections.unmodifiableList(flat);
  }

  /** Clears both stacks and any open batch. */
  public void clear() {
    undoStack.clear();
    redoStack.clear();
    pendingBatch = null;
    batchDepth = 0;
  }
}
