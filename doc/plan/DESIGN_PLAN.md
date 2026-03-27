# OOGA Design Plan — Good Piggies

## Design Goals

The design prioritizes **encapsulation of core simulation logic** while enabling **flexible extension of gameplay elements**. The architecture separates concerns into distinct layers:

- **Simulation Engine**: Physics calculations, collision detection, and world state management are encapsulated behind stable APIs. These subsystems are "closed" to modification — they operate as pure, deterministic engines that do not vary across game genres.
- **Game Content**: GameObjects, Action Strategies, and Collision Strategies are "open" for extension. New component types, vehicle behaviors, and interactive objects can be added without modifying the engine core.
- **Game Configuration**: A data-driven configuration layer (`game_config.json`) controls high-level gameplay behavior — build mode, camera mode, input mapping, scoring rules, and win/loss conditions. Switching from a Bad Piggies-style sandbox to a Geometry Dash-style auto-scroller requires changing a config file, not engine code.
- **Authoring vs. Runtime**: The authoring environment and gameplay runtime share the same underlying data formats and world representation, eliminating duplication and ensuring that player-created levels behave identically during play.

### Flexible

- **GameObject** — to create different types of components without changing the code
- **Action and Collision Strategies** — interchangeable behaviors attached to GameObjects
- **Position & Orientation** wrapped in class
- **Object properties**: velocity, mass, and force
- **LevelMap**: start position, destination, game objects, terrain layout
- **Win/Loss Conditions** — configurable per game type (reach goal, survive endlessly, collect all items)
- **Camera Behavior** — switchable between follow-player, auto-scroll, and fixed modes
- **Scoring Rules** — time bonus, distance scoring, progress percentage, coin values, star thresholds
- **Input Routing** — key-to-action mappings configurable per game type
- **Rendering Mode** — per-object control over tiled textures vs. single sprites

### Encapsulated

- **Physics Engine** — pure calculation, responsible only for numerical simulation of motion
- **Collision Detector** — responsible for detecting overlaps/intersections between objects, produces collision events or collision pairs
- **World** — holds all GameObjects, knows the game state, gives list of renderables to view
- **Renderer** — consumes read-only render data, handles tiling and sprite drawing, does not contain game logic
- **Level Persistence** — serialization/deserialization of level and vehicle data to/from JSON files

---

## Primary Architecture: Open/Closed Principle

### Closed for Modification (Encapsulated)

| Subsystem | Encapsulated Responsibility | Why Closed |
|---|---|---|
| **Physics Engine** | Numerical integration of motion (velocity, force, mass), constraint solving, and joint enforcement. | Physics is domain-agnostic; it should work identically for any game using realistic motion. Changing its internal algorithm should not affect game-specific code. |
| **Collision Detector** | Intersection testing, contact pair generation, and broad-phase culling. | The detection logic is independent of what happens after a collision. It produces raw collision events but does not interpret them. |
| **World Manager** | Storage and lifecycle of GameObjects and Joints; provides read-only views for rendering and simulation. | World state management is a fixed service. How objects are stored or enumerated should not vary by game type. |
| **Renderer** | Drawing loop, canvas management, texture tiling for terrain, sprite rendering for discrete objects. Reads `tile_texture` and `render_mode` flags from component data to determine how each object is drawn. | The view layer consumes read-only graphical data. It does not contain game logic and can be swapped for different front-ends. |
| **Level Persistence** | Serialization/deserialization of level data, vehicle configurations, and game configs to/from JSON files. | The format for storing levels is stable; saving and loading should work consistently across authoring and runtime modes. |

### Open for Extension (Flexible)

