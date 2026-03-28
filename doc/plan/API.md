# OOGA API Design: Good Piggies

## Overview

This document describes the eight APIs that compose the OOGA engine. Each API is designed around three principles:

- **Easy to learn**: simple entry points with intuitive naming
- **Hard to misuse**: interfaces expose only what's needed; mutable state is never leaked
- **Provide for extension**: new game types, components, and behaviors added without modifying existing code

All APIs follow the `api/` and `internal/` package convention: the `api/` package contains only interfaces, immutable records, and exceptions. Concrete implementations live in `internal/` sub-packages and are never imported directly by outside modules.

---

## 1. App State API (`app.api`)

### Design Goals

Manages JavaFX screen transitions. Isolates the game loop from menus and the authoring environment. Upholds the Single Responsibility Principle: each screen owns its own lifecycle, and the ScreenManager only handles routing.

- **Easy to learn**: `screenManager.navigateTo("GAMEPLAY");`
- **Hard to misuse**: Forces `getScene()` so the manager doesn't manually construct JavaFX nodes.
- **Provide for extension**: New screens (settings, multiplayer lobby, results) are added by implementing `AppScreen` without modifying `ScreenManager`.

### Classes

```java
package app.api;

/**
 * Contract for a visual state in the application. Each screen must provide
 * a JavaFX Scene and manage its own lifecycle (timers, listeners, etc.).
 *
 * Extension point: implement this interface to add new screens to the
 * application without modifying the ScreenManager.
 */
public interface AppScreen {

    /** Returns the JavaFX Scene for this screen. */
    javafx.scene.Scene getScene();

    /** Called when this screen becomes active. Start timers/listeners here. */
    void onEnter();

    /** Called when this screen is being replaced. Stop timers/listeners here. */
    void onExit();
}

/**
 * Routes between AppScreen instances. Holds a registry of named screens
 * and manages transitions between them.
 */
public interface ScreenManager {

    /** Register a screen with a unique name. */
    void registerScreen(String name, AppScreen screen);

    /** Transition to the screen registered under the given name. */
    void navigateTo(String name);
}
```

---

## 2. Config & Rules API (`runtime.api.rules`)

### Design Goals

Data-driven logic. Upholds the Open/Closed Principle by allowing genre switching via JSON configuration rather than code changes. The `GameConfig` interface provides read-only access to parsed settings, and condition checkers evaluate game state without coupling to specific game types.

- **Easy to learn**: `config.isBuildModeEnabled()` and `checker.isWinConditionMet(world)`
- **Hard to misuse**: Config is read-only. Checkers receive the `World` but cannot modify it.
- **Provide for extension**: New win/loss conditions are added by creating a new `WinConditionChecker` or `LossConditionChecker` implementation and registering its type string in the config file.

### Classes

```java
package runtime.api.rules;

/**
 * Read-only access to parsed game configuration settings.
 * Loaded from game_config.json at startup.
 */
public interface GameConfig {

    /** Whether the vehicle build phase is shown before gameplay. */
    boolean isBuildModeEnabled();

    /** Camera mode string: "follow_player", "auto_scroll", etc. */
    String getCameraMode();
}

/**
 * Evaluates World state to determine if the player has won.
 * Extension point: implement for new victory conditions (reach goal,
 * collect all, survive for time, etc.).
 */
public interface WinConditionChecker {

    /** Returns true if the win condition is currently satisfied. */
    boolean isWinConditionMet(engine.api.World world);
}

/**
 * Evaluates World state to determine if the player has lost.
 * Extension point: implement for new loss conditions (health zero,
 * out of bounds, instant death on obstacle, velocity stop, etc.).
 */
public interface LossConditionChecker {

    /** Returns true if the loss condition is currently satisfied. */
    boolean isLossConditionMet(engine.api.World world);
}

/**
 * Calculates the final score based on World state and scoring rules
 * from the GameConfig.
 */
public interface ScoreCalculator {

    /** Compute the final score using metrics defined in the config. */
    int calculateFinalScore(engine.api.World world, GameConfig config);
}
```

---

## 3. Input Routing API (`runtime.api.input`)

### Design Goals

Maps JavaFX `KeyCode` events to abstract game `Action` commands. The engine never sees keyboard events: it only receives `execute()` calls. This decouples input hardware from game logic and makes input remappable at runtime.

