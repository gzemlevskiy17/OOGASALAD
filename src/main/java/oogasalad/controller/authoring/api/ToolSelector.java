package oogasalad.controller.authoring.api;

/**
 * Manages which editor tool is currently "in the user's hand."
 */
public interface ToolSelector {

  /**
   * To be updated in the future
   */
  enum ToolType { SELECT, DRAW, ERASE, PAINT, SPLINE }

  void setActiveTool(ToolType type);
  ToolType getActiveTool();
}