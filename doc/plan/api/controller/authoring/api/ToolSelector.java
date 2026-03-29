package oogasalad.controller.authoring.api;

/**
 * Manages which editor tool is currently "in the user's hand."
 */
public interface ToolSelector {

  /**
   * To be updated in the future
   */
  enum ToolType { DRAW, ERASE }

  void setActiveTool(ToolType type);
  ToolType getActiveTool();
}