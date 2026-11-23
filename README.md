# Simple-Platformer
For terra hackclub

To download, go to releases, select latest release, download the .zip and extract it
from there run the .exe and have fun!

anything new added will be labeled "UPDATE (version #)" at the bottom of this file 

controls: arrow keys for movement (down on magenta platforms to go through them) Z for zooming
X for dashing (please don't press modifier keys (shift, ctrl, alt, etc..) it bugs slightly)
red kills you, enemies (small moving guys) kill you
controls while editing: left/right click to place corner one, then again to place corner 2
middle click to select and object for editing/deletion
esc to stop selecting an object (bit buggy currently)
arrow keys to move around stage (may have to middle click on canvas first to activate)

This is a platformer, when you enter it auto loads map 1, to edit simply click the platform type you want (right side)
or click on the enemies button to select enemies to place, then click where you want to place, for objects you need
to click a second time to place it (will highlight what the object will look like), then a configuration panel will
appear
triggered by channel ID: what trigger ID (activates channel ID), triggers it
activates channel ID: for trigger objects, what ID it triggers (triggerd by channel ID)
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

UPDATE 4
    fully changed objects moving, when you create an object, or middle click to edit an object
    you will notice an "add waypoint" button, click this to add a waypoint (nodes that the object travels through
    one at a time), when you middle click an object it shows all the waypoints (white outline for non triggered waypoints (waypoints 
    for when the object isn't triggered), and yellow waypoints (for when object is triggered)), if you click and drag a waypoint (or the object itself)
    it will move along with your mouse, there is a number in the middle of the line too and from it to the next waypoints, double click the numbers
    to be able to edit its speed (px/f is how many pixels per frame, frames is how many frames it takes to travel to the next point), or double
    click px/f or frames to swap between the two
    Import/Export: self explanatory, if you want to save/create a level as/from a file you can
    Delete Level: this deletes everything from the current level, you can get the progress back by not saving
    and quitting out of the game, be sure you want to before pressing this (does have a confirmation screen).

UPDATE 5:
    added items to the game, when you place an item, if it is a weapon you can add waypoints to the weapon
    these waypoints are where the item moves to when you attack with it equipped relative to you, (if there is a waypoint
    ahead of it's start position by 100 pixels at a rate of 2 px/f when you attack it will move in front of you by 100 pixels
    at a rate of 2 px/f). you can also save items as templates, where you can select that item later and place it down
    fully made.
    Weapons when they hit an enemy kill the enemy. 


if you enjoyed playing please message me with what features you think i should add! (feedback is appreciated)
also if you go into the src folder and look at levels.json you can copy/paste to let other people play your level 
(no longer need as you can import/export, but still doable);