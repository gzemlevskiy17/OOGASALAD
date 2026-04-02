package oogasalad.controller.player.api;

/** A discrete gameplay command. */
public interface Action {
  /** Executes the command on the model. */
  void execute();
}