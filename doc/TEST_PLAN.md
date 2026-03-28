# Test Strategies

## Strategy 1: writing smaller, more independent classes
We plan to adhere to the Single Responsibility Principle and design classes that are small and atomic in their functionality/responsibility. This could make the APIs more testable by isolating behaviors without managing complex dependencies. It will allow us to write focused unit tests with simple input and output.

## Strategy 2: adding exceptions
We plan to add specific, custom exceptions to improve our API's testability by making error handling predictable. Instead of returning ambiguous values for missing data or errors in computation, the class should throw an exception that clearly flags invalid input or when a bad state is reached. This also allows us to easily write negative tests and check if the correct exception is thrown under the right circumstances.


# API Test Scenarios
## Game Player
### Level file verification for missing level file (sad)
#### Expected Outcome: 
The system should reject the load request and throw an error indicating the file could not be found or is invalid. The game state should not change, and the player should remain on the current screen with an error message displayed.
#### Support from Design: 
The LevelPersistence API declares the method loadLevel(String filepath) throws LevelLoadException. To test this, you can call loadLevel("nonexistent_level.json") and write an assertion to ensure that a LevelLoadException is thrown. You can also check that the WorldManager.getAllObjects() list remains empty.

### Invalid part attachment during car construction (sad) 
#### Expected Outcome: 
The system should prevent the vehicle component from being attached to the grid. The part should snap back to the inventory pool, and an error animation/tooltip should notify the user. 
#### Support from Design: 
The `VehicleWorkspace` API has the method `attachPart(String partId, GridCoordinate coord) throws InvalidAssemblyException`. In a test, you can pass overlapping `GridCoordinate` objects or coordinates outside the canvas bounds. The test will verify that an `InvalidAssemblyException` is thrown and that calling `isValidAssembly()` returns `false`. 

### Valid car assembly and starting the level (happy) 
#### Expected Outcome: 
The player's completed vehicle blueprint is validated successfully. The system removes the build UI, sets up the physical connections between adjacent vehicle components, and starts the physics simulation. 
#### Support from Design: 
After successfully calling `attachPart()` for a pig and a frame, calling `VehicleWorkspace.isValidAssembly()` should return `true`. You can test the transition by triggering `GameLoop.startGameSimulation()`, which should systematically invoke `WorldManager.createWeld(GameObject a, GameObject b, double breakForce)` for connected parts. You can then call `WorldManager.getActiveJoints()` to verify the correct number of joints were generated. 

### Applying gameplay input to a vehicle component (happy) 
#### Expected Outcome: 
When a player presses a bound key (e.g., SPACE to activate an engine/fan), the component turns on and applies a directional force to the vehicle. Releasing the key stops the force. 
#### Support from Design: 
The `InputRouter` API uses the Command pattern via `bindKeyToAction()`. You can test this by calling `receiveKeyPress("SPACE")` to trigger a `MotorAction`. Then, simulate a frame by calling `GameEngine.stepSimulation(deltaTime)`. You can verify success by checking `PhysicsBody.getVelocity()` on the vehicle to ensure the vector has changed compared to its resting state. Calling `receiveKeyRelease("SPACE")` should halt further acceleration. 

### High-speed collision resulting in vehicle damage (happy) 
#### Expected Outcome: 
If the vehicle crashes into terrain with an impulse that exceeds its components' damage threshold, the affected blocks should lose health or break entirely, snapping off from the main vehicle. 
#### Support from Design: 
The `CollisionDetector.detectAndResolve()` method calculates the `impactForce`. By setting a high initial velocity on the vehicle and calling `stepSimulation()` to force an overlap with a `TerrainComponent`, the `VehicleCollisionStrategy.resolveCollision()` will fire. You can test if the impact force exceeded the `breakForce` threshold by checking if `WorldManager.severAllConnections(target)` was called, and asserting that the broken component's `isActive()` returns `false`. 

