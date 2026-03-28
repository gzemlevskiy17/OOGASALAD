# OOGA Design Plan — Good Piggies

## Design Goals

Our design prioritizes **encapsulation of core simulation logic** while enabling **flexible extension of gameplay elements**. The architecture separates concerns into distinct layers:

- **Simulation Engine**: Physics calculations, collision detection, and world state management are encapsulated behind stable APIs. These subsystems are "closed" to modification — they operate as pure, deterministic engines that do not vary across game genres.
- **Game Content**: GameObjects, Action Strategies, and Collision Strategies are "open" for extension. New component types, vehicle behaviors, and interactive objects can be added without modifying the engine core.
- **Game Configuration**: A data-driven configuration layer (`game_config.json`) controls high-level gameplay behavior — build mode, camera mode, input mapping, scoring rules, and win/loss conditions. Switching from a Bad Piggies-style sandbox to a Geometry Dash-style auto-scroller requires changing a config file, not engine code.
- **Authoring vs. Runtime**: The authoring environment and gameplay runtime share the same underlying data formats and world representation, eliminating duplication and ensuring that player-created levels behave identically during play.
- **API / Internal Separation**: Every module exposes an `api/` package containing only interfaces, abstract classes, immutable records, and exceptions. Concrete implementations live in `internal/` sub-packages and are never imported directly by outside modules. This enforces loose coupling and makes every subsystem independently testable and swappable.

### Flexible

- **GameObject** — interface for all in-game entities; new component types added without changing engine code
- **Action Strategies** — command pattern; interchangeable gameplay actions attached to vehicle components
- **Collision Strategies** — strategy pattern; pluggable handlers for what happens when two objects collide
- **Camera Behavior** — switchable between `FollowPlayerBehavior`, `AutoScrollBehavior`, and future modes via `CameraBehavior` interface
- **Win/Loss Conditions** — configurable per game type via `WinConditionChecker` and `LossConditionChecker` interfaces
- **Scoring Rules** — time bonus, distance scoring, progress percentage, coin values, star thresholds; all driven by `ScoreCalculator` reading from `GameConfig`
- **Input Routing** — `InputRouter` interface maps hardware keys to `Action` commands, configurable per game type
- **Rendering Mode** — per-object control over tiled textures vs. single sprites via data flags
- **AppScreen** — interface for visual states; new screens (level select, settings, multiplayer lobby) added without modifying `ScreenManager`

### Encapsulated

- **Physics Engine** — pure calculation, responsible only for numerical simulation of motion, joint constraint solving
- **Collision Detector** — responsible for detecting overlapping hitboxes, produces collision pairs but does not interpret them
- **World** — holds all GameObjects and Joints, knows the game state, provides read-only views for rendering and simulation
- **Renderer** — consumes read-only render data via `Renderable` interface, handles tiling and sprite drawing, does not contain game logic
- **Level Persistence** — `LevelLoader` and `LevelSaver` interfaces handle serialization/deserialization of level and vehicle data to/from JSON files
- **ScreenManager** — routes between `AppScreen` implementations; the routing logic itself is fixed while screens are extensible

---

## Primary Architecture: Open/Closed Principle

### Closed for Modification (Encapsulated)

| Subsystem | Encapsulated Responsibility | Why Closed |
|---|---|---|
| **PhysicsEngine** (implements `GameEngine`) | Numerical integration of motion (velocity, force, mass), applies gravity, integrates velocity, resolves Joints as constraints. | Physics is domain-agnostic; it should work identically for any game using realistic motion. Changing its internal algorithm should not affect game-specific code. |
| **CollisionDetector** (interface + `CollisionDetectorImpl`) | Loops through hitboxes to check for intersections, returns collision pairs. Delegates resolution to `CollisionStrategy` instances. | The detection logic is independent of what happens after a collision. It produces raw collision events but does not interpret them. |
| **World** (interface + `WorldImpl`) | Memory container for all active GameObjects and Joints. Manages adding/removing items. Provides read-only views for rendering and simulation. | World state management is a fixed service. How objects are stored or enumerated should not vary by game type. |
| **Renderer** (interface + `RendererImplementation`) | Paints `Renderable` objects using JavaFX tools. Reads `tile_texture` and `render_mode` flags to determine tiled vs. sprite drawing. Applies `Camera` viewport transform before drawing. | The view layer consumes read-only graphical data. It does not contain game logic and can be swapped for different front-ends. |
| **LevelLoader / LevelSaver** (interfaces + Impl classes) | Translates JSON files to/from in-memory World state. | The format for storing levels is stable; saving and loading should work consistently across authoring and runtime modes. |
| **ScreenManager** (interface + `ScreenManagerClass`) | Routes between `AppScreen` instances. | Screen routing logic is fixed. New screens are added by implementing `AppScreen`, not by modifying the manager. |

