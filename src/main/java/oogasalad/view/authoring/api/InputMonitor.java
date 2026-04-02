package oogasalad.view.authoring.api;

@FunctionalInterface
public interface InputMonitor {

  void onCellInteraction(int col, int row);
}
