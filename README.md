# Railchess
The Minecraft Bukkit implementation of a game created by SlinkierApple13 and his friends.

## The game
### Overview
Two to four players take turns to move in a rail transit system, claiming stations for points. 
The player with the most points wins.

### Scoring
Each station has a **station value**, which is equal to the number of its neighbouring stations.
By claiming a station, a player gains points equal to the station's value.

### Movement
On a player's turn, the player gets a random number $n$ (from $1$ to some pre-specified value, e.g., $12$).
Then, the player takes at most two trains, and get off at the $n$-th station. The player claims that station subsequently.

The player's path should not involve duplicate segments ($A\to B\to A$ or $A\to B\to C\to\cdots\to A\to B$); 
neither should it involve stations claimed by other players previously. 

If no movements are possible, the player gets "stuck", and is skipped for this round.
If a player gets stuck too many times (e.g., $5$), the player will be permanently skipped for the rest of the game.

### Autoclaim
A station is claimed automatically by a player the moment he/she becomes the only player who can possibly reach that station.

## The plugin
### Requirements
* The server supports Bukkit API
* Minecraft version Java 1.20.4

### Permissions
* **railchess.edit**\
Default: op. Permission for map editing.
* **railchess.play**\
Default: true. Permission for playing Railchess games.
* **railchess.subscribe**\
Default: true. Permission for spectating Railchess games.

### Basics

For details about commands, see the next subsection.

**RailchessStand**\
A virtual site for playing Railchess games and editing maps. 
RailchessStands can be created through commands.\
A RailchessStand does not display actual rail transit maps;
they must be added manually, for example, by a map-art generator.

**Gameplay**\
Players join a RailchessStand (by commands), and a player types a command to start a game.
At one's turn to move, one right-clicks on one's destination with a blaze_rod in one's main hand.\
Each player has a specific colour; 
a player's position and claimed stations will be marked with his/her respective colour.\
Players can leave the game by typing commands. 
A player will leave the game automatically if he/she gets stuck too many times, or he/she has no more possible stations to claim.\
After each move, the players' points will be given in the form of\
"<player_name> -- <current_point> / <best_point_possible>".\
The game ends if there is only one player left, or all stations have been claimed. 
The players' final points will be given subsequently.

**Game Replay**\
A player joins a RailchessStand, and types a command to replay a previous game. 
When holding a blaze_rod in the main hand, a player\
~ right-clicks to go to the next move;\
~ left-clicks to go to the previous move.

**Map Editing**\
A player joins a RailchessStand, and types a command to start editing. When holding a blaze_rod in the main hand, a player\
~ right-clicks to select an existing station;\
~ left-clicks to select an existing station, or create one if there isn't any;\
~ right-clicks while sneaking to select or create a station, and connect it to the previous selected station; and,\
~ left-clicks while sneaking to remove a station.\
The line number for connecting the stations can be chosen by commands. 
By default, every possible path of train exists, e.g., if one connects stations $a$ and $b$, $b$ and $c$, and $b$ and $d$ with the same line, 
then trains following the paths $a-b-c$, $a-b-d$, and $c-b-d$ are all possible. One may block certain train paths through commands.

### Commands
**/rc**: The plugin's main command.

* **/rc create \<horizontalDirectionX\> \<horizontalDirectionZ\> \<width\> \<height\>**\
  Requires permission: railchess.edit.\
  Creates a RailchessStand at current location, with the given direction, width, and height. 
  For example, if the player stands at $(0, 60, 0)$ and runs\
  /rc create 1 0 15 12\
  a RailchessStand will be created with the four corners $(0, 60, 0)$, $(15, 60, 0)$, $(15, 72, 0)$, and $(0, 72, 0)$.
* **/rc list**\
  Requires permission: None.\
  Lists all RailchessStands on the server.
* **/rc join**\
  Requires permission: railchess.play.\
  Joins the nearest RailchessStand within $8$ blocks. 
  The command fails if no RailchessStands are found, or if the RailchessStand already contains $4$ players.
* **/rc leave**\
  Requires permission: railchess.play.\
  Leaves the current RailchessStand.
* **/rc duplicate**\
  Requires permission: railchess.edit.\
  Joins the current RailchessStand once more.
* **/rc edit \<mapName\>**\
  Requires permission: railchess.edit.\
  Edits the specified map.
  If the map does not exist, a new map with the given name is created.
