/**
 * Provides the ability to work with data (saving and loading game states and configurations)
 *
 * OVERVIEW
 *
 * This package handles the translation between JSON files and the application's
 * internal memory. It provides three primary interfaces for external actors
 * (such as the GameController or AuthoringController):
 *
 * 1. LevelLoader: The deserialization gateway.
 * - loadLevel(String, World, GameConfig): Translates a JSON file into active
 * objects within a World using configuration rules.
 *
 * 2. LevelSaver: The serialization gateway.
 * - saveLevel(String, World): Translates the current state of a World into
 * a persistent JSON file.
 *
 * 3. GameConfig: The read-only settings provider.
 * - getSetting(String): Retrieves a specific configuration value (e.g., gravity,
 * time limits) for the GameController.
 *
 * DETAILS
 *
 * The Data API is designed to be format-agnostic. While the current implementation
 * focuses on JSON, the interfaces allow for swapping in XML, YAML, or database
 * backends without changing the rest of the engine. The GameConfig interface
 * acts as a "source of truth" for game-specific constants that aren't hardcoded.
 *
 * CONSIDERATIONS
 *
 * We separate Loading and Saving to allow for read-only game clients or
 * write-only logging systems. LevelLoader requires a GameConfig as a collaborator
 * to ensure that loaded JSON data correctly maps to the expected game rules
 * and object prototypes defined for that specific game type.
 */
package oogasalad.data.api;