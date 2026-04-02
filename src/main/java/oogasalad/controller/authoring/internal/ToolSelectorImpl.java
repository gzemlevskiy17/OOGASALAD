package oogasalad.controller.authoring.internal;

import oogasalad.controller.authoring.api.ToolSelector;

/**
 * Tracks the currently active editor tool (DRAW or ERASE).
 * Defaults to DRAW on construction.
 *
 * Collaborators: AuthoringControllerImpl (reads active tool per grid interaction).
 */
public class ToolSelectorImpl implements ToolSelector {

  private ToolType activeTool = ToolType.DRAW;

  @Override
  public void setActiveTool(ToolType type) {
    this.activeTool = type;
  }

  @Override
  public ToolType getActiveTool() {
    return activeTool;
  }
}