- **Easy to learn**: `router.bindKeyToAction(KeyCode.SPACE, new MotorAction());`
- **Hard to misuse**: The engine only receives `execute()`; it has no idea what a keyboard is.
- **Provide for extension**: New actions are added by implementing `Action`. New input devices (gamepad, touch) can be supported by creating a new router implementation that produces the same `Action` calls.

### Classes

```java
package runtime.api.input;

/**
 * Command pattern for executing a gameplay action.
 * Each implementation knows how to activate or deactivate
 * its associated vehicle component.
 *
 * Extension point: implement for new player-controllable actions
 * (turbo boost, eject part, toggle balloon, etc.).
 */
public interface Action {

    /** Execute the action (e.g., apply force, toggle component state). */
    void execute();
}

/**
 * Maps hardware key events to abstract Action commands.
 * Receives raw JavaFX KeyCode events and dispatches the
 * corresponding Action.
 */
public interface InputRouter {

    /** Bind a keyboard key to a game action. */
    void bindKeyToAction(javafx.scene.input.KeyCode key, Action command);

    /** Called when a key is pressed. Dispatches the bound Action. */
    void receiveKeyPress(javafx.scene.input.KeyCode key);

    /** Called when a key is released. Can deactivate the bound Action. */
    void receiveKeyRelease(javafx.scene.input.KeyCode key);
}
```

---

## 4. Engine Core API (`engine.api`)

### Design Goals

Pure math simulation. Owns the physics `World`, applies forces, integrates velocity and position, and resolves `Joint` constraints for vehicle assembly. No rendering, no input, no UI: just physics.

- **Easy to learn**: `engine.step(deltaTime, config)` advances the simulation by one frame.
- **Hard to misuse**: Encapsulates vector math and Joint tracking inside internal implementations. `GameObject` exposes only getters.
- **Provide for extension**: Agnostic to specific part classes. Any object implementing `GameObject` participates in physics automatically.

### Classes

```java
package engine.api;

/**
 * Immutable 2D vector for physics math.
 */
public record Vector(double x, double y) {}

/**
 * Base entity in the simulation. Exposes mass, velocity, and hitbox data.
 * Extends Renderable so the view layer can draw it without casting.
 *
 * Extension point: implement for new entity types (terrain, power-ups,
 * finish lines, etc.). The engine treats all GameObjects uniformly.
 */
public interface GameObject extends view.api.Renderable {

    /** Unique identifier for this object. */
    String getId();

    /** Mass in kilograms. Affects force/acceleration calculations. */
    double getMass();

    /** Current velocity vector. */
    Vector getVelocity();
}

/**
 * Physical connection holding two GameObjects together.
 * The PhysicsEngine resolves joint constraints each step.
 */
public interface Joint {

    /** First connected object. */
    GameObject getObjectA();

    /** Second connected object. */
    GameObject getObjectB();

    /** Force threshold at which this joint breaks. */
    double getBreakingStrain();
}

/**
 * Memory container for all active GameObjects and Joints.
 * Provides methods to add/remove objects and create/destroy joints.
 */
public interface World {

    /** Add a GameObject to the simulation. */
    void addGameObject(GameObject obj);

    /** Create a rigid weld joint between two objects with a breaking force threshold. */
    void createWeld(GameObject a, GameObject b, double breakForce);

    /** Destroy all joints connected to the given object (simulates breakage). */
    void destroyJointsAttachedTo(GameObject target);

    /** Returns an unmodifiable list of all GameObjects in the world. */
    java.util.List<GameObject> getAllObjects();
}

/**
 * Contract to step the simulation forward.
 * The GameController calls step() each frame.
 */
public interface GameEngine {

    /** Advance physics by deltaTime seconds. Config provides physics overrides. */
    void step(double deltaTime, runtime.api.rules.GameConfig overrides);

    /** Access the World containing all active objects. */
    World getWorld();
}
```

---

## 5. Collision API (`engine.api.collision`)

### Design Goals

Strategy pattern for resolving overlaps. The `CollisionDetector` finds intersecting hitboxes, then delegates resolution to `CollisionStrategy` instances attached to each object. Detection math is fixed; resolution behavior is pluggable.

- **Easy to learn**: `detector.detectAndResolve(world);`
- **Hard to misuse**: Detection and resolution are separate concerns. Strategies receive impulse data but cannot bypass detection.
- **Provide for extension**: Add `BouncyCollisionStrategy`, `ExplosiveCollisionStrategy`, `CoinCollectionStrategy`, etc. without changing the detection math.

