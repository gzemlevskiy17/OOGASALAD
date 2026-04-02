/**
 * Provides the visual interface components for the level authoring environment.
 *
 * OVERVIEW
 *
 * This package defines the contracts for the primary visual regions of the editor.
 * It ensures that the authoring tools remain decoupled from the specific layout
 * implementation, allowing for different UI themes or arrangement of editor panes.
 *
 * The following interfaces are available for external actors:
 *
 * 1. AuthoringCanvas: The visual grid workspace.
 * - getVisualRoot(): Returns the JavaFX Node representing the editable grid.
 * - refreshGrid(): Forces a visual update of the blocks currently on the canvas.
 *
 * 2. ComponentPalette: The selection interface for editor parts.
 * - initialize(GameConfig): Populates the palette based on available game configurations.
 * - getVisualRoot(): Returns the JavaFX Node representing the list of available parts.
 *
 * DETAILS
 *
 * The AuthoringCanvas acts as the visual destination for interactions handled by
 * the AuthoringController. The ComponentPalette serves as the source of parts
 * that the user can select to place onto the canvas. By using the 'GameConfig'
 * as a collaborator, the palette dynamically adapts to whatever game types are
 * currently being authored.
 *
 * CONSIDERATIONS
 *
 * Both interfaces return generic JavaFX 'Node' objects rather than specific
 * layout panes (like ScrollPane or VBox). This provides the flexibility to
 * wrap these components in different containers depending on the screen size
 * or editor layout requirements.
 */
package oogasalad.view.authoring.api;