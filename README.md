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
On a player's turn, the player gets a random number $n$ (from $1$ to some pre-specified value, e.g., 12).
Then, the player takes at most two trains, and get off at the $n$-th station. The player claims that station subsequently.

The player's path should not involve duplicate segments ($A\to B\to A$ or $A\to B\to C\to\cdots\to A\to B$); 
neither should it involve stations claimed by other players previously. 

If no movements are possible, the player gets "stuck", and is skipped for this round.
If a player gets stuck too many times (e.g., 5), the player will be permanently skipped for the rest of the game.

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
After each move, the players' points will be given in the "<player_name> -- <current_point> / <best_point_possible>" form.\
The game ends if there is only one player left, or all stations have been claimed. 
The players' final points will be given subsequently.

**Map Editing**\
A player joins a RailchessStand, and types a command to start editing. When holding a blaze_rod in the main hand, a player\
right-clicks to select an existing station;\
left-clicks to select an existing station, or create one if there isn't any;\
right-clicks while sneaking to select or create a station, and connect it to the previous selected station; and\
left-clicks while sneaking to remove a station.\
The line number for connecting the stations can be chosen by commands; 
by default, every possible path of train exists, e.g., if one connects stations $a$ and $b$, $b$ and $c$, and $b$ and $d$ with the same line, 
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
  The command fails if no RailchessStands are found, or if the RailchessStand c=already contains no less than $4$ players.
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
* **/rc remove**\
  Requires permission: railchess.edit.\
  Removes the current RailchessStand.

**/rcgame**: The command for gameplay.