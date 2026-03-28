/**
 * Provides the controller for the authoring environment
 *
 * OVERVIEW
 *
 * This package defines the contracts for manipulating the level editor's state and
 * managing the user's editing context. It exposes two primary interfaces for
 * external actors (such as the UI/View layer):
 *
 * 1. AuthoringController: The primary entry point for level manipulation and persistence.
 * - handleGridInteraction(MouseEvent): Translates raw mouse input into grid placement/removal logic.
 * - saveLevel(String): Triggers the serialization of the current progress to a file.
 * - loadLevel(String): Populates the editor state from an existing level file.
 *
 * 2. ToolSelector: Manages the "active state" of the cursor.
 * - setActiveTool(ToolType): Switches the current tool (e.g., DRAW or ERASE).
 * - getActiveTool(): Returns the currently active tool to sync UI indicators.
 *
 * DETAILS
 *
 * The AuthoringController acts as the mediator between the visual grid and the
 * underlying data layer. It interprets coordinates from JavaFX MouseEvents to
 * ensure that the Model is updated correctly according to the current editor
 * configuration and the active tool selected via the ToolSelector.
 *
 * CONSIDERATIONS
 *
 * By separating the ToolSelector from the AuthoringController, we allow the
 * toolbar UI to remain completely independent of the grid interaction logic.
 * This makes the API highly extensible; adding a new tool (like a "Fill" or "Rotate"
 * tool) only requires an update to the ToolType enum and the corresponding logic
 * inside the controller implementation.
 */
package oogasalad.controller.authoring.api;