| Subsystem | Extension Mechanism | What Can Be Added Without Engine Changes |
|---|---|---|
| **GameObject Hierarchy** | Inheritance and composition. | New vehicle parts (e.g., rockets, springs), terrain types (e.g., ice, mud), power-ups, and goal objects. Each object defines its own mass, collision shape, and rendering sprite. |
| **Action Strategies** | Command pattern with interchangeable implementations. | New player-controllable actions (e.g., turbo boost, eject part, toggle balloon inflation). Actions can be triggered by input or game events. |
| **Collision Strategies** | Strategy pattern with pluggable handlers. | New collision effects: sticky surfaces, teleporters, damage zones, coin collection, level completion. Strategies can be assigned per GameObject or per collision pair. |
| **Game Configuration** | Data-driven config file (`game_config.json`) loaded at startup. | New game types can be defined entirely through configuration: set `build_mode.enabled`, choose a `camera.mode`, define `input_mapping`, configure `win_condition` and `loss_conditions`, tune `scoring` rules, and set `gameplay_modifiers`. No code changes needed to create a new game variation. |
| **Camera System** | Mode-based camera with pluggable behaviors. | `follow_player` (tracks vehicle with lead-ahead), `auto_scroll` (constant-speed rightward scroll), `fixed` (static camera for single-screen levels). New modes can be added as implementations of a CameraBehavior interface. |
| **Scoring System** | Rule-based scoring configured per game type. | Completion bonuses, time bonuses, distance-based scoring, progress-percentage scoring, coin multipliers, and star thresholds. All configured in `game_config.json`, evaluated by a generic ScoreCalculator. |
| **Win/Loss Conditions** | Configurable condition checkers read from game config. | `reach_goal` (pig reaches finish line), `endless_survival` (survive as long as possible), `collect_all` (gather every collectible). Loss conditions: `pig_health_zero`, `collision_with_obstacle` (instant death), `out_of_bounds`. New conditions can be added as implementations of a ConditionChecker interface. |
| **Level Map** | Grid-based placement interface with serializable data. | Level layouts, terrain arrangements, start and goal positions, and placement of interactive objects. All are defined in data files, not code. |
| **Input Router** | Key-to-Action mapping with runtime configuration. | New control schemes, remappable keys, and input modes (e.g., driving vs. building) can be added without altering core input handling. |

---

## API Services and Collaboration

The following describes how major subsystems interact through APIs to achieve the project goals.

### Physics-Based Vehicle Construction

- **GameObject** exposes properties: mass, position, velocity, applied forces, and collision shape.
- **DriveComponent** (a subtype of GameObject) defines an **Action Strategy** that, when activated, applies force to the parent vehicle.
- **Wheel** and **StructuralComponent** define how parts connect via **Joints**, which the Physics Engine resolves as constraints.
- **VehicleCollisionStrategy** handles impacts: if impulse exceeds a threshold, it requests the **World Manager** to destroy specific Joints, simulating part breakage.

### Game Configuration and Genre Switching

- **GameConfig** is loaded from `game_config.json` at startup and provides read-only access to all game-type settings.
- **GameController** reads GameConfig to determine which subsystems to activate:
  - If `build_mode.enabled` is true, it initializes the **CarBuilderController** and presents the vehicle construction UI before gameplay begins.
  - It selects the appropriate **CameraBehavior** based on `camera.mode` (e.g., `follow_player` vs. `auto_scroll`).
  - It configures the **InputRouter** with the key-to-action mappings from `input_mapping`.
  - It registers the appropriate **WinConditionChecker** and **LossConditionCheckers** based on `win_condition` and `loss_conditions`.
  - It passes `gameplay_modifiers` (e.g., `instant_death_on_obstacle`, `auto_move_speed`, `jump_force`) to the relevant subsystems.
- **ScoreCalculator** reads `scoring` config to determine which metrics to track and how to compute the final score and star rating.
- **PhysicsEngine** receives `physics_overrides` from the game config or level file, allowing per-game or per-level gravity, friction, and restitution tuning.

### No-Code Level and Vehicle Building (Authoring Environment)

The authoring environment is organized as a separate module with its own controller layer, sharing data formats and component definitions with the gameplay runtime.

#### Architecture

- **AuthoringController** is the top-level coordinator for the level editing mode. It manages tool selection (terrain, obstacle, goal, collectible placement), delegates canvas interactions to **AuthoringCanvas**, and coordinates save/load operations with **LevelSaver** and **LevelLoader**.
- **CarBuilderController** is a sibling controller that manages the vehicle editing mode. It manages the vehicle construction grid, delegates to **VehicleBuildRulesManager** for attachment validation, and shares the same component library used by the gameplay runtime.
- **ComponentPalette** is a view-layer class that displays available terrain blocks, obstacle types, vehicle parts, and goal objects. It reads from the shared component library (the same source that `VehicleBuildRulesManager` uses for gameplay validation) so authoring and runtime stay in sync about what components exist and what their default properties are.
- **AuthoringCanvas** renders the level-in-progress on a grid, handles drag-and-drop placement, snap-to-grid, and visual feedback for invalid placements (overlap warnings, out-of-bounds indicators).

#### Data Flow

- The authoring environment produces the same JSON files consumed by the runtime: `level.json` files (via LevelSaver) and `vehicle.json` files (via vehicle serialization). There is no separate authoring format.
- **LevelSaver** validates the level before writing (checks for start position, at least one goal, no out-of-bounds objects) and serializes to the shared level schema.
- **LevelLoader** is used by both the authoring environment (to load a level for editing) and the gameplay runtime (to load a level for playing). Same class, same parsing logic, same schema.
- **GameConfig** files are read by the authoring environment to determine which tools to display. For example, if the game type has `build_mode.enabled: false`, the car builder tab is hidden.

