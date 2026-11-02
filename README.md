# Simple-Platformer
For terra hackclub

controls: arrow keys for movement (down on magenta platforms to go through them) Z for zooming
X for dashing (please don't press modifier keys (shift, ctrl, alt, etc..) it bugs slightly)
red kills you, enemies (small moving guys) kill you
controls while editing: left/right click to place corner one, then again to place corner 2
middle click to select and object for editing/deletion
esc to stop selecting an object (bit buggy currently)
arrow keys to move around stage (may have to middle click on canvas first to activate)

To download, go to releases, select latest release, download the .zip and extract it
from there run the .exe and have fun!

This is a platformer, when you enter it auto loads map 1, to edit simply click the platform type you want (right side)
or click on the enemies button to select enemies to place, then click where you want to place, for objects you need
to click a second time to place it (will highlight what the object will look like), then a configuration panel will
appear
triggered by channel ID: what trigger ID (activates channel ID), triggers it
activates channel ID: for trigger objects, what ID it triggers (triggerd by channel ID)
travel duration in frames: how long it takes to move to it's oscillation destination (standard oscillation, and oscillation after triggered)
for the next three, after you click it, you need to move your mouse to the main grid screen
you will notice a highlighted shape that is the location where it will be placed for the next three
place standard oscillation: where it moves back and forth to/from
place triggered move target: after it's been triggered, this is it's home after being triggered (triggered by channel ID)
place oscillation after triggered: where it moves back and forth to/from after being triggered
NEVER: default, this is it's movement condition, tap it to change when it moves, (never: never moves, touch: move when you touch it, 
always: always moves, trigger: only moves after being triggered)
confirm and close: confirms and saves your choices you made in that window

below that, is things you can do with the game
save level: saves the level so you can close the game safely without loosing progress
play: play testing the level/ just playing the level to have fun
exit to editor: for when playing, so you can keep editing
load stage: input a number into mapID numberfield and press load stage to load that stage
free place: exit/enter out of grid placing mode, which forces you to place on grid
New map: loads a map that you haven't saved/placed any objects into
browse levels: opens a menu that shows all the levels you have created + saved

if you enjoyed playing please message me with what features you think i should add! (feedback is appreciated)
also if you go into the src folder and look at levels.json you can copy/paste to let other people play your level