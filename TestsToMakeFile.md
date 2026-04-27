# Tests To Make

This file lists production classes that do not currently have a direct same-package
`*Test` class, plus the tests that would be most useful to add. A direct test means
`src/main/java/oogasalad/foo/Bar.java` has `src/test/java/oogasalad/foo/BarTest.java`.

## Highest Priority Tests

These are the most relevant gaps for the authored data flow work: exact catalog ids
should move through authoring, play entry, vehicle builder, gameplay, and resources
without translation.

| Production Class | Proposed Test Class | Tests To Write |
| --- | --- | --- |
| `oogasalad.data.internal.LevelPaths` | `LevelPathsTest` | `gameFileForLevel` returns `data/games/<gameType>/game.json` when it exists; blank or null `gameType` falls back to sibling `game.json`; missing central catalog falls back to sibling `game.json`; level paths with no parent still resolve safely. |
| `oogasalad.data.internal.GameLoaderImpl` | `GameLoaderImplTest` | `loadGame` loads a `GameConfigImpl` from a given path; invalid/missing files throw the expected load exception; deprecated `parseFromFile` still delegates consistently if it remains public. |
| `oogasalad.data.internal.LevelLoaderImpl` | Extend `LevelLoaderImplTest` | `loadAttributes` reads `gameMetadata.gameType`; `loadAttributes` reads `mainCharacter.elementsAllowed`, `maxSelectable`, and HP values exactly; missing optional metadata keeps safe defaults. |
| `oogasalad.data.internal.LevelSaverImpl` | Extend `LevelSaverImplTest` | Saved `elementsAllowed` preserves exact ids and order; saved static object types preserve exact catalog ids; `GoalLine` is saved as `GoalLine`, not `finish_line`; `AuthoringStaticComponent` saves its type and scaled position correctly. |
| `oogasalad.view.player.internal.PlayerEntryAppScreen` | Extend `PlayerEntryAppScreenTest` | `onEnter` refreshes the scan result; newly added worlds/levels appear without app restart; selected authored worlds use the same keys returned by `LevelScanner`. |
| `oogasalad.view.player.internal.VehicleBuilderAppScreen` | Extend `VehicleBuilderAppScreenTest` | `setAllowedComponentIds` filters the palette by exact ids; disallowed ids do not render; allowed ids like `Frame`, `Wood Wheel`, and `Engine` match registry/resource names exactly. |
| `oogasalad.controller.core.internal.ScreenFactory` | `ScreenFactoryTest` | Play entry gets fresh scanned level data; builder receives `LevelLoader.loadAttributes` allowed components; gameplay resolves the game catalog from level metadata instead of sibling level folders. |
| `oogasalad.view.authoring.internal.AuthoringEntryScreenImpl` | `AuthoringEntryScreenImplTest` | Selecting an existing world/level notifies catalog selection before opening authoring; creating a new level under a world uses that world's game config; entry refreshes scanned levels on each entry. |
| `oogasalad.view.authoring.internal.AuthoringScreenImpl` | `AuthoringScreenImplTest` | Vehicle, terrain, and static palettes are populated from the selected `GameConfig`; switching selected game config refreshes palettes; exact catalog ids are used for created components. |
| `oogasalad.view.authoring.internal.ComponentPaletteImpl` | `ComponentPaletteImplTest` | Static catalog items create `AuthoringStaticComponent`; palette item ids match `StaticComponentInfo` ids exactly; drag/drop payloads preserve exact names. |
| `oogasalad.model.engine.internal.components.AuthoringStaticComponent` | `AuthoringStaticComponentTest` | Constructor stores exact type id; `createAt` preserves type and geometry; render/collision dimensions match the static catalog item. |
| `oogasalad.model.engine.internal.effects.ScoreEffect` | `ScoreEffectTest` | Applying the effect adds the configured score amount; invalid negative values are handled as intended; null score services or unsupported targets fail clearly. |
| `oogasalad.view.accounts.internal.LoginScreen` | Extend `LoginScreenTest` | The Skip Login button fires `Outcome.SKIP_LOGIN`; skip login does not require username/password; normal login/create/back flows still work. |

## Other Useful Concrete Class Tests

### Controller: Authoring

