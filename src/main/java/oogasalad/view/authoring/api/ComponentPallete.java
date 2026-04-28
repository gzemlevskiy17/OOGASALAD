package oogasalad.view.authoring.api;

import javafx.scene.Node;
// Assuming Settings exists in a configuration or model API package
// import oogasalad.model.api.Settings;

/** Provides a visual list of available parts/assets for the user to select. */
public interface ComponentPallete {

  /** * Populates the palette with parts defined in the provided configuration.
   * @param config The data source containing available parts and their metadata.
   */
  // void initialize(Settings config);

  /** @return the visual root of the palette (e.g., a sidebar or tray). */
  Node getVisualRoot();
}