# DESIGN Document for OOGASalad
### Good Piggies
### Georgiy Zemlevskiy, Iurii Beliaev, Paulina Vvedenskaya, Michael Li, Emilio Garcia, Mason Wu, Xuye Chen


## Team Roles and Responsibilities

 * Georgiy Zemlevskiy: My responsibility was most of the physics engine: mostly collision handling and the component abstractions, although I worked on joints (which attach components to each other in different ways) as well. Smaller features include drag from the surrounding medium (air), and drive components.

* Iurii Beliaev: My responsibilities throughout the Sprints included the following:
  * Discussion and Agile Lead - I led the group meetings, encouraged participation and following Agile process, I encouraged engagement in the meetings and tried to make sure everybody knows what they are doing and what everybody doing. I also led the standup meetings
  * Designer - I was extremely engaged in the architecture decision making and was the one who created the final version of the initial design based on our discussions. Then we discussed it in a team and the team agreed. I was responsible for the design of the initial APIs
  * VIEW API Lead - I created the foundation for the entire View API and rigid structure (`Renderer`, `Renderable`, `Camera`, `CameraBehavior`, `AppScreen`, `AbstractAppScreen` logic. 
  * Engine debugger - during the sprints 3 and 4 with the LLM Teammate I had to debug the engine and make it behave realistically
  * Worked on engine - implemented some of the engine feature with the LLM Teammate to finish its integration and implementation
  * Engine and Joints Integration - during the sprints 3 and 4 I fixed the integration between the Engine and Joints
  * Merge specialist - I spent a very long time merging the work of other people together
  * Debugger - during the last 2 sprints, I had to fix a lot of bugs to make our product demoable
  * Worked on Controller and pulling everything together
  * Overall generalist, implemented a good amount of features


 * Paulina Vvedenskaya: I worked across gameplay control, screen flow, authentication/accounts, vehicle building,
   data schema evolution, and late-stage refactoring. Early in the project, I implemented the screen manager,
   game controller loop, input routing, pause/restart actions, camera following, and navigation outcomes.
   I also built the login, account creation, profile, update profile, local authentication service,
   SQLite user repository, password hashing, and auth controller integration.

   Later, I worked on vehicle builder behavior, sprites, vehicle part registry, placement validation, build rules,
   main character support, goal line behavior, HP/damage systems, pickup components, collision effects,
   button binding, camera target resolution, and gameplay session wiring. Near the end, I refactored several
   large systems, including breaking apart `GameEngineImpl` into focused physics collaborators, extracting
   gameplay world/renderable/session helpers, improving authoring viewport behavior, fixing background/track
   color support, and cleaning up level/game data.

 * Michael Li: I focused on the authoring environment, data layer, terrain systems, and gameplay wiring
   throughout the project. For sprint 1, I built the foundation for the level editor, implementing the level
   loader and saver (JSON parsing with Jackson), authoring controller, game config, and authoring canvas.
   I later expanded it with attribute and terrain property panels, object selection, draw/erase tools,
   and undo/redo functionality. A major area of ownership was the spline terrain system, including parabola
   and terrain spline classes, interactive knot authoring, and spline rendering. I also handled the spawn
   point system, vehicle-terrain interaction, multi-polygon collision detection, and a responsive camera with
   zoom support. Because the splines themselves changed how the ground was rendered, I additionally rewrote
   the level validator and level saver as part of an overhaul of the game catalog editing flow.
   In the final sprint, I redesigned the finish line as a draggable stripe, added an HP display to the HUD,
   fixed terrain block persistence and finish line win detection, resolved vehicle builder palette crashes,
   and corrected a SmallPig name mismatch bug. I also refactored the authoring canvas rendering.

 * Emilio Garcia: I focused mainly on the authoring environment, game catalog authoring, cheat system,
   and AI/LLM level generation. Early in the project, I created the first game menu, authoring entry screen,
   screen factory, authoring screen layout, component palettes, CSS styling, and screen switching support.
   I also worked on making authoring views use properties files and integrating the authoring canvas into
   the main UI. I implemented the LLM help/chat window and AI level generation pipeline, including prompt
   files, generated level normalization, validation, JSON building, and generated terrain/object/pickup
   handling. I also designed and implemented the cheat system: cheat actions, key bindings, input handling,
   runtime context, target resolution, effect registry, cheat data records, validation, and cheat authoring UI.
   In the final sprint, I refactored the authoring and game catalog code by extracting controllers, draft
   models, sprite import services, catalog tabs, file actions, layout factories, and authoring mode controllers.

 * Mason Wu: I worked on the authoring canvas, level validation, scoring, player progression, logging,
   and level-selection flow. Early in the project, I implemented the initial authoring canvas view, component
   palette support, placed object representation, and terrain block/controller refactors. I also worked on
   undo/redo, fill/autofill behavior, occupancy grid logic, drag-and-drop placement, clear actions, save/load
   wiring, and validation for level files. I also implemented gameplay scoring and progression systems,
   including score calculation, elapsed time/progress tracking, play history persistence, user progression,
   locked/unlocked level behavior, furthest-level tracking, level scanning, result screen statistics,
   and terminal outcome handling. I also added logging support through log4j configuration and event logging
   for startup/shutdown, screen transitions, auth, game outcomes, HP changes, AI level generation,
   help chat sessions, and caught/uncaught exceptions.

 * Xuye Chen: I worked mainly on the game component model, world implementation, joints, and vehicle stability.
   Early in the project, I implemented the base `GameComponent` abstraction with position, velocity, force,
   mass, hitboxes, friction, and active state. I also created concrete component types such as vehicle
   components, drive components, finish line components, power-up components, and pig/main-character
   components. I also implemented the `World` abstraction for storing and filtering game objects, along with
   the joint interface and joint implementations such as rigid, rope, spring, and revolute joints. Later,
   I worked on dynamic/static component abstractions, rigid joint fixes, vehicle stability issues,
   wheel/terrain behavior, component compaction, cursor behavior fixes, and terrain descriptions.


## Design Goals

 * Goal #1: Make the engine as flexible as possible so that we can support many variations of our game.
   This includes making game components, collision handling, and joint resolution extensible by making them
   as independent as possible from each other. Specifically, we wanted to make it easy to not only write
   new game components but also integrate them into the game, namely into the joint resolution and collision
   detection and handling. That is, we wanted all of our logic to be as independent as possible from the
   components and from other parts of the logic.

 * Goal #2: Make game variation data-driven so designers can create new worlds, levels, components, terrain,
   static objects, and assets without changing Java code. Instead of hard-coding one Bad Piggies game, we
   wanted `game.json` to define the available game pieces and `level.json` to define the map. This supports
   the Open-Closed Principle because new game content should usually be added by extending data rather than
   modifying core engine/controller classes.

 * Goal #3: Keep the major systems separated by responsibility so that authoring, data loading/saving,
   gameplay, physics, and rendering could evolve independently. The authoring environment should create valid
   data, the data layer should translate files into runtime objects, the engine should simulate physics
   without knowing about UI screens, and the view/controller code should coordinate user workflows without
   owning physics rules.


#### How were Specific Features Made Easy to Add

 * Game Components: We made game components easy to add by defining an interface (`GameObject`) and a
   hierarchy with abstract classes that new classes (of new component types) could extend, only implementing
   what they needed. See the Engine section for more details.

 * New game variations: We made new game variations easier to add by moving game-specific information into
   data files. A new game can define its own components, sprites, terrain types, static objects, collision
   actions, controls, and background assets in `game.json`, while individual levels reference that catalog
   through `level.json`. This means a Mario-style game can use the same engine and player flow as Bad Piggies
   while changing the visual theme and component definitions.

 * New levels and authored maps: We made new levels easier to add through the authoring environment and the
   `LevelLoader`/`LevelSaver` APIs. The authoring tool lets designers place terrain, spawn points, static
   objects, pickups, hazards, goal lines, and background images, then saves them into level files. The runtime
   does not need to know whether a level was hand-authored or loaded from an existing folder because both go
   through the same data layer.

 * New screens: Any new player-facing screen only requires extending `AbstractAppScreen`, overriding
   `createRoot()`, registering with `ScreenFactory`, and adding one value to the `Outcome` enum.
   No existing screens need to change.


## High-level Design

#### Core Classes and Abstractions, their Responsibilities and Collaborators

 * **Physics Engine (Georgiy, Iurii):** The `GameObject` interface heads the component hierarchy, and abstract class `GameComponent` defines shared implementation for all components — `getPolygon()`, `getStaticState()`, and since it extends `Renderable`, the methods required for the view to render it. There are three base component types: `StaticComponent` (immovable terrain, no `DynamicState`), `DynamicComponent` (general-purpose moving objects with `DynamicState`), and `PassthroughComponent` (objects other objects pass through without state changes). `RigidBody` is extended by both `DynamicComponent` and `StaticComponent` for code sharing in collision and joint solvers. Abstract class `DriveComponent` extends `DynamicComponent` and implements `Driveable` to allow a thrust force on user input. Components implement `Damageable` to gain HP properties. Geometry is represented through convex polygons (`ConvexPolygon` — a list of points relative to the object's center). All objects hold a `StaticState` (linear and angular position); dynamic objects additionally hold `DynamicState` (linear and angular velocities/accelerations), with linear quantities as `Vector` and angular as `double`.

 * **Collision Dispatcher (Georgiy):** `CollisionDispatcher` holds a "collision matrix" — a `Map` from pairs of component types to their `CollisionSolver`. Different solver logic is needed for different type combinations: a static object must not move in a dynamic-static collision, and a powerup must not affect motion in a dynamic-powerup collision. `CollisionSolver` is a functional interface implemented by `DynamicDynamicSolver`, `DynamicStaticSolver`, etc. Shared impulse math (relative velocity, positional correction, impulse computation) lives in `ImpulseSolverUtilities`; inverse mass and inverse moment of inertia are precomputed on `RigidBody` at construction to avoid repeated division per contact per frame.

 * **Physics Orchestration (Georgiy):** `World` holds all game objects, joint constraints, and global physics variables (drag, gravity via `PhysicsEnvironment`). It tracks active and active-dynamic object subsets for engine convenience. `GameEngineImpl` runs a fixed-timestep loop (1/60 s, 4 substeps per frame) independent of the render frame rate. Each substep: `DynamicComponentUtilities` applies forces and integrates velocities/positions; `SweptCollisionResolver` checks each component's full path through the timestep to prevent tunnelling against static objects; `CollisionPhase` runs `CollisionDetectorImpl` (implementing `CollisionDetector`, which returns a `Collision` context object on overlap) across all relevant pairs, then applies warm-starting from a cached impulse store, and finally calls `CollisionDispatcher.dispatch()` for impulse and positional correction in separate passes.

 * **Joints (Georgiy / Xuye / Paulina):** The `Joint` interface defines the constraint contract — `apply(dt)` enforces positional/angular constraints each substep, and `applyVelocity()` zeroes relative velocity that would violate the constraint. `AbstractJoint` centralises endpoint management and common displacement calculations. Concrete subclasses encode different rules: `RigidJoint` (fixed connection), `SpringJoint` (stretch with rest-length pull-back), `RopeJoint` (maximum distance limit), `RevoluteJoint` (rotation around a shared anchor). The engine delegates joint resolution to `JointSolver`, keeping the main loop free of per-joint-type logic. Adding a new joint type requires only a new `Joint` implementation.

 * **Screen Controller / Navigation API (Iurii / Paulina):** The central principle is that no screen knows
   about any other screen — navigation is owned by the controller. `AppScreen` declares `getRoot()` (the
   JavaFX `Parent` node placed in the shared `Scene`) and `onEnter()` (called on every activation).
   `AbstractAppScreen` adds the `createRoot()` template method with caching so `getRoot()` is instant on
   subsequent calls; every concrete screen (`GameplayAppScreen`, `PlayerEntryAppScreen`, etc.) extends it
   and only implements `createRoot()`. Navigation signals flow through a `Consumer<Outcome>` injected at
   construction — screens fire outcomes (e.g. `Outcome.OPEN_PLAYER_ENTRY`) without knowing what should
   happen next. `ScreenManager` exposes `registerScreen`, `switchScreen`, and `onOutcome`;
   `ScreenManagerImpl` maps outcome values to screen names; `ScreenFactory` builds every screen and injects
   the `onOutcome` lambda.

 * **View API — Renderable, Camera, Renderer (Iurii):** `Renderable` is a minimal read-only interface
   exposing only `x()`, `y()`, and `rotation()`. The rendering system never needs internal model data —
   only position and orientation. `Camera` manages the viewport: `isVisible(renderable)` is called by the
   renderer to skip off-screen objects; `setPosition(x, y)` is called by the active behavior each frame.
   `CameraImpl.isVisible()` performs an AABB overlap against polygon bounds for `GameObject` instances and
   falls back to a point-in-viewport test for plain `Renderable` objects. `CameraBehavior` is a one-method
   Strategy (`update(Camera)`); `AbstractCameraBehavior` handles lerp-smoothing and optional world-boundary
   clamping, delegating only `desiredX(camera)` and `desiredY(camera)` to subclasses.
   `FollowPlayerCameraBehavior` centers the viewport on the tracked player component each frame.
   `RendererImpl` iterates the object list, culls with `camera.isVisible()`, then reads `x()`, `y()`,
   `rotation()` from each visible `Renderable` to position and orient its sprite.

 * **Game Controller API (Iurii / Paulina):** `GameController` is the central coordinator for a running
   gameplay session. On each JavaFX animation frame it calls `engine.step(dt)`. Three callback setters
   decouple downstream consumers: `setOnStatsTick(biCallback)` fires every frame with elapsed seconds and
   progress percentage for the HUD; `setOnTerminalScore(callback)` fires once with an immutable
   `GameplayScore` when the run ends; `setOnOutcome(callback)` fires once with `GAME_WON` or `GAME_LOST`
   so the screen manager can route to the correct result screen. Button-press events reach the controller
   through `handleButtonPress(id)` and `handleButtonRelease(id)`, which delegate to the registered
   `InputRouter`.

 * **Input Router API (Paulina):** `InputRouter` maps key codes and on-screen button IDs to `Action`
   implementations. Each `Action` has a single `execute()` method. Registering an action requires only an
   identifier and an `Action` instance, keeping input handling fully decoupled from the physics or
   game-logic code the action invokes.

 * **Configuration / Data API (Michael / Paulina):** `GameLoader.loadGame(file)` returns a `GameConfig`
   exposing `gameId()`, `mainCharacterId()`, `components()`, `staticComponents()`, `terrainTypes()`,
   `cheats()`, and key-value settings inherited from `Settings`. All Jackson `ObjectMapper` and `JsonNode`
   traversal is hidden inside `GameConfigImpl`; callers only see the interface.
   `LevelLoader.loadLevel(fileName, gameConfig, world, levelAttributes)` reads the level JSON,
   cross-references component parameters against `GameConfig` to resolve sprite URLs and physics values,
   instantiates the appropriate `GameComponent` subclasses, and calls `world.addComponent()` /
   `world.addJoint()` to populate the world. `LevelSaver.saveLevel(fileName, world, levelAttributes)`
   serialises current world state back to JSON; the authoring controller reaches it through
   `AuthoringPersistenceService`.

 * **Authoring API (Paulina / Michael / Emilio / Mason):** `AuthoringCanvas` is the editing surface. The controller
   uses it through `setSelectedPlaceable(placeable)` (stores the active palette selection),
   `addOverlay` / `clearOverlays` (spline drawing), `refreshGrid()` (full redraw from world state), and
   `setOnObjectSelected(handler)`. `AuthoringCanvasImpl.refreshGrid()` delegates to
   `AuthoringCanvasObjectRenderer`, which iterates `world.getObjects()`, converts each to
   `AuthoringPlaceable`, and renders it. `AuthoringPlaceable` provides display metadata (`getWidth`,
   `getHeight`, `getColor`, `getSprite`, `isTiled`, `getFriction`, `getRestitution`) and a
   `createAt(col, row, cellSize)` factory method that produces a new backend object anchored at the given
   grid position. `AuthoringController` coordinates editing: `initializeCanvasHandlers()` creates all input
   handlers and registers them on the canvas root; `saveLevel` / `loadLevel` commit in-progress splines
   before persisting. `CanvasInputHandler` is a one-method interface (`register(root)`) implemented by
   `HoverPreviewInputHandler`, `ClickPlaceInputHandler`, `PaletteDropInputHandler`, `SpawnDragInputHandler`,
   `GoalLineDragInputHandler`, and `SplineInputHandler` — each attaches its own listeners, keeping input
   concerns separated.

 * **Authentication / Accounts API (Paulina):** `AuthService` manages user sessions. Its methods — `login`,
   `register`, `updateAvatar`, `updateUsername`, `updatePassword`, `logout` — all return the sealed
   `AuthResult` interface: either `AuthResult.Success(PlayerProfile)` or `AuthResult.Failure(String reason)`.
   The view pattern-matches on this result to decide whether to navigate or display an error. `session()`
   returns a `SessionContext` with `isLoggedIn()` and `currentUser()` (a `PlayerProfile` with `username()`
   and `avatarPath()`). `PasswordHasher` is package-private, so the hashing algorithm can be swapped
   without touching any public API.

 * **Gameplay Session Integration (Iurii / Paulina / Mason):** `GameplaySessionFactory` is the Factory that assembles a full session,
   accepting only `(GameplayAppScreen, Consumer<Outcome>, VehicleLayout)` from the caller. Internally it
   calls `GameLoader.loadGame(gameId)` for the `GameConfig`, then
   `LevelLoader.loadLevel(levelPath, gameConfig, world, attributes)` to populate the world. The loader
   attaches `CollisionEffect` instances to every spawned component, registers drive and force `Action`
   instances through the `InputRouter`, and calls `spriteRegistry.registerFromConfig(gameConfig)` so the
   renderer can resolve sprites at runtime. The factory wires `WinConditionChecker`,
   `LossConditionChecker`, and `GameplayStatsTracker` into the `GameController` and starts the
   `GameplaySession`. Each frame: `camera.updateBehavior()` → `engine.step(dt)` (force accumulation,
   joint solving, integration, collision detection/resolution, collision-effect dispatch) →
   `statsTracker.sample(world, dt)` → win/loss check → `setOnStatsTick` →
   `renderer.render(camera, world.getComponents())` (culls with `camera.isVisible()`, reads `x()`, `y()`,
   `rotation()` from each visible component).


## Assumptions or Simplifications

 * Decision #1: The physics engine assumes all moving gameplay objects can be represented as rigid bodies
   with convex collision polygons. More complex shapes, like terrain splines, are approximated by multiple
   convex slabs so they still work with the same impulse solver.

 * Decision #2: The engine is largely meant for side-view vehicle/platformer games, not arbitrary 2D games.
   Gravity, friction, joints, and terrain are all built around vehicles or player bodies moving across
   authored ground.

 * Decision #3: Collision response is separated from collision detection. `CollisionDetector` only detects
   overlaps, while `CollisionDispatcher` chooses the solver for each pair of object categories.

 * Decision #4: Game variation is mostly data-driven through `game.json` and `level.json`, but brand-new
   low-level physics behaviors still require small Java additions, such as adding a new component type,
   collision solver, or effect implementation.

 * Decision #5: Components are represented through convex polygons. These are versatile enough to approximate
   most shapes, including circles (which would normally use a center/radius representation for perfection).

 * Decision #6: `Renderable` exposes only `x()`, `y()`, and `rotation()`. This fully decouples the view from
   model internals but requires a special-cased `instanceof GameObject` check in `CameraImpl.isVisible()` to
   perform accurate AABB culling for objects that have collision polygons. A cleaner future fix would be
   `Optional<Aabb> getBounds()` on `Renderable`.


## Changes from the Original Plan

 * Change #1: The idea to use a strategy pattern to handle collisions with different types of objects did not
   work and had to be replaced by the collision matrix. This way we could provide a lookup value to determine
   what specific type of collision existed within that part of the matrix.

 * Change #2: The original plan described many per-game behaviors as fully configurable, but in practice the
   stable approach was to keep the physics engine fixed and expose only safe variation points through data:
   component catalogs, terrain types, static collision actions, input forces, level physics, win/loss
   settings, and assets.

 * Change #3: Our original design stored all game information — available components, terrain types, physics
   constants, and level layout — in a single `level.json`. While straightforward, this meant that adding a
   second level required duplicating the entire component catalog, and creating a different game (e.g., Mario
   vs. Bad Piggies) was not feasible without a full rewrite. We split the data into two files: `game.json`
   at `data/games/<gameId>/game.json` holds the catalog shared across all levels of a game (component
   definitions with sprites and masses, terrain types, static object collision actions, button controls, and
   cheats), while `level.json` at `data/levels/<gameId>/<level>.json` holds only what is level-specific
   (terrain spline knots, spawn point, placed objects, gravity, drag, and the allowed vehicle subset).
   `GameConfig`/`GameLoader` are responsible for the former; `LevelLoader` loads the latter and
   cross-references the catalog to resolve sprites and physics values. The tradeoff is two loaders and a
   `gameType` field linking a level to its catalog, but a change to `game.json` propagates to all levels
   of that game automatically and new levels require no Java code changes.

 * Change #4: Vehicle stability required more engine work than expected. The solver moved toward a
   split-phase impulse approach with joint velocity solving, positional correction, warm starting, and
   special handling for rigid/revolute joints to avoid unstable jittering of the car's components.

 * Change #5: Impulse math that was originally duplicated across `DynamicDynamicSolver` and
   `DynamicStaticSolver` (four identical private methods each) was extracted into `ImpulseSolverUtilities`.
   `getInverseMass()` and `getInverseMomentOfInertia()` were added to `RigidBody` so the inverses are
   precomputed at construction rather than recomputed per contact per frame. This eliminated the duplicate
   code and created a single source of truth for impulse resolution math.


## How to Add New Features

#### Features Designed to be Easy to Add

 * New game variations through data files: Add a new folder under `data/games/<gameId>/game.json` and
   matching levels under `data/levels/<gameId>/`. The catalog can define different vehicle parts, sprites,
   static objects, terrain types, controls, masses, HP values, and collision actions without changing the
   engine. These files can be generated through the authoring environment provided the required assets
   (sprites, background image) are present.

 * New levels for an existing game: Create another `level.json` that references the existing `gameType`.
   Designers can change terrain splines, spawn location, gravity, drag, pickups, enemies, goal placement,
   background image, and allowed vehicle parts entirely through level data.

 * New visual themes / schematics: A game can look different by changing sprite paths, terrain colors,
   background images, and CSS/resource files. For example, Mario uses the same gameplay systems as the
   other games but swaps in Mario assets and different level data.

 * New pickup or hazard effects using existing action types: Static objects can be added in `game.json`
   with collision actions like `score`, `damage`, `heal`, or `finish`. Levels can then place those objects
   anywhere in the map without adding new Java code.

 * New types of game components: Extend one of the existing classes in the component hierarchy (or implement
   `GameObject` directly), then write a `CollisionSolver` for any new type pairs and register it in
   `CollisionDispatcher`'s solver map.

 * New screens: Extend `AbstractAppScreen`, implement `createRoot()`, register the screen in `ScreenFactory`
   with a new string key, add a matching value to the `Outcome` enum, and wire a button in any existing
   screen to fire that outcome. Zero changes to existing screens are required.

 * New camera behaviors: Extend `AbstractCameraBehavior` and implement `desiredX(camera)` and
   `desiredY(camera)`. Smoothing, clamping, and `setPosition` are inherited automatically.


#### Features Not Yet Done

 * Multiplayer / networking: The current architecture assumes one local `World` and one local gameplay
   session. To support multiplayer, we would need a networking layer, synchronized world state, deterministic
   or server-authoritative physics updates, and UI for joining or hosting sessions.

 * Ghost racing: A ghost racing mode would record a previous run's position over time and replay it as a
   non-interactive visual object during a later attempt. This fits the existing replay/progression direction,
   but we would need to persist time-stamped player positions and add a view/model object that renders the
   ghost without participating in collisions.

 * Sound effects and music: The project has visual assets and CSS/resource files, but not a full audio
   system. A good design would add an `AudioService` that maps game events (collision, pickup, win, loss,
   button press, engine activation) to sound resources defined in data, avoiding hard-coded audio calls
   throughout gameplay classes.

 * More general main-character support: Some game variations, like Mario, work better as direct
   character-control games than vehicle-building games. Our data-driven system can fake this with invisible
   transport components, but a cleaner design would add a general `ControllableActor` or
   `MainCharacterComponent` abstraction so each game can choose between vehicle assembly and direct actor
   control.

 * Condition-based level progression (Level Manager): A `LevelManager` interface with `all()`,
   `nextAfter(LevelId, LevelManagerContext)`, and `isUnlocked(LevelId, LevelManagerContext)` would replace
   the current linear `LevelProgression`. `game.json` would gain an optional `transitions[]` array where
   each entry holds a from-level, a to-level, and a string condition key. A `LevelConditionRegistry` would
   map condition keys to `@FunctionalInterface` `LevelCondition` implementations evaluated against a
   `LevelManagerContext` (furthest level reached, last score, last elapsed time). All storage and
   win-detection code (`SqliteUserProgressionRepo`, `TerminalResultHandler`, `PlayHistoryRepository`)
   would remain unchanged.

 * High scores screen: A `HighScoresAppScreen extends AbstractAppScreen` injected with
   `PlayHistoryRepository` would display per-player best times and scores. Registration and navigation
   require only the steps described in the "New screens" feature above.
