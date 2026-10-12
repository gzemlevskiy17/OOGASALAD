# oogasalad - Final Project for Duke CompSci 308 - Advanced Software Design & Implementation
## Good Piggies
## Georgiy Zemlevskiy, Michael Li, Paulina Vvedenskaya, Iurii Beliaev, Mason Wu, Xuye Chen, Emilio Garcia


This project implements an authoring environment and player for multiple related games.

Players progress through authored levels: assemble vehicles from a per-game catalog, drive them through terrain with **2D rigid-body physics** (collisions, joints, pickups), and win or lose against **data-driven** goals (e.g. crossing a finish line, falling off the world). Designers edit **game catalogs** (`game.json`: terrain types, static objects, movable parts, `mainCharacterId`, win/loss rules, input mapping, scoring) and **levels** (`level.json`: layout, splines, spawn, physics tweaks). Assets live under `data/assets/`; schemas under `data/game.schema.json` and `data/level.schema.json` document the JSON shapes.

### Architecture (overview)

The design keeps **simulation machinery stable** (physics integration, collision detection, world state, rendering from read-only data) while **gameplay stays extensible**: new component types, actions, collision behaviors, camera modes, win/loss checkers, and scoring rules are added through interfaces and configuration, not by forking the engine. **Game-type behavior** (build mode, camera mode, controls, win/loss, scoring, modifiers) comes from the parsed **`game.json`** (`GameConfig` in code — the course design doc sometimes calls this layer `game_config.json`; on disk this project uses `game.json`). **Authoring and runtime share the same JSON** (`level.json` and catalog files), so edited levels load the same way in play. Packages follow **`api/` (interfaces, records) vs `internal/` (implementations)** so features depend on contracts, not concrete classes. For subsystem tables, screen flow, and the module diagram.

### Timeline

 * Start Date: March 22nd

 * Finish Date: April 29th

 * Hours Spent: about ~500 team hours



### Attributions

 * Resources used for learning (including AI assistance): 
    * JavaFX API docs and examples
    * [Dyn4j](https://github.com/dyn4j/dyn4j) (2D Java physics engine)

 * Resources used directly (including AI assistance): 
    * OpenAI Codex for original joint implementations
    * Cursor IDE for refactoring and documentations




### Running the Program

 * Main class: `oogasalad.Main` (`src/main/java/oogasalad/Main.java`). It constructs `ScreenManagerImpl`, registers screens via `ScreenFactory`, and opens the **login** screen by default. Launch from your IDE (recommended for JavaFX module path), or run `mvn compile` and execute `Main` with a JDK that includes **JavaFX** (project targets **Java 25** per `pom.xml`).

 * Data files needed:
    * **`data/games/<gameId>/game.json`** — catalog: terrain ids, static components (pickups, goal line, hazards), vehicle components, buttons/thrust, optional gameplay settings (`win_conditions`, `loss_conditions`, etc.).
    * **`data/levels/<...>/level.json`** — level geometry and placement; references the game type / catalog.
    * **`data/assets/`** — sprites and backgrounds referenced by paths in JSON (e.g. `assets/bp_frame.png`).
    * **SQLite** DB for accounts/progression (created at runtime when using login flows).

 * Interesting data files:

    * **`data/games/_current_game.json`** — helper pointer used by some authoring workflows.

    * **`src/main/resources/oogasalad/view/authoring/AuthoringValueHints.properties`** — suggested value ranges and tooltips for catalog and level authoring fields.

 * Key/Mouse inputs:

    * **Gameplay:** Pause / Reset on the HUD; vehicle tool buttons (engine, fan, balloon, etc.) depend on the loaded `game.json`; optional **Edit level** returns the active level path into authoring.

    * **Authoring / menus:** Standard mouse navigation between screens (login, menu, authoring entry, vehicle builder, gameplay).

    * Exact keybindings for driving live in the loaded game config (`input_mapping` / controller settings) where configured.



### Notes/Assumptions

 * Assumptions or Simplifications:

    * Physics uses a fixed-timestep loop with joint constraints and collision resolution tuned for side-view vehicles.

    * Victory on the finish line uses the **`mainCharacterId`** from `game.json` (must match a movable component **id**); if omitted, legacy behavior may fall back to pig-tagged parts.

    * Levels reference a game catalog path resolved via `LevelPaths` / loader conventions described in code (`LevelLoaderImpl`, `GameLoaderImpl`).

 * Known Bugs:

    * Two pigs can be placed in a single frame slot, which makes vehicle structure unstable.

 * Features implemented:

    * Multi-screen app shell (`ScreenFactory`, `ScreenManager`): login, menu, authoring entry, game catalog editor, level editor canvas (terrain, splines, pigs/items, settings), vehicle builder grid, player level picker, gameplay loop with HUD.

    * Data loading/saving for levels and catalogs (`LevelSaverImpl`, `GameCatalogSaver`, validators).

    * Gameplay session wiring: world load, vehicle spawn from layout, camera follow, win/loss checkers, terminal scoring hooks.

    * Authoring hints (`AuthoringValueHints`) for reasonable numeric ranges on catalog and level fields.

 * Features unimplemented:

    * **Ghost chase** (an enemy pursuing the vehicle for difficulty) was discussed but not implemented due to time. 

 * Noteworthy Features:

    * **Catalog-driven** vehicles and level objects; **preview Run** without committing to a permanent level file path.

    * **Help chat** in authoring (and related tooling) for guidance and optional level-generation workflows.

    * **SQLite-backed** user/session flows where enabled for user account and profile.



### Assignment Impressions

Working in a larger team was more challenging than in a four-person group. Scheduling meetings became harder, and even with a group chat, it was not always clear who was responsible for what at any given time, which sometimes led to duplicated work.

To manage this, we split into smaller sub-teams (engine, gameplay, and authoring). While parallel development improved overall productivity, it also caused branches to diverge in both UI and behavior, making dependencies and merge conflicts more difficult to handle. In this setup, clear and consistent communication became especially important.