| Production Class | Proposed Test Class | Tests To Write |
| --- | --- | --- |
| `AuthoringPersistenceService` | `AuthoringPersistenceServiceTest` | Save delegates to `LevelSaver`; load delegates to `LevelLoader`; failures surface through the expected exception/alert path. |
| `CanvasAction` | `CanvasActionTest` | Record fields preserve action type and payload; undo/redo descriptions are stable if exposed. |
| `HoverPreviewInputHandler` | `HoverPreviewInputHandlerTest` | Hovering valid cells updates preview; leaving canvas clears preview; invalid placement uses the rejected preview style. |
| `OccupancyGrid` | `OccupancyGridTest` | Placing cells marks occupancy; clearing cells removes occupancy; overlaps and out-of-bounds placements are rejected. |
| `PaletteDropInputHandler` | `PaletteDropInputHandlerTest` | Dropping a palette item creates the correct authoring component; invalid drops do not mutate the canvas; static item ids are preserved. |
| `SpawnDragInputHandler` | `SpawnDragInputHandlerTest` | Dragging spawn updates the spawn position; invalid drag positions are rejected; releasing commits one history action. |
| `SpawnPointService` | `SpawnPointServiceTest` | Only one spawn point exists; moving spawn updates the existing object; missing spawn is created in the expected default position. |
| `SplineInputHandler` | `SplineInputHandlerTest` | Clicks add spline knots; drag updates control points; cancel clears the draft. |
| `ToolSelectorImpl` | `ToolSelectorImplTest` | Default tool is correct; selecting a tool notifies listeners; repeated selection does not emit duplicate state if that is the intended behavior. |

### Controller: Player

| Production Class | Proposed Test Class | Tests To Write |
| --- | --- | --- |
| `GameControllerImpl` | `GameControllerImplTest` | Keyboard input routes to actions; pause/restart delegates to the session; terminal win/loss results route to the result handler. |
| `GamePlayScreenCoordinator` | `GamePlayScreenCoordinatorTest` | Entering the screen starts gameplay; exiting stops gameplay; restart rebuilds from the current level metadata. |
| `GameplaySession` | `GameplaySessionTest` | Start/stop lifecycle is idempotent; terminal results are only reported once; tick/update delegates to engine and view. |
| `VehicleFactory` | `VehicleFactoryTest` | Vehicle layouts create expected parts; joints connect adjacent parts correctly; exact part ids are looked up through `VehiclePartRegistry`. |
| `DrivePartSpec` | `DrivePartSpecTest` | Creates driveable components with expected dimensions/sprite; control buttons map to drive behavior. |
| `PigPartSpec` | `PigPartSpecTest` | Creates the pig/main character part; hp and dimensions are correct. |
| `StructuralPartSpec` | `StructuralPartSpecTest` | Creates structural components with expected dimensions; non-drive parts are not driveable. |
| `VehiclePartRegistry` | `VehiclePartRegistryTest` | All expected ids are registered exactly; lookup by exact id succeeds; unknown ids fail clearly. |

### Data

| Production Class | Proposed Test Class | Tests To Write |
| --- | --- | --- |
| `ButtonInfo` | `ButtonInfoTest` or JSON round-trip tests | Constructor fields preserve name and direction; JSON loading keeps exact names. |
| `CollisionAction` | `CollisionActionTest` or loader tests | Type and amount fields preserve values; `score` action maps to `ScoreEffect` through loader tests. |
| `ComponentInfo` | `ComponentInfoTest` or game config tests | Id/name/sprite/dimensions load exactly from `game.json`; ids with spaces are preserved. |
| `LevelId` | `LevelIdTest` | World and level keys are preserved; equality/hash behavior works for map keys. |
| `StaticComponentInfo` | `StaticComponentInfoTest` or game config tests | Static ids like `Coin`, `Heart`, `Bomb`, `XP Boost`, and `GoalLine` load exactly. |
| `TerrainInfo` | `TerrainInfoTest` or game config tests | Terrain ids and sprite paths load exactly from the catalog. |

### Model: Engine

