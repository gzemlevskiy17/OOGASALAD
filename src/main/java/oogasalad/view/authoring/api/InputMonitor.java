package oogasalad.view.authoring.api;

@Deprecated
@FunctionalInterface
public interface InputMonitor {

  void onCellInteraction(int col, int row);
}