### Open for Extension (Flexible)

| Subsystem | Extension Mechanism | What Can Be Added Without Engine Changes |
|---|---|---|
| **GameObject Hierarchy** | `GameObject` interface → `GameComponent` abstract class → concrete subclasses (`VehicleComponent`, `TerrainComponent`, `PowerupComponent`, `FinishLineComponent`). `VehicleComponent` further splits into `DriveComponent` and `StructuralComponent`. | New vehicle parts (rockets, springs), terrain types (ice, mud), power-ups, and goal objects. Each defines its own mass, collision shape, and rendering sprite. |
| **Action Strategies** | `Action` interface with `execute()`. Implementations: `FanAction`, `BalloonAction`, `FrameAction`, etc. | New player-controllable actions (turbo boost, eject part, toggle balloon). Actions triggered by `InputRouter` mapping keys to `Action` commands. |
| **Collision Strategies** | `CollisionStrategy` interface. Implementations: `ElasticCollisionStrategy`, `InelasticCollisionStrategy`, and future strategies (sticky, teleport, damage zone, coin collection). | New collision effects assigned per GameObject or per collision pair. |
| **GameConfig** | Data-driven config file loaded at startup. `GameConfig` interface with read-only access to parsed settings. | New game types defined entirely through configuration: `build_mode`, `camera.mode`, `input_mapping`, `win_condition`, `loss_conditions`, `scoring`, `gameplay_modifiers`. |
| **CameraBehavior** | `CameraBehavior` interface. Implementations: `FollowPlayerBehavior`, `AutoScrollBehavior`, and future modes. | New camera modes added as implementations without touching `Camera` or `Renderer`. |
| **Scoring** | `ScoreCalculator` interface + `ScoreCalculatorImpl`. Reads scoring rules from `GameConfig`. | Completion bonuses, time bonuses, distance scoring, progress-percentage scoring, coin multipliers, star thresholds — all configured in data. |
| **Win/Loss Conditions** | `WinConditionChecker` interface (e.g., `ReachGoalChecker`). `LossConditionChecker` interface (e.g., `VelocityStopChecker`, `OutOfBoundsChecker`, `InstantDeathChecker`). | New condition types added as implementations and registered by type string in config. No changes to `GameController`. |
| **AppScreen** | `AppScreen` interface requiring a `Scene`. Implementations: `GamePlayScreen`, `GameMenuScreen`, `AuthoringScreen`. | New screens (level select, settings, multiplayer lobby, results) added by implementing the interface. |
| **Input Router** | `InputRouter` interface + `InputRouterImpl`. Maps hardware keys to `Action` commands. | New control schemes, remappable keys, and input modes added without altering core input handling. |

---

## Application Lifecycle and Screen Management

The application starts from `Launcher/Main`, which initializes the JavaFX Stage and creates the `ScreenManager`.

### Launcher → ScreenManager → AppScreens

- **Launcher** creates the `ScreenManager` and passes it the Stage.
- **ScreenManager** (interface, implemented by `ScreenManagerClass`) routes between `AppScreen` implementations. It holds a reference to the active screen and provides methods to switch screens.
- **AppScreen** (interface) is the contract for a visual state. Each screen must provide a JavaFX `Scene`. Implementations include:
  - **GameMenuScreen** — the start menu. Shows options to play, open the authoring environment, or access settings. Collaborates with `ScreenManager` to navigate.
  - **GamePlayScreen** — the active gameplay view. Sets up the canvas, instantiates the `GameController` when entered, and destroys it when exited. This is the only screen where the game loop runs.
  - **AuthoringScreen** — the level/vehicle editor. Hosts `AuthoringCanvas` and `ComponentPalette`. Instantiates `AuthoringController` when entered.

### Screen Transition Flow