* **/rc play|game \<mapName\> \<maxN\> \<maxStucks\> \<hint: true|false\>**\
  Requires permission: railchess.play.\
  Starts a game with all players in the current RailchessStand with permission railchess.play, with the specified map, 
  the given cap of the random number $n$, and the number of stucks for a player to be permanently skipped.
  If the final parameter is true, all possible options will be highlighted on a player's turn to move.
* **/rc replay \<gameId\>**\
  Requires permission: railchess.subscribe.\
  Starts replaying the game with the given id.
* **/rc remove \<standName\>**\
  Requires permission: railchess.edit.\
  Removes the RailchessStand with the given name.

**/rcgame**: The command for gameplay.
* **/rcgame leave**\
  Requires permission: railchess.subscribe.\
  Leaves current game.
* **/rcgame spectate**\
  Requires permission: railchess.subscribe.\
  Subscribes to the nearest game within $8$ blocks, so as to be informed with developments of the game even if the player is far away.
* **/rcgame despectate**\
  Requires permission: railchess.subscribe.\
  Stops subscribing to the current game.

**/rclog**: The command for game logs.
* **/rclog list**
  Requires permission: railchess.subscribe.\
  Lists all previous game logs.

**/rcreplay**: The command for game replays.
* **/rcreplay join**\
  Requires permission: railchess.subscribe.\
  Joins a nearby game replay.
* **/rcreplay leave**\
  Requires permission: railchess.subscribe.\
  Leaves the present game replay.
* **/rcreplay goto \<step\>**\
  Requires permission: railchess.subscribe.\
  Jumps to the given step.
* **/rcreplay close**\
  Requires permission: railchess.subscribe.\
  Closes the current replay.

**/rcmap**: The command for maps.
* **/rcmap list**\
  Requires permission: railchess.subscribe.\
  Lists all available maps.
* **/rcmap rename \<from\> \<to\>**\
  Requires permission: railchess.edit.\
  Renames the specified map to the given name.

**/rcedit**: The command for editing maps.
* **/rcedit join**\
  Requires permission: railchess.edit.\
  Joins the nearest map editing in $8$ blocks.
* **/rcedit leave**\
  Requires permission: railchess.edit.\
  Leaves from the current map editing.
* **/rcedit save**\
  Requires permission: railchess.edit.\
  Saves the current map.
* **/rcedit saveAs \<mapName\>**\
  Requires permission: railchess.edit.\
  Saves the current map as the given name. 
  This command can be used for map copying.
* **/rcedit flush**\
  Requires permission: railchess.edit.\
  Updates the spawns and station values in the map. 
  Maps are updated automatically on saving.
* **/rcedit close**\
  Requires permission: railchess.edit.\
  Closes the current editor, without saving the map.
* **/rcedit readonly**\
  Requires permission: railchess.edit.\
  Sets the current map to read-only (so that games can be recorded and replayed).
* **/rcedit line <lineNumber>**\
  Sets the current line number.
* **/rcedit connect \<connectType\>**\
  Requires permission: railchess.edit.\
  If connectType is $0$: removes all connections between the current selected station and the previous selected station.\
  If connectType is $1$: connects the current selected station with the previous selected station with the current line
  in only one direction (from previous to current).\
  If connectType is $2$: connects the current selected station with the previous selected station with the current line in both directions 
  (same as right-clicking while sneaking with a blaze_rod in the main hand).
* **/rcedit add|remove notransfer \<line1\> \<line2\>**\
  Requires permission: railchess.edit.\
  Forbids or allows taking trains of both of the two given lines in one move.
* **/rcedit add|remove nospawn**\
  Requires permission: railchess.edit.\
  Forbids or allows players spawning in both the current and previous selected station under most circumstances.
* **/rcedit add|remove notrain \<from\> \<via\> \<to\> \<line\>**\
  Requires permission: railchess.edit.\
  Forbids or allows trains from \<from\> to \<to\> via \<via\> on line \<line\>.\
  Please note that trains from \<to\> to \<from\> via \<via\> are not affected.
  
Note: Map deletions are very dangerous (since it takes a lot of time to create one), and thus
there is no command to directly delete a map. 
However, if one does want to delete a non-read-only map, one may use "/rcedit saveAs" to cover it with an empty map.