| Production Class | Proposed Test Class | Tests To Write |
| --- | --- | --- |
| `Collision` | `CollisionTest` | Stores objects/contact data correctly; swapping object order keeps expected semantics. |
| `CollisionDispatcher` | `CollisionDispatcherTest` | Dispatch invokes collision effects; unsupported pairs are ignored; score actions apply through `ScoreEffect`. |
| `CollisionPair` | `CollisionPairTest` | Equality works regardless of intended pair ordering if unordered; hash behavior supports map lookups. |
| `ContactPointPhysics` | `ContactPointPhysicsTest` | Contact normal and penetration calculations are stable; zero overlap is handled. |
| `ConvexPolygon` | `ConvexPolygonTest` | Projection axes are correct; translated/rotated vertices produce expected bounds. |
| `DynamicDynamicSolver` | `DynamicDynamicSolverTest` | Dynamic bodies separate after collision; impulses update velocity correctly. |
| `DynamicStaticSolver` | `DynamicStaticSolverTest` | Dynamic objects resolve against static terrain; pass-through/static collisions respect tags. |
| `StaticState` | `StaticStateTest` | Static state preserves position/rotation; conversion to renderable is correct. |
| `StaticStaticSolver` | `StaticStaticSolverTest` | Static-static collisions do not create motion; overlap results are reported consistently. |
| `GoalLineComponent` | `GoalLineComponentTest` | Component type is `GoalLine`; crossing produces win effect/finish behavior; dimensions and renderable state are correct. |
| `RockComponent` | `RockComponentTest` | Type id is preserved; collision effect is applied; hp/damage behavior is correct if supported. |
| `SpawnPointComponent` | `SpawnPointComponentTest` | Spawn position and type are stable; component is not treated as a normal obstacle. |
| `StructuralComponent` | `StructuralComponentTest` | Static dimensions and tags are correct; collision bounds match sprite dimensions. |
| `TerrainBlock` | `TerrainBlockTest` | Block dimensions and type id are preserved; collision shape matches the block. |
| `HPChangerEffect` | `HPChangerEffectTest` | Damage/heal values change hp as expected; non-damageable targets are ignored or fail as designed. |
| `SpringJoint` | `SpringJointTest` | Rest length and spring force are applied; damping prevents energy blow-up; endpoints follow attached bodies. |

### Model: Help

| Production Class | Proposed Test Class | Tests To Write |
| --- | --- | --- |
| `ChatMessage` | `ChatMessageTest` | Role and content fields are preserved; empty/null content behavior is documented. |
| `HelpChatModel` | `HelpChatModelTest` | Adding user/assistant messages updates history; clearing history works; error messages are represented consistently. |
| `HelpLLMService` | `HelpLLMServiceTest` | Builds expected request payload; handles missing API key; handles error responses without crashing the UI. |

### Model: Player

| Production Class | Proposed Test Class | Tests To Write |
| --- | --- | --- |
| `BuildRules` | `BuildRulesTest` | Position constraints, max counts, and allowed sides are preserved. |
| `DefaultBuildRulesRegistry` | `DefaultBuildRulesRegistryTest` | Known component types return expected rules; unknown types return default or failure behavior. |
| `FinishLineWinChecker` | `FinishLineWinCheckerTest` | Pig crossing goal line wins; other components crossing do not win; no false positive before crossing. |
| `GameplayScore` | `GameplayScoreTest` | Score fields preserve values; equality works for history snapshots. |
| `PlacedComponent` | `PlacedComponentTest` | Component id, grid position, and rotation are preserved; equality works for layouts. |
| `PlacementValidator` | `PlacementValidatorTest` in `model/player/internal` | Bottom-row constraints are enforced; inside-frame constraints are enforced; open-side/attachment rules respect rotation. |
| `PlayHistoryEntry` | `PlayHistoryEntryTest` | Level id, score, timestamp, and result fields are preserved. |
| `ScoreServiceImpl` | `ScoreServiceImplTest` | Points accumulate; reset clears score; listeners/snapshots update if supported. |
| `UserProgression` | `UserProgressionTest` | Completed/unlocked level data is immutable and comparable; empty progression is valid. |
| `ValidationResult` | `ValidationResultTest` | Valid/invalid factories or fields carry the expected reason. |
| `VehicleLayout` | `VehicleLayoutTest` | Placements are preserved in order; layout is immutable if intended. |