### Classes

```java
package engine.api.collision;

/**
 * Defines what happens when two GameObjects collide.
 *
 * Extension point: implement for new collision effects: elastic bounce,
 * inelastic stick, explosive destruction, coin collection, teleportation, etc.
 * Strategies can be assigned per GameObject or per collision pair.
 */
public interface CollisionStrategy {

    /**
     * Resolve a collision between two objects.
     *
     * @param a             first colliding object
     * @param b             second colliding object
     * @param impactImpulse magnitude of the collision impulse
     * @param world         the World, for side effects like destroying joints
     */
    void resolveCollision(
        engine.api.GameObject a,
        engine.api.GameObject b,
        double impactImpulse,
        engine.api.World world
    );
}

/**
 * Contract for finding overlapping hitboxes and delegating resolution
 * to CollisionStrategy instances.
 */
public interface CollisionDetector {

    /** Detect all collisions in the World and resolve them via strategies. */
    void detectAndResolve(engine.api.World world);
}
```

---

## 6. View & Rendering API (`view.api`)

### Design Goals

Reads `Renderable` data and draws it via JavaFX. Transforms world coordinates to screen coordinates using the `Camera`. The view layer is entirely passive: it consumes read-only data and never modifies game state.

- **Easy to learn**: `renderer.drawScene(camera, renderables);`
- **Hard to misuse**: `Renderable` only provides getters. The UI cannot accidentally modify physics.
- **Provide for extension**: New camera behaviors (follow player, auto-scroll, cinematic pan) added by implementing `CameraBehavior`. New rendering modes can be added by checking `isTiled()` and future flags.

### Classes

```java
package view.api;

/**
 * Read-only graphical data for drawing a game object.
 * Implemented by GameObject so the view layer can draw entities
 * without knowing their physics internals.
 *
 * The view layer never holds mutable references: it reads these
 * values fresh each frame.
 */
public interface Renderable {

    /** Asset path for the texture/sprite to draw. */
    String getTextureId();

    /** World X position. */
    double getX();

    /** World Y position. */
    double getY();

    /** Rotation in degrees. */
    double getRotation();

    /** If true, tile the texture across the object's size. If false, draw as a single sprite. */
    boolean isTiled();
}

/**
 * Logic for how the camera should move each frame.
 *
 * Extension point: implement for new camera modes: follow player
 * with lead-ahead, auto-scroll at constant speed, fixed position,
 * cinematic pan, etc.
 */
public interface CameraBehavior {

    /** Update the camera's position based on this behavior's logic. */
    void execute(Camera camera, double deltaTime, engine.api.World world);
}

/**
 * Determines which portion of the World is visible on screen.
 * Provides coordinate transformation from world space to screen space.
 */
public interface Camera {

    /** Set the movement behavior (follow player, auto-scroll, etc.). */
    void setBehavior(CameraBehavior behavior);

    /** Update the camera position for this frame. */
    void update(double deltaTime, engine.api.World world);

    /** Check if a Renderable is within the current viewport. */
    boolean isVisible(Renderable object);

    /** Convert a world X coordinate to a screen X coordinate. */
    double worldToScreenX(double worldX);

    /** Convert a world Y coordinate to a screen Y coordinate. */
    double worldToScreenY(double worldY);
}

/**
 * Contract for drawing a list of Renderable objects to the screen.
 * Uses the Camera for viewport transformation and visibility culling.
 */
public interface Renderer {

    /** Draw all visible renderables using the camera's viewport. */
    void drawScene(Camera camera, java.util.List<Renderable> renderables);
}
```

---

## 7. Authoring API (`authoring.api`)

### Design Goals

Exposes tools for building levels and vehicles using the same validation rules as the runtime. The authoring environment produces JSON files in the same format consumed by the game player, ensuring what you build is what you play.

- **Easy to learn**: `canvas.placeComponent("ground", 5, 2);`
- **Hard to misuse**: Placement goes through validation before being accepted. Invalid attachments are rejected with feedback.
- **Provide for extension**: New component types are added to the config data; the palette and canvas handle them without code changes.

### Classes

