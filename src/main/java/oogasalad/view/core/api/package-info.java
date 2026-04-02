/**
 * Provides the core rendering and visual state management for the application view.
 *
 * OVERVIEW
 *
 * This package defines the contracts for the engine's visual representation. It decouples
 * the rendering logic from the physical game objects, allowing for flexible camera
 * behaviors and various rendering implementations (e.g., Canvas vs. Scene Graph).
 *
 * The following interfaces are available for external actors:
 *
 * 1. AppScreen: The high-level visual state.
 * - getRoot(): Returns the root node for display in the shared Scene.
 *
 * 2. Renderer: The drawing engine.
 * - render(Camera, Collection<Renderable>): Draws the visible subset of objects.
 *
 * 3. Renderable: A read-only graphical contract for game objects.
 * - getX(), getY(), getRotation(), getSprite(): Provides spatial and visual data.
 *
 * 4. Camera: The viewport logic.
 * - isVisible(Renderable): Determines if an object is within the viewing frustum.
 *
 * 5. CameraBehavior: The movement logic for the viewport.
 * - update(Camera): Defines how the camera follows players or targets.
 *
 * DETAILS
 *
 * The rendering pipeline follows a specific flow:
 * The Renderer receives a Camera and a list of Renderables. It uses the Camera's
 * 'isVisible' logic to cull objects before drawing. The Camera's position and
 * zoom are controlled by its assigned CameraBehavior.
 *
 * CONSIDERATIONS
 *
 * Renderable is strictly read-only to ensure that the View layer cannot
 * accidentally modify the Model's state during a draw call.
 */
package oogasalad.view.core.api;
