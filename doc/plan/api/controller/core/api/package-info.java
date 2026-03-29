/**
 * Provides the core interfaces for managing high-level application state and navigation.
 *
 * OVERVIEW
 *
 * The primary goal of this API is toorchestrate the transitions
 * between distinct screens and their workflows,
 * the Level Authoring Environment, and the Gameplay Simulation. By centralizing
 * navigation, the API upholds the Single Responsibility Principle (SRP):
 * individual screens are responsible for their own internal layout and logic,
 * while the ScreenManager is solely responsible for the lifecycle of those
 * screens and the movement between them.
 *
 * This design encourages Extension through the Open/Closed Principle. The
 * ScreenManager is closed to modification, as its routing logic remains constant, but
 * open to extension via the AppScreen interface (different screens can be added). Developers can plug in entirely
 * new game modes or utilities (e.g., a "Replay Viewer" or "Settings Menu")
 * simply by registering a new implementation of a screen.
 *
 * DETAILS
 *
 * The API handles the "hand-off" between different specialized controllers.
 * For example, in the transition from the AuthoringScreen to the GameplayScreen,
 * the ScreenManager ensures that the editor's event-driven listeners are removed before the gameplay's continuous simulation clock begins.
 *
 * CONSIDERATIONS
 *
 * A significant design decision involved the use of a generic Object payload
 * in the navigateTo method.
 *
 * Pros: Maximum flexibility; allows to pass any data type
 * (Strings, JSON DTOs, or Model objects) without changing the API signature.
 *
 * Cons: Loss of compile-time type safety. Implementing screens must manually
 * cast the payload, which could lead to ClassCastExceptions if the sender
 * and receiver are not aligned on the protocol.
 */
package oogasalad.controller.core.api;