### Process Win Condition at the Finish Line (happy) 
#### Expected Outcome: 
When the pig object intersects with the finish line object and its health is greater than 0, the game registers a win. The physics simulation stops, and the UI transitions to the victory/score screen. 
#### Support from Design: 
You can simulate this by manually setting the pig's `Vector` position to overlap the `FinishLineComponent` and calling `CollisionDetector.detectAndResolve()`. This should trigger `FinishLineCollisionStrategy.resolveCollision()`. You can verify the outcome by checking that the `GameLoop` state changes out of play mode and that a specific event (like updating the player's profile score) is successfully passed to the `Data Persistence` backend.

### Pausing and Resuming the Game Simulation (happy)
#### Expected Outcome: 
When the player triggers the pause action, all physics calculations, timer countdowns, and animations halt immediately. The vehicle freezes in place. Unpausing restores the exact velocity, position, and momentum without any physics "glitches" or sudden jumps.
#### Support from Design: 
The `GameLoop` API should contain `pause()` and `resume()` methods. To test this, you can let the simulation run for a few frames, record the `PhysicsBody.getVelocity()` and `Position` of the pig, call `GameLoop.pause()`, and simulate 10 more frames. Assert that the velocity and position remain identical to the recorded values. Then call `resume()` and assert that the values begin changing again on the next frame update.

### Vehicle falls completely out of world bounds (negative)
#### Expected Outcome: 
If the player's vehicle drives or falls off the map (exceeding the defined bottom/side boundaries of the level), the game should automatically register a loss. The simulation should stop, and the "Level Failed" screen should appear.
#### Support from Design: 
The `WorldManager` or `LevelState` likely has a `checkBounds(GameObject obj)` method evaluated during the game loop. In a test, manually set the pig's `Vector` position to a deeply negative Y-coordinate (e.g., `-9999`) and call `GameEngine.stepSimulation()`. Assert that a `LossConditionMetException` is thrown or that `GameLoop.getCurrentState()` transitions to `GAME_OVER_LOSS`.

### Pig health reaches zero from severe collisions (happy)
#### Expected Outcome: 
If the pig takes lethal damage (e.g., from an explosion or a series of high-speed crashes before reaching the finish line), the pig sprite changes to a "popped" or defeated state, the run immediately fails, and the loss screen is presented.
#### Support from Design: 
You can test this without needing actual physics by directly calling `PigComponent.takeDamage(100)` (assuming 100 exceeds max health). Assert that `PigComponent.getHealth()` returns `0`, `PigComponent.isActive()` returns `false`, and the next call to `GameLoop.checkEndConditions()` returns a loss state, preventing any further player input via `InputRouter`.

### Dynamic camera tracking at high speeds (happy)
#### Expected Outcome: 
As the vehicle moves through the level, the camera's viewport automatically pans to keep the pig relatively centered. The camera should stop panning if it hits the hard edges of the level boundaries, preventing the player from seeing "out of bounds" empty space.
#### Support from Design: 
The `Camera` API should have an `update(Target target)` method that follows the pig. Test this by moving the pig's position 1000 pixels to the right. Call `Camera.update()`. Assert that `Camera.getViewportX()` has shifted to the right accordingly. Then, move the pig to an extreme coordinate like `100000` (beyond the level width). Assert that `Camera.getViewportX()` is capped exactly at `LevelState.getMaxCameraBoundsX()`.


## Authoring Environment
### Placing a valid object on the level canvas (happy)
#### Expected Outcome: 
The object (e.g., terrain, hazard, or enemy) is successfully added to the level model, rendered on the canvas at the specified coordinates, and becomes selectable for further editing.
#### Support from Design: 
The `LevelEditor` API likely has a method like `placeObject(GameObject obj, Position pos)`. To test this, you can call this method with valid coordinates within the canvas bounds. You can verify the outcome by asserting that `LevelManager.getAllObjects()` contains the newly added object and that its `Position` matches the provided coordinates.