#### Collaboration with Runtime

- The authoring environment's "Test Play" feature temporarily hands the current level data and vehicle config to the **GameController**, which initializes the physics engine and runs normal gameplay. When the designer exits test mode, the GameController is discarded and the authoring state is restored unchanged.

### Rendering Pipeline

The Renderer consumes read-only data from GameObjects via the Renderable interface and draws them to the screen. Two rendering modes are supported, controlled by per-object data flags:

- **Tiled rendering** (`tile_texture: true`): Used for terrain blocks (ground, walls, ramps). The Renderer reads the object's size and repeats the texture image across the full width and height. A 60×2 ground block with a grass texture gets tiled 60 times horizontally, 2 times vertically.
- **Sprite rendering** (`render_mode: "sprite"`): Used for discrete objects (coins, flags, vehicle components, obstacles like sawblades). The Renderer draws the texture as a single image scaled to the object's size.

For triangle-shaped objects (ramps, spikes), the `direction` field (`"up"`, `"down"`, `"left"`, `"right"`) tells the Renderer which way to orient the shape. The same spike texture and collision shape can point in any direction without needing separate assets.

The **Camera** determines which portion of the world is visible. It provides a viewport transform that the Renderer applies before drawing. Camera behavior is selected from the game config:
- `follow_player`: Viewport centers on the player's vehicle, with optional lead-ahead in the direction of motion.
- `auto_scroll`: Viewport moves rightward at a constant speed defined in the config, independent of player position.

### Scoring, Win Conditions, and Loss Conditions

The **GameController** evaluates win and loss conditions each frame after physics and collision updates.

- **WinConditionChecker** is an interface with implementations for each condition type: `ReachGoalChecker` (subject intersects finish line), `EndlessSurvivalChecker` (no win — game runs until loss), `CollectAllChecker` (all collectibles gathered).
- **LossConditionChecker** is an interface with implementations for: `HealthZeroChecker` (pig health depleted), `InstantDeathChecker` (any lethal collision), `OutOfBoundsChecker` (subject leaves level boundaries).
- The active checkers are instantiated based on the `win_condition` and `loss_conditions` arrays in `game_config.json`. Adding a new condition type means writing a new checker implementation and registering its type string — no changes to GameController.
- **ScoreCalculator** runs on level completion (or on death for endless modes). It reads the scoring config to compute: base completion score, time bonus (if `time_bonus: true`), distance score (if `distance_scoring: true`), progress percentage, and coin totals. Star ratings are determined by comparing the total score against `star_thresholds`.

---

## Module Relationship Diagram

See the CRC cards diagram in `doc/plan/wireframe/` for the full class relationship picture. The high-level module dependency structure is:

```
┌─────────────────────────────────────────────────────────┐
│                    Game Configuration                    │
│              (game_config.json, level.json)              │
└──────────────┬──────────────────────┬───────────────────┘
               │                      │
               ▼                      ▼
┌──────────────────────┐  ┌──────────────────────────────┐
│  Authoring Environment│  │       Game Runtime            │
│                       │  │                              │
│  AuthoringController  │  │  GameController              │
│  CarBuilderController │  │    ├── InputRouter           │
│  ComponentPalette     │  │    ├── CameraBehavior        │
│  AuthoringCanvas      │  │    ├── WinConditionChecker   │
│                       │  │    ├── LossConditionChecker  │
│                       │  │    └── ScoreCalculator       │
└───────────┬───────────┘  └──────────────┬───────────────┘
            │                             │
            ▼                             ▼
┌─────────────────────────────────────────────────────────┐
│                   Shared Core Engine                     │
│                                                         │
│  GameEngine (interface)                                 │
│    └── PhysicsEngine                                    │
│  CollisionDetector + CollisionStrategies                │
│  World (holds all GameObjects)                          │
│  Renderer + Renderable                                  │
│  LevelLoader / LevelSaver                               │
│                                                         │
│  GameObject Hierarchy:                                  │
│    GameComponent (abstract, implements Renderable)       │
│      ├── Vehicle (extends GameObject)                   │
│      │     └── VehicleComponents (Drive, Structural)    │
│      ├── TerrainComponent                               │
│      ├── PowerupComponent                               │
│      ├── FinishLineComponent                            │
│      └── CollisionStrategies (per component type)       │
└─────────────────────────────────────────────────────────┘
```

Both the Authoring Environment and Game Runtime depend on the Shared Core Engine. They never depend on each other. The Game Configuration layer is read by both to determine behavior.