### View: Accounts

| Production Class | Proposed Test Class | Tests To Write |
| --- | --- | --- |
| `ProfileScreen` | `ProfileScreenTest` | Displays username/avatar; logout/back buttons fire expected outcomes; missing profile data is handled. |
| `UpdateProfileScreen` | `UpdateProfileScreenTest` | Saving profile validates fields; cancel returns without update; avatar path changes are displayed. |

### View: Authoring

| Production Class | Proposed Test Class | Tests To Write |
| --- | --- | --- |
| `AuthoringAlertService` | `AuthoringAlertServiceTest` | Error/info alerts show expected text; exceptions are converted to user-facing messages. |
| `AuthoringCanvasImpl` | `AuthoringCanvasImplTest` | Adding/removing placeables updates rendered nodes; coordinate conversion is correct; selection state updates. |
| `FileChooserService` | `FileChooserServiceTest` | Save/open path selection returns expected paths with mocked chooser behavior; cancel returns empty. |
| `GameAuthoringScreenImpl` | `GameAuthoringScreenImplTest` | Catalog editing saves game config; adding component/static/terrain entries updates the model; invalid ids are rejected. |
| `LevelAttributesPanel` | `LevelAttributesPanelTest` | UI fields read/write `LevelAttributes`; `elementsAllowed` preserves exact names. |
| `TerrainPlaceable` | `TerrainPlaceableTest` | Type id and dimensions are preserved; create-at returns expected placement. |
| `TerrainPropertiesPanel` | `TerrainPropertiesPanelTest` | Terrain property edits update selected terrain; invalid values are rejected. |
| `VehicleComponentScrollPalette` | `VehicleComponentScrollPaletteTest` | Shows catalog vehicle components; exact ids with spaces render and drag correctly. |

### View: Core

| Production Class | Proposed Test Class | Tests To Write |
| --- | --- | --- |
| `GameMenuScreen` | `GameMenuScreenTest` | Menu buttons fire the correct outcomes; keyboard shortcuts work if supported. |
| `SingleSprite` | `SingleSpriteTest` | Path, width, and height fields are preserved; invalid dimensions are handled if validated. |

### View: Help

| Production Class | Proposed Test Class | Tests To Write |
| --- | --- | --- |
| `HelpChatHistoryView` | `HelpChatHistoryViewTest` | Messages render in order; user/assistant roles use expected styling; clearing history removes nodes. |
| `HelpChatInputBar` | `HelpChatInputBarTest` | Send button submits text; Enter key submits; blank input is ignored. |
| `HelpChatWindow` | `HelpChatWindowTest` | Opening/closing works; submitted messages call coordinator; errors display in the chat. |
| `PopupDragSupport` | `PopupDragSupportTest` | Dragging changes popup position; drag offset is preserved; null/invalid events are ignored. |

### View: Player

| Production Class | Proposed Test Class | Tests To Write |
| --- | --- | --- |
| `GameHudDisplay` | `GameHudDisplayTest` | HP/score/time labels update; missing stats display defaults. |
| `VehicleHpHud` | `VehicleHpHudTest` | HP bar reflects current and max hp; zero hp displays empty; over-max hp clamps if intended. |
| `ResultStatsPanel` | `ResultStatsPanelTest` | Score/time/result labels display correctly; restart/menu buttons expose expected actions if present. |

## Lower Priority Or Usually Covered Indirectly

These types usually do not need a dedicated unit test unless they gain logic. Cover them
through concrete implementations, integration tests, or serialization tests.

### Interfaces

`AuthController`, `AuthoringController`, `ToolSelector`, `ScreenManager`, `Action`,
`GameController`, `GameplaySessionFactory`, `InputRouter`, `GameConfig`, `GameLoader`,
`LevelLoader`, `LevelSaver`, `Settings`, `AuthService`, `AuthResult`, `PlayerProfile`,
`SessionContext`, `VehicleBuildRulesManager`, `CollisionDetector`, `CollisionEffect`,
`CollisionSolver`, `Damageable`, `Driveable`, `GameEngine`, `GameObject`,
`HasCollisionEffect`, `Interactive`, `Joint`, `TypedComponent`, `World`,
`BuildRulesRegistry`, `GameplayStatsTracker`, `LossConditionChecker`,
`PlayHistoryRepository`, `ScoreCalculator`, `ScoreService`, `UserProgressionRepository`,
`VehicleWorkspace`, `WinConditionChecker`, `AccountOutcome`, `AuthoringCanvas`,
`AuthoringPlaceable`, `ComponentPallete`, `AppScreen`, `Camera`, `CameraBehavior`,
`Renderable`, `Renderer`, `Sprite`, `SpriteRegistry`, `GameplayView`,
`VehiclePartSpec`, `CanvasInputHandler`, `UserRepository`.