```
Launcher
  └── ScreenManager
        ├── GameMenuScreen (start here)
        │     ├── "Play" → GamePlayScreen
        │     ├── "Build" → AuthoringScreen
        │     └── "Settings" → (future SettingsScreen)
        │
        ├── GamePlayScreen
        │     ├── GameController runs game loop
        │     ├── Win/Loss → Results (future ResultsScreen or dialog)
        │     └── Pause/Quit → GameMenuScreen
        │
        └── AuthoringScreen
              ├── AuthoringController manages editing
              ├── "Test Play" → temporary GamePlayScreen
              └── "Back" → GameMenuScreen
```

The `ScreenManager` owns transitions. Individual screens never directly create other screens — they request transitions through `ScreenManager`.

---

## API Services and Collaboration

### Physics-Based Vehicle Construction

- **GameObject** (interface, extends `Renderable`) exposes properties: mass, velocity, hitboxes, position.
- **Vehicle** holds a 2D grid of `VehicleComponent` objects. Each step, it sums the forces from all components and exposes the total to the `PhysicsEngine`.
- **DriveComponent** (abstract) returns a force vector when activated via its `Action`. Concrete implementations: `Fan` (horizontal force), `Balloon` (vertical force).
- **StructuralComponent** (abstract) returns zero force. Concrete implementations: `Wheel`, `Frame`.
- **Joint** (interface) represents a physical connection holding two GameObjects together. The `PhysicsEngine` resolves joint constraints each step. Joint types include rigid (weld) and revolute (wheel axle).
- **VehicleCollisionStrategy** handles impacts: if impulse exceeds a threshold, it can request the `World` to destroy specific Joints, simulating part breakage.

### Game Configuration and Genre Switching

- **GameConfig** (interface, implemented by `GameConfigImpl`) is loaded from `game_config.json` at startup and provides read-only access to all game-type settings.
- **GameController** (interface, implemented by `GameControllerImpl`) reads `GameConfig` to determine which subsystems to activate:
  - If `build_mode.enabled` is true, the `GamePlayScreen` presents the vehicle construction UI (via `VehicleBuildManager` and `VehicleWorkspace`) before starting simulation.
  - It selects the appropriate `CameraBehavior` based on `camera.mode`.
  - It configures the `InputRouter` with the key-to-action mappings from `input_mapping`.
  - It registers the appropriate `WinConditionChecker` and `LossConditionChecker` instances based on `win_condition` and `loss_conditions`.
  - It passes `gameplay_modifiers` (e.g., `instant_death_on_obstacle`, `auto_move_speed`, `jump_force`) to the relevant subsystems.
- **GameControllerImpl** runs the JavaFX `Timeline` at 60 FPS. Each frame it commands engine steps and view draws.
- **ScoreCalculator** reads `scoring` config to determine which metrics to track and how to compute the final score and star rating.
- **PhysicsEngine** receives `physics_overrides` from the game config or level file, allowing per-game or per-level gravity, friction, and restitution tuning.

### Input Pipeline

- **InputRouter** (interface, implemented by `InputRouterImpl`) maps hardware keys to game commands.
- When a key is pressed, `InputRouter` looks up the corresponding `Action` and calls `execute()`.
- `Action` (interface) uses the command pattern. Each implementation (`FanAction`, `BalloonAction`, `FrameAction`) knows how to activate or deactivate its associated component.
- **ComponentActivator** triggers component state changes (activated on/off) based on `Action` execution. This separates input handling from physics logic — the `InputRouter` never directly touches forces or velocities.

### No-Code Level and Vehicle Building (Authoring Environment)

The authoring environment is a separate module (`authoring/`) with its own `api/` and `internal/` packages, sharing data formats and component definitions with the gameplay runtime.

#### Controller Layer

- **AuthoringController** (interface, implemented by `AuthoringControllerImpl`) is the top-level coordinator for level editing. It translates JavaFX `MouseEvent`s to grid placements, manages tool selection via `AuthoringToolSelector`, and coordinates save/load operations with `LevelSaver` and `LevelLoader`. Collaborates with `AuthoringCanvas`, `LevelLoader`, `LevelSaver`, and `ComponentPalette`.
- **AuthoringToolSelector** (interface) manages the active editor tool (Draw, Erase, Place Goal, etc.).
- **VehicleBuildManager** validates part placement against `VehicleBuildRulesManager` constraints and creates weld Joints when the player presses "Play."
- **VehicleWorkspace** (interface) defines the rules for the vehicle assembly sandbox.
- **VehicleBuildRulesManager** (interface) defines what a designer can restrict (e.g., max parts, required pig, connectivity rules).

