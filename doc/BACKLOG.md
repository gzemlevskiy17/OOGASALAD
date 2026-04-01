### Backlog
### Team Name: Good Piggies
### Georgiy Zemlevskiy, Michael Li, Paulina Vvedenskaya, Iurii Beliaev, Mason Wu, Xuye Chen, Emilio Garcia

# Use Cases:

## Game Player

### Load customized level file
Gameplay env requests a level file
Validate format of the level file
System parses terrain, obstacles, and goals
Systems initializes level state
Initialize car construction.
### Construct Car: (Georgiy)
Gameplay env enters build mode
Displays available blocks & pigs from the library.
Player input selects and places blocks & pigs.
Attachments are validated
If an attachment is invalid
The block should snap back to the pool of available blocks.
Display a tooltip, or an animation that lets the user know that the attachment was invalid.
System assembles the vehicle entity

### Handle Collision/Interactions (Paulina & Georgiy)
Physics engine detects overlap between two objects
System determines what type of collision it is, and what it should do based on the objects’ behavior.
Stationary objects like terrain will not move upon collision with the vehicle
Special blocks trigger additional effects upon collisions (explosives detonate, coins are collected)
Vehicle components that are attached together will stick together.
Another player’s ghost will pass through other vehicle objects but not terrain.
Based on the collision characteristics that the objects have, the collision normal and impulse are calculated.
Velocity and position of affected parts are updated.
If impulse exceeds damage threshold, reduce hitpoints of relevant blocks, and if they lose all of their HP, remove them.
If, in the case of bad piggies, if the pig loses all of its health, the game is ended.

### Handle Gameplay Input (Paulina)
Player presses a key (W / UP / SPACE / etc.) or clicks on screen button
That part activates (engine applies force, rocket fires, balloon inflates)
On key/button release, the part deactivates and force stops
If the part doesn't exist on the vehicle, nothing happens and a tooltip is displayed.

### Process Win/Loss Condition (Mason)
Check the position of the pig and its health
If predefined conditions for winning are satisfied (the pig intersects with the finish line and has health larger than 0), proceed as a win. If not, proceed as a loss.
Stop the simulation
Calculate the final scores
Save to user profile and proceed with unlocking new levels/difficulty
Transition to the result screen

### Start Level after Car Construction (Mason)
Verify that the car construction is valid (has a pig, block, etc)
GUI change to hide the build UI and add gameplay UI
Set the car to the initial parameters specified in the level file
Initialize physics engine

### Restart Level (Mason)
Gameplay requests a reset of the level
Save the car configurations
Reset current gameplay data (health and positions of the car components, velocity, points, etc)
Reload the level file and the gameplay environment.

### Level Selection (Mason)
User clicks start
Verify if the level and difficulty selection and if the level file has valid format
If verification passed, load the level file and transition to gameplay scene. If not, show error message of what is invalid

### Continue to Next Game (Mason)
User clicks continue to next level after completing the level
If the next difficulty level exists, load the next difficulty level. If it does not, return to the level selection scene


## Authoring Environment

### Enter authoring mode
User enters authoring env
System displays instructions, tools, and level list
### Customize Power-ups(Xuye)
User uploads files to customize
System checks all available files to display the options(appearance and rules)
User select the option and  rules
### Customize an obstacle (Xuye)
User Selects Renderer, texture(frictions, gravity), collisions(whether to allow the vehicle to collide into the object), hp(if it would be broken), location
System displays the corresponding obstacle with customized attributions

### Customize the game Goal (Xuye)
System displays a group of variables that can be tracked within a single play
User selects the variable and input thresholds

### Load existing level file (Xuye)
User clicks loads button to load another file
System checks whether current scene has been saved
System asks user to save or desert the current scene
System loads the level file selected

### Run the level file (Xuye)
User clicks Run button after editing the file
System checks whether the file has been saved
System ask user to save the current file
System validates whether the file is ready to run
System shows messages to notify the user what else needs to be implemented
System run the current file

### Goes back to the previous page and desert the current file (Xuye)
User clicks Back button
System checks whether the file has been saved
System ask user whether to save or desert the file
System goes back to the last page

#### Level Editing (Georgiy)
User clicks ‘create’ button and clicks the ‘create new level’ button. They are also presented with a choice to edit an existing level, in which case the associated file bundle for it will be loaded. The attributes and object locations will be parsed and displayed.
The user will be presented with a default canvas area, a panel from which to select terrain objects, and panels to edit the global level attributes, and the attributes of the currently selected object.
The user can select objects, edit their properties, and place them on the canvas.
The user can also draw terrain with a mouse. Spline functionality is available. The program will snap the drawn line to grid blocks in the model.
After the user makes any change, a model object will be updated.

#### Assigning actions to user input (keys, mouse buttons) (Georgiy)
In the authoring environment, there will be a button to trigger the input dialog.
It will provide a list of objects/behaviors that can be activated by user input.
The user will have opportunity to perform their input, and the system will capture it.
In the game player, if the user performs the input again and the necessary components are present, they will activate accordingly.

#### Customizing available vehicle objects (Georgiy)
By clicking the ‘Vehicle’ button in the authoring environment, the user can switch to the vehicle object customization tab.
The user will have the opportunity to select which objects will be given to the player to choose from when they select their level.
They will have the opportunity to edit the attributes of the selected objects like HP and friction, and edit the global vehicle attributes.

#### Save Level (Paulina)
1. User clicks Save in the authoring environment
2. System validates text fields.
3. If validation fails, show an error message and block the save.
4. User is prompted for a level name if saving for the first time
5. System serializes terrain, obstacles, goal, and physics settings to a JSON file
6. File is written to disk and a success message is shown

#### Panning and Zooming the Camera (Mason)
The input detects signals for zooming/panning
Compute the new viewing window
Validate if the viewing window is valid
If so, set the new coordinates for the viewing window and adjust it. If it is not, no UI is changed

## Login/User Auth (Iurii)

Create an account
User creates an account
The account is created in the database
User gets a message that account is created successfully
Valid Log in
User enters valid credentials
Model makes the request to the database
The credentials are confirmed
User enters into their account
Invalid Log in
User enters invalid credentials
Model makes the request to the database
The credentials are confirmed
User enters into their account

## Settings (Iurii)
Name - Users can configure their name
Change Password - users can change their password
Customizations - users can add and delete cars for certain levels manually

### Vehicle object selection tab / component palette (Emilio)

User clicks the Vehicle button in the authoring environment
System shows the currently available vehicle objects and the full object
library
User selects which parts should be available to the player in that level
System updates the level setup data to reflect the selected vehicle objects

### Switch between authoring panels (Emilio)

User selects a different authoring tab such as Terrain, Vehicle, or Goal
System saves the current unsaved UI state for the active panel
System switches to the selected panel without losing the current level data
System displays the tools and controls relevant to that panel

### Show authoring guidance and validation feedback (Emilio)

User hovers over a tool or attempts an invalid authoring action
System displays instructions, tool descriptions, or validation messages
If required level elements are missing, system shows what still needs to be
added
System updates the guidance as the level becomes closer to runnable