### Exceptions

`AuthoringPlacementException`, `LevelLoadException`, `LevelLockedException`,
`LevelSaveException`, `HelpChatConfigurationException`, `HelpChatRequestException`,
`VehicleSelectionLimitException`.

### Enums

`Outcome`, `EngineError`, `JointStyle`, `DefaultTag`, `Containment`,
`PositionConstraint`, `Side`.

### Abstract Base Classes

`AbstractAppScreen`, `AbstractCameraBehavior`, `FinishLineComponent`, `GameComponent`,
`PassThroughComponent`, `RigidBody`, `StaticComponent`, `TerrainComponent`,
`AbstractJoint`, `AbstractVehiclePartSpec`.

### Package Info Files

`controller/authoring/api/package-info.java`, `controller/core/api/package-info.java`,
`controller/player/api/package-info.java`, `data/api/package-info.java`,
`model/authoring/api/package-info.java`, `model/engine/api/package-info.java`,
`model/player/api/package-info.java`, `view/authoring/api/package-info.java`,
`view/core/api/package-info.java`.

## Classes That Already Have Direct Tests

These already have same-package `*Test` classes and should usually be extended rather
than duplicated when adding new scenarios.

`Main`, `AuthControllerImpl`, `AuthoringControllerImpl`, `CanvasActionHistory`,
`SplineDraftController`, `ScreenManagerImpl`, `DriveAction`,
`GameplaySessionFactoryImpl`, `InputRouterImpl`, `PauseAction`, `RestartAction`,
`TerminalResultHandler`, `LevelAttributes`, `GameCatalogSaver`, `GameConfigImpl`,
`LevelLoaderImpl`, `LevelProgression`, `LevelSaverImpl`, `LevelScanner`,
`LevelValidator`, `LocalAuthService`, `PasswordHasher`, `CollisionDetectorImpl`,
`DynamicPassthroughSolver`, `DynamicState`, `GameEngineImpl`,
`ImpulseSolverUtilities`, `WorldImpl`, `Vector`, `DynamicComponent`,
`PickupComponent`, `PigComponent`, `TerrainSpline`, `VehicleComponent`,
`DistanceJoint`, `RigidJoint`, `ParabolaSpline`, `SplineTessellator`,
`FallOffWorldLossChecker`, `GameplayStatsTrackerImpl`, `ScoreCalculatorImpl`,
`SqlitePlayHistoryRepo`, `SqliteUserProgressionRepo`, `CreateAccountScreen`,
`LoginScreen`, `LayeredSprite`, `CameraImpl`, `FollowPlayerCameraBehavior`,
`PaletteSpriteRegistry`, `RenderableImpl`, `RendererImpl`, `SpriteCache`,
`GamePlayAppScreen`, `LoseAppScreen`, `PlayerEntryAppScreen`,
`VehicleBuilderAppScreen`, `WinAppScreen`.

## Notes

- `RevoluteJoint` and `RopeJoint` have tests in `oogasalad/model/engine`, not in
  their implementation package `oogasalad/model/engine/internal/joints`.
- There appears to be a possible stale or cross-package `PlacementValidatorTest`.
  The production class is `oogasalad.model.player.internal.PlacementValidator`, so
  future tests should live in that package unless the authoring package owns a
  different validator.
- For simple records like `ButtonInfo`, `ComponentInfo`, `StaticComponentInfo`, and
  `TerrainInfo`, JSON round-trip tests through `GameConfigImpl`, `GameLoaderImpl`,
  `LevelLoaderImpl`, or `LevelSaverImpl` may be more valuable than one test file per
  record.