#### View Layer

- **AuthoringScreen** (implements `AppScreen`) hosts the authoring UI. Contains `AuthoringCanvas` and `ComponentPalette`.
- **AuthoringCanvas** (interface, implemented by `AuthoringCanvasImpl`) is the visual grid where blocks are placed. Handles drag-and-drop, snap-to-grid, and visual feedback for invalid placements.
- **ComponentPalette** (interface) provides a visual list of available parts. Reads from `GameConfig` to determine which components are available for the current game type.

#### Validation

- **GridValidationLogic** ensures all elements are in bounds and win conditions exist (start position present, at least one goal) before allowing a save. Collaborates with `LevelSaver`.

#### Data Flow

- The authoring environment produces the same JSON files consumed by the runtime: `level.json` files (via `LevelSaver`) and `vehicle.json` files (via vehicle serialization). There is no separate authoring format.
- `LevelLoader` is used by both the authoring environment (to load a level for editing) and the gameplay runtime (to load a level for playing). Same interface, same parsing logic, same schema.
- `GameConfig` files are read by the authoring environment to determine which tools to display. If the game type has `build_mode.enabled: false`, the vehicle workspace is hidden.

#### Test Play

The authoring environment's "Test Play" feature requests a transition to `GamePlayScreen` via the `ScreenManager`, passing the current level data and vehicle config. When the designer exits, `ScreenManager` transitions back to `AuthoringScreen` with the editing state restored unchanged.

### Rendering Pipeline

The `Renderer` (interface, implemented by `RendererImplementation`) consumes read-only data from GameObjects via the `Renderable` interface and paints them using JavaFX tools. Two rendering modes are supported, controlled by per-object data flags:

- **Tiled rendering** (`tile_texture: true`): Used for terrain blocks (ground, walls, ramps). The Renderer reads the object's size and repeats the texture image across the full width and height. A 60×2 ground block with a grass texture gets tiled 60 times horizontally, 2 times vertically.
- **Sprite rendering** (`render_mode: "sprite"`): Used for discrete objects (coins, flags, vehicle components, obstacles like sawblades). The Renderer draws the texture as a single image scaled to the object's size.

For triangle-shaped objects (ramps, spikes), the `direction` field (`"up"`, `"down"`, `"left"`, `"right"`) tells the Renderer which way to orient the shape. The same spike texture and collision shape can point in any direction without needing separate assets.

### Camera System

The **Camera** (interface) determines which portion of the World is visible. It provides a viewport transform that the `Renderer` applies before drawing. The Camera delegates its movement logic to a **CameraBehavior** (interface):

- **FollowPlayerBehavior**: Viewport centers on the player's vehicle, with optional lead-ahead in the direction of motion. Used for Bad Piggies.
- **AutoScrollBehavior**: Viewport moves rightward at a constant speed defined in the config, independent of player position. Used for Geometry Dash and Jetpack Joyride.

Both behaviors collaborate with `Camera` and `World`. The `GameController` calls `Camera.update()` each frame after physics, then passes the Camera to the Renderer for viewport transformation.

### Scoring, Win Conditions, and Loss Conditions

The **GameController** evaluates win and loss conditions each frame after physics and collision updates.