```java
package authoring.api;

/**
 * The visual grid where level components are placed.
 * Handles snap-to-grid placement and removal.
 *
 * Extension point: future implementations could support freehand
 * polygon drawing in addition to grid-based placement.
 */
public interface AuthoringCanvas {

    /**
     * Place a component at the given grid position.
     *
     * @param componentId the type of component to place (e.g., "ground", "spike")
     * @param gridX       grid column
     * @param gridY       grid row
     * @throws InvalidPlacementException if the position is occupied or out of bounds
     */
    void placeComponent(String componentId, int gridX, int gridY);

    /**
     * Remove whatever component exists at the given grid position.
     *
     * @param gridX grid column
     * @param gridY grid row
     */
    void removeComponent(int gridX, int gridY);
}

/**
 * Provides the list of available components for the current game type.
 * Reads from GameConfig to determine which parts are available.
 */
public interface ComponentPalette {

    /**
     * Returns the IDs of all components available for placement.
     *
     * @param config the current game configuration
     * @return list of component type IDs
     */
    java.util.List<String> getAvailableComponents(runtime.api.rules.GameConfig config);
}

/**
 * Validates vehicle assembly rules: which parts can attach where.
 * Used by both the authoring environment and the gameplay car builder.
 */
public interface VehicleBuildRulesManager {

    /**
     * Check whether a component can be placed at the given position
     * in the vehicle grid, considering adjacency and attachment rules.
     *
     * @param componentId the type of component
     * @param gridX       grid column in the vehicle grid
     * @param gridY       grid row in the vehicle grid
     * @return true if the attachment is valid
     */
    boolean isValidAttachment(String componentId, int gridX, int gridY);
}
```

---

## 8. Data Persistence API (`data.api`)

### Design Goals

Translates JSON files into Java objects and vice versa. Provides a stable contract for loading and saving level data, vehicle configurations, and game configs. Used by both the authoring environment and the gameplay runtime.

- **Easy to learn**: `World world = loader.loadLevel("levels/level1.json");`
- **Hard to misuse**: Loading returns fully constructed objects. Saving validates before writing.
- **Provide for extension**: New data fields in JSON files are handled by the loader implementation; callers access them through the `GameConfig` and `World` interfaces without changes.

### Classes

```java
package data.api;

/**
 * Translates JSON files into in-memory game objects.
 * Parses level files into a populated World and config files
 * into a GameConfig.
 *
 * Throws descriptive exceptions if files are malformed or
 * missing required fields.
 */
public interface LevelLoader {

    /**
     * Parse a level JSON file and construct a World containing
     * all terrain, obstacles, collectibles, and goals.
     *
     * @param filepath path to the level JSON file
     * @return a populated World ready for simulation or editing
     * @throws LevelLoadException if the file is malformed or missing required fields
     */
    engine.api.World loadLevel(String filepath);

    /**
     * Parse a game configuration JSON file.
     *
     * @param filepath path to the game_config.json file
     * @return a read-only GameConfig
     * @throws LevelLoadException if the file is malformed
     */
    runtime.api.rules.GameConfig loadGameConfig(String filepath);
}

/**
 * Serializes in-memory World state to a JSON file.
 * Validates level completeness before writing.
 */
public interface LevelSaver {

    /**
     * Serialize the World to a JSON file at the given path.
     * Validates that the level has a start position and at least one goal
     * before writing.
     *
     * @param world    the World to serialize
     * @param filepath destination file path
     * @throws LevelSaveException if validation fails or the file cannot be written
     */
    void saveLevel(engine.api.World world, String filepath);
}
```

---

## Complete Data Flow

1. **App Boots**: `Launcher` tells `ScreenManager` to load `GameMenuScreen`.
2. **User Clicks Play**: `ScreenManager` swaps the JavaFX `Scene` to `GamePlayScreen`.
3. **Gameplay Starts**: `GamePlayScreen` creates the `GameController`.
4. **Data Loads**: `GameController` asks `LevelLoaderImpl` to parse `level.json`. It builds a `WorldImpl` full of `GameObjects`.
5. **The Loop Ticks**: The JavaFX `Timeline` fires 60 times per second inside `GameController`:
   1. User holds Spacebar → JavaFX fires a `KeyEvent` → `InputRouterImpl` sees `KeyCode.SPACE` → triggers `MotorAction.execute()`.
   2. `GameEngine.step()` updates all physics math and resolves Joints.
   3. `CollisionDetector.detectAndResolve()` checks for crashes.
   4. `Camera.update()` recalculates the viewport.
   5. `RendererImpl.drawScene()` gets the `GraphicsContext`, clears the canvas, loops over all `Renderable` objects in the `World`, converts their world X/Y to screen X/Y using the `Camera`, and draws images to the screen.