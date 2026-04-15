package oogasalad.controller.authoring.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import oogasalad.controller.authoring.internal.CanvasAction.ActionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CanvasActionHistoryTest {

  private CanvasActionHistory history;

  @BeforeEach
  void setUp() {
    history = new CanvasActionHistory();
  }

  private static CanvasAction place(int col, int row) {
    return new CanvasAction(ActionType.PLACE, col, row, 1, 1, "#000", "sprite", false, 0.5);
  }

  @Test
  void undo_emptyHistory_returnsEmpty() {
    assertFalse(history.canUndo());
    assertEquals(Optional.empty(), history.undo());
  }

  @Test
  void record_outsideBatch_commitsSingleElementBatch() {
    CanvasAction a = place(1, 2);
    history.record(a);

    assertTrue(history.canUndo());
    List<CanvasAction> popped = history.undo().orElseThrow();
    assertEquals(List.of(a), popped);
  }

  @Test
  void batch_groupsAllRecordedActionsAsOneUndoStep() {
    CanvasAction a = place(0, 0);
    CanvasAction b = place(1, 0);
    CanvasAction c = place(2, 0);

    history.beginBatch();
    history.record(a);
    history.record(b);
    history.record(c);
    history.endBatch();

    assertEquals(List.of(a, b, c), history.undo().orElseThrow());
    assertFalse(history.canUndo());
  }

  @Test
  void endBatch_withNoRecordedActions_doesNotPushEmptyBatch() {
    history.beginBatch();
    history.endBatch();

    assertFalse(history.canUndo());
  }

  @Test
  void nestedBatches_commitOnlyOnOutermostEnd() {
    CanvasAction a = place(0, 0);
    CanvasAction b = place(1, 0);

    history.beginBatch();
    history.record(a);
    history.beginBatch();
    history.record(b);
    history.endBatch();
    assertFalse(history.canUndo());
    history.endBatch();

    assertEquals(List.of(a, b), history.undo().orElseThrow());
  }

  @Test
  void endBatch_withoutMatchingBegin_isNoOp() {
    history.endBatch();
    history.record(place(0, 0));

    assertTrue(history.canUndo());
  }

  @Test
  void redo_afterUndo_returnsSameBatch() {
    CanvasAction a = place(3, 4);
    history.record(a);
    history.undo();

    assertEquals(List.of(a), history.redo().orElseThrow());
    assertTrue(history.canUndo());
    assertFalse(history.canRedo());
  }

  @Test
  void record_afterUndo_clearsRedoStack() {
    history.record(place(0, 0));
    history.undo();
    history.record(place(1, 1));

    assertFalse(history.canRedo());
  }

  @Test
  void undo_popsBatchesInLastInFirstOutOrder() {
    CanvasAction first = place(0, 0);
    CanvasAction second = place(1, 1);
    history.record(first);
    history.record(second);

    assertSame(second, history.undo().orElseThrow().get(0));
    assertSame(first, history.undo().orElseThrow().get(0));
  }

  @Test
  void clear_emptiesBothStacksAndDropsPendingBatch() {
    history.beginBatch();
    history.record(place(0, 0));
    history.clear();
    history.endBatch();

    assertFalse(history.canUndo());
    assertFalse(history.canRedo());
  }

  @Test
  void getActions_returnsFlattenedLogOldestFirst() {
    CanvasAction a = place(0, 0);
    CanvasAction b = place(1, 0);
    CanvasAction c = place(2, 0);

    history.beginBatch();
    history.record(a);
    history.record(b);
    history.endBatch();
    history.record(c);

    assertEquals(List.of(a, b, c), history.getActions());
  }
}