- **WinConditionChecker** (interface) evaluates World state to determine victory. Implementations: `ReachGoalChecker` (subject intersects finish line), and future checkers for endless survival and collect-all modes.
- **LossConditionChecker** (interface) evaluates World state to determine defeat. Implementations: `VelocityStopChecker` (vehicle stopped and can't recover), and future checkers for out-of-bounds and instant death.
- The active checkers are instantiated based on the `win_condition` and `loss_conditions` arrays in `game_config.json`. Adding a new condition type means writing a new checker implementation and registering its type string — no changes to `GameController`.
- **ScoreCalculator** (interface, implemented by `ScoreCalculatorImpl`) runs on level completion (or on death for endless modes). It reads the scoring config from `GameConfig` and evaluates against `World` state to compute: base completion score, time bonus, distance score, progress percentage, and coin totals. Star ratings are determined by comparing the total score against `star_thresholds`.

---

## Shared Data Model

### Immutable Records

- **Vector** (record) — immutable X/Y representation for physics math. Used throughout the `PhysicsEngine`, `GameObject` properties, and force calculations.
- **Pose** — contains position and orientation. Contained by every `GameObject`.
- **VehicleRenderData** (record) — holds position, angle, and components grid for passing vehicle state from model to view without exposing mutable model objects.
- **ComponentRenderData** — holds per-component rendering information (texture, render mode, direction).

These records ensure that the view layer never holds references to mutable model objects. The model produces fresh records each frame, and the `Renderer` consumes them as read-only data.

---

## Module Relationship Diagram

See the CRC cards diagram (Version 2) in `doc/plan/wireframe/` for the full class relationship picture. The high-level module dependency structure, reflecting the `api/` and `internal/` package separation:

```
┌──────────────────────────────────────────────────────────────┐
│                        config/                                │
│                                                              │
│  api/  GameConfig (interface)                                │
│  internal/  GameConfigImpl                                   │
│                                                              │
│  Reads: game_config.json, level.json, vehicle.json           │
└──────────────┬──────────────────────────┬────────────────────┘
               │                          │
               ▼                          ▼
┌────────────────────────┐   ┌─────────────────────────────────┐
│    authoring/           │   │        player/                   │
│                         │   │                                 │
│ api/                    │   │ api/                             │
│  AuthoringController    │   │  GameController (interface)     │
│  AuthoringCanvas        │   │  InputRouter (interface)        │
│  AuthoringToolSelector  │   │  WinConditionChecker            │
│  ComponentPalette       │   │  LossConditionChecker           │
│  VehicleBuildRules-     │   │  ScoreCalculator                │
│    Manager              │   │                                 │
│  VehicleWorkspace       │   │ internal/                       │
│                         │   │  GameControllerImpl             │
│ internal/               │   │  InputRouterImpl                │
│  AuthoringControllerImpl│   │  ReachGoalChecker               │
│  AuthoringCanvasImpl    │   │  VelocityStopChecker            │
│  VehicleBuildManager    │   │  ScoreCalculatorImpl            │
│  GridValidationLogic    │   │                                 │
└───────────┬─────────────┘   └──────────────┬──────────────────┘
            │                                │
            ▼                                ▼
┌──────────────────────────────────────────────────────────────┐
│                          core/                                │
│                                                              │
│ api/                                                         │
│  GameEngine (interface)      World (interface)                │
│  GameObject (interface)      Joint (interface)                │
│  Renderable (interface)      CollisionDetector (interface)    │
│  CollisionStrategy (iface)   CameraBehavior (interface)       │
│  Camera (interface)          LevelLoader (interface)          │
│  Renderer (interface)        LevelSaver (interface)           │
│  AppScreen (interface)       ScreenManager (interface)        │
│  Action (interface)          Vector (record)                  │
│                                                              │
│ internal/                                                    │
│  PhysicsEngine               WorldImpl                       │
│  CollisionDetectorImpl       RendererImplementation          │
│  LevelLoaderImpl             LevelSaverImpl                  │
│  ScreenManagerClass          FollowPlayerBehavior            │
│  AutoScrollBehavior                                          │
│                                                              │
│ internal/objects                                             │
│  GameComponent (abstract, implements GameObject)              │
│    ├── VehicleComponent (abstract)                           │
│    │     ├── DriveComponent (abstract)                       │
│    │     │     ├── Fan                                       │
│    │     │     └── Balloon                                   │
│    │     └── StructuralComponent (abstract)                  │
│    │           ├── Wheel                                     │
│    │           └── Frame                                     │
│    ├── TerrainComponent (abstract)                           │
│    ├── PowerupComponent (abstract)                           │
│    └── FinishLineComponent (abstract)                        │
│                                                              │
│ internal/collision                                           │
│  ElasticCollisionStrategy                                    │
│  InelasticCollisionStrategy                                  │
│                                                              │
│ view/                                                        │
│  GamePlayScreen (implements AppScreen)                       │
│  GameMenuScreen (implements AppScreen)                       │
│  AuthoringScreen (implements AppScreen)                      │
└──────────────────────────────────────────────────────────────┘
```

**Dependency rules:**
- `authoring/` and `player/` both depend on `core/api/` — never on each other.
- `authoring/` and `player/` never import from `core/internal/` — they only see interfaces.
- `config/` is read by both `authoring/` and `player/` for game-type settings.
- `core/view/` contains `AppScreen` implementations that the `ScreenManager` routes between.
- Concrete classes within `internal/` packages can depend on other `internal/` classes in the same module, but are never exposed outside.