### Placing an object out of canvas bounds (sad)
#### Expected Outcome: 
The system rejects the placement because it falls outside the playable or authoring area. The object is not added to the canvas, and a visual warning/tooltip is displayed to the user.
#### Support from Design: 
Test this by calling `LevelEditor.placeObject(GameObject obj, Position pos)` with a `Position` that exceeds `LevelBounds.getMaxX()` or `getMaxY()`. Assert that an `OutOfBoundsException` is thrown (or the method returns `false`), and verify that `LevelManager.getAllObjects().size()` remains unchanged.

### Modifying an object's physical properties (happy)
#### Expected Outcome: 
When the user changes an attribute in the property inspector (e.g., changing a block's mass or friction), the selected object's data model is updated immediately and reflects the new values.
#### Support from Design: 
The `ObjectInspector` or `PropertyManager` API should have a method like `setProperty(String objectId, String propertyName, Object value)`. You can test this by calling `setProperty("block_1", "mass", 50.0)`. Verify success by calling `LevelManager.getObject("block_1").getProperty("mass")` and asserting it equals `50.0`.

### Setting an invalid property value (sad)
#### Expected Outcome: 
The system prevents an invalid value (e.g., negative mass, or a string in a numeric field) from being applied. The input field reverts to the last valid value, and an error indicator appears.
#### Support from Design: 
Test this by calling `ObjectInspector.setProperty("block_1", "mass", -10.0)`. The API should throw an `InvalidPropertyValueException`. You can catch this exception in the test and assert that `GameObject.getProperty("mass")` still retains its previous valid value, ensuring the bad data did not pollute the model.

### Removing an object from the canvas (happy)
#### Expected Outcome: 
When a user selects an object and hits the delete key (or delete button), the object is completely removed from the level model, its properties disappear from the inspector, and it stops rendering on the canvas.
#### Support from Design: 
Call `LevelEditor.removeObject(String objectId)`. To verify, assert that `LevelManager.getObjectById(objectId)` returns `null`, and that the total size of the objects list returned by `LevelManager.getAllObjects()` has decreased by one.

### Saving a complete and valid level (happy)
#### Expected Outcome: 
The current state of the authoring canvas is successfully serialized into a JSON/XML file and stored in the designated file system directory. A "Save Successful" notification is shown.
#### Support from Design: 
Call `LevelSerializer.saveLevel(LevelModel currentLevel, String filename)`. You can verify this by checking if the method returns `true`, and then utilizing `DataPersistence.fileExists(filename)` to assert that the file was actually written to the local disk.

### Saving a level with missing mandatory elements (sad)
#### Expected Outcome: 
The save action is blocked because the level is considered "unplayable" (e.g., missing a player spawn point or a finish line). An error dialog lists the specific missing requirements.
#### Support from Design: 
Before saving, the API likely calls `LevelValidator.validate(LevelModel level)`. Test this by creating a `LevelModel` without a finish line component and attempting to save. Assert that an `InvalidLevelException` is thrown and that `DataPersistence.fileExists(filename)` returns `false` for the new save attempt.

### Loading an existing level into the editor (happy)
#### Expected Outcome: 
A previously saved level file is successfully parsed. The editor canvas is cleared and then repopulated with all saved objects, terrain, and background settings exactly as they were authored.
#### Support from Design: 
Call `LevelSerializer.loadLevel(String filepath)`. You can verify this by checking that `LevelManager.isLevelLoaded()` returns `true`, and by asserting that `LevelManager.getAllObjects().size()` matches the exact number of objects defined in the test JSON file.

### Loading a corrupted or incompatible level file (sad)
#### Expected Outcome: 
The editor fails to load the file due to malformed data or an outdated schema. The current canvas remains completely unaffected (or clears safely), and an error message informs the user of file corruption.
#### Support from Design: 
Test this by providing a path to a file containing invalid JSON or missing mandatory schema keys to `LevelSerializer.loadLevel("corrupted_level.json")`. Assert that a `FileParsingException` is thrown, and verify that the `LevelModel` remains in its default/empty state.

### Undoing the last authoring action (happy)
#### Expected Outcome: 
When the user triggers the undo command, the most recent action (like placing, moving, or deleting an object) is completely reverted. The canvas and data model reflect the state exactly one step prior.
#### Support from Design: 
Assuming the editor uses the Command pattern, test this by performing an action (e.g., `ActionManager.execute(new PlaceObjectCommand(...))`), and then calling `ActionManager.undo()`. If the action was placing an object, assert that `LevelManager.getAllObjects()` no longer contains that specific object, reverting to its prior state.

### Redoing a previously undone authoring action (happy)
#### Expected Outcome: 
When the user triggers the redo command immediately after an undo, the exact action that was undone is reapplied to the level. The data model and visual canvas update to reflect this restored state.
#### Support from Design: 
Following an `ActionManager.undo()` call, test this by invoking `ActionManager.redo()`. If the original action was placing an object, assert that `LevelManager.getObjectById(objectId)` once again returns a valid object, confirming it was successfully restored to the active level model.

### Moving an existing object to a valid new coordinate (happy)
#### Expected Outcome: 
When a user clicks and drags an object to a new valid location, its spatial coordinates are updated in the level data model without altering any of its other properties or creating a duplicate.
#### Support from Design: 
Test this by calling `LevelEditor.moveObject(String objectId, Position newPos)` using coordinates within the canvas bounds. Retrieve the object via `LevelManager.getObjectById(objectId)` and assert that `GameObject.getPosition()` exactly matches `newPos`, while ensuring `LevelManager.getAllObjects().size()` remains exactly the same.

### Moving an existing object out of bounds or to an invalid space (sad)
#### Expected Outcome: 
If the user attempts to move an object outside the playable canvas or into a restricted area, the system rejects the movement. The object instantly snaps back to its last valid coordinates, and an error sound or visual flash occurs.
#### Support from Design: 
Call `LevelEditor.moveObject(String objectId, Position invalidPos)` where `invalidPos` has negative coordinates or exceeds the level's maximum dimensions. The API should throw an `InvalidPlacementException`. Catch this exception in the test and assert that `GameObject.getPosition()` still returns the original coordinates prior to the move attempt.

### Clearing the entire level canvas (happy)
#### Expected Outcome: 
When the user selects the "New/Clear Level" option, the current active level is completely wiped. All placed objects, custom terrain, and modified global settings are removed, leaving a blank default canvas.
#### Support from Design: 
Populate the level with several objects, then call `LevelEditor.clearLevel()`. You can verify the outcome by asserting that `LevelManager.getAllObjects().isEmpty()` returns `true`, and checking that global variables like `LevelManager.getLevelName()` have reverted to their default strings (e.g., "Untitled Level").

## Login/User Auth
### Entering valid credentials for an existing account (happy)
#### Expected Outcome: 
When the user enters the correct username and password combination, the system successfully authenticates them. The UI transitions from the login screen to the main menu, and the user's saved profile data (like high scores and unlocked levels) is loaded into memory.
#### Support from Design: 
The `AuthenticationManager` (or similar auth API) should have a `login(String username, String password)` method. To test this, you can pass valid mock credentials. You can verify the outcome by asserting that `AuthenticationManager.getCurrentUser()` is not null, matches the expected user ID, and that `GameLoop.getCurrentState()` transitions to the `MAIN_MENU` state.

### Entering an incorrect password for an existing account (sad)
#### Expected Outcome: 
The system rejects the login attempt. The user remains on the login screen, and a visual error message displays stating "Invalid username or password." No profile data is loaded.
#### Support from Design: 
Test this by calling `AuthenticationManager.login(String validUser, String wrongPassword)`. The API should throw an `InvalidCredentialsException` (or return `false`). You can catch this exception in your test and assert that `AuthenticationManager.getCurrentUser()` remains `null` and the game state remains on `LOGIN_SCREEN`.

### Attempting to log in with an unregistered username (sad)
#### Expected Outcome: 
The system checks the database/file system and cannot find a matching username. The login is rejected, and the UI displays a "User does not exist" or similar error message, prompting them to register.
#### Support from Design: 
Call `AuthenticationManager.login(String nonexistentUser, String password)`. The method should throw a `UserNotFoundException`. Assert that the exception is thrown, no user session is instantiated, and `DataPersistence.loadUserProfile(nonexistentUser)` is never successfully completed.

### Registering a brand new user account (happy)
#### Expected Outcome: 
When the user inputs a unique, unused username and a valid password, the system creates a new user profile. The new credentials are saved to the backend/file system, and the user is either logged in automatically or redirected to the login screen with a success message.
#### Support from Design: 
The API likely contains `AuthenticationManager.register(String newUsername, String newPassword)`. Test this by calling the method with a unique string. You can verify success by asserting the method returns `true`, and then calling `DataPersistence.userExists(newUsername)` to ensure the new profile was physically written to the storage system.

### Attempting to register with an already taken username (sad)
#### Expected Outcome: 
The system detects that the requested username already exists in the database. The registration process is aborted, and the UI shows an error indicating "Username is already taken, please choose another."
#### Support from Design: 
Test this by calling `AuthenticationManager.register(String existingUsername, String password)` using a username known to already be in the mock database. Assert that a `UsernameTakenException` is thrown, and verify that the original user profile associated with that name remains completely unmodified.

### Logging out of an active session (happy)
#### Expected Outcome: 
When a logged-in user clicks the "Log Out" button, their active session is terminated. Any unsaved temporary profile changes are discarded or saved (depending on design), the current user data is cleared from memory, and the application returns to the login screen.
#### Support from Design: 
While a user is actively mocked as logged in, call `AuthenticationManager.logout()`. To verify the outcome, assert that `AuthenticationManager.getCurrentUser()` returns `null`, ensure that `UserProfileManager.getActiveProfile()` is cleared, and check that the UI state manager transitions back to the `LOGIN_SCREEN`.


## Settings
### Changing the application theme to Dark Mode (happy)
#### Expected Outcome: 
When the user selects "Dark Mode" from the theme dropdown, the application's UI immediately updates its color palette (backgrounds, text, and button colors) to the dark theme specifications. The preference is saved to the user's local settings profile.
#### Support from Design: 
The `SettingsManager` API should have a method like `setTheme(Theme.DARK)`. Test this by calling the method and verifying the outcome by asserting that `SettingsManager.getCurrentTheme()` returns `Theme.DARK`. You can also verify that a specific UI component's background color property has dynamically changed to match the dark theme constant.

### Setting text size to an invalid or unreadable value (sad)
#### Expected Outcome: 
If a user (or a malformed config file) attempts to set the UI text scale to an extreme value (e.g., negative, zero, or excessively large), the system rejects the change. The text size reverts to the default or last valid size, preventing the UI from becoming unusable.
#### Support from Design: 
Test this by calling `SettingsManager.setTextScale(-1.5)` or `SettingsManager.setTextScale(999.0)`. The API should throw an `InvalidSettingValueException` or automatically clamp the value to acceptable bounds. Assert that `SettingsManager.getTextScale()` remains at its previous valid float value (e.g., `1.0`).

### Changing the display language to Spanish (happy)
#### Expected Outcome: 
When the user switches the language setting to Spanish, all static text in the UI (menus, buttons, tooltips) immediately re-renders using the Spanish localization file. The chosen language is saved for future sessions.
#### Support from Design: 
Call `SettingsManager.setLanguage(Locale.SPANISH)`. Verify the outcome by asserting that `LocalizationManager.getCurrentLocale()` matches the new setting. Then, query the UI via `UIManager.getLocalizedText("button_start")` and assert that it returns the expected Spanish string (e.g., "Iniciar") instead of "Start".


