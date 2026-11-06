package src.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.awt.Rectangle;

public class PatrolEnemy extends Enemy{

    public PatrolEnemy(int x, int y, int mapID){
        super(x, y, 32, 32, mapID);
        this.type = "PATROL";
        this.velX = 4.5;
    }

    @Override 
    public void aiTick(ArrayList<GameObject> objects){
        if(!isGrounded) return;

        int lookAheadCheckX = (velX > 0) ? (int) (x + width + velX) : (int)(x - velX);
        int lookAheadCheckY = y + 4; // y is bottom-left
        Rectangle edgeSensorBox = new Rectangle(lookAheadCheckX, lookAheadCheckY, (int) Math.abs(velX), 8);

        boolean solidGroundAhead = false;
        for(GameObject obj: objects){
            if (obj.mapID != this.mapID) continue;
            if (obj.isTriggered && !obj.triggeredDisplay) continue;
            if (obj.type == GameObject.Type.solid) {
                int objTop = obj.currentY - obj.height;
                Rectangle blockBounds = new Rectangle(obj.currentX, objTop, obj.width, obj.height);
                if(edgeSensorBox.intersects(blockBounds)){
                    solidGroundAhead = true;
                    break;
                }
            }
        }
        if(!solidGroundAhead){
            velX = -velX;
        }
    }

    @Override
    protected void handleWallBump(){
        velX = -velX;
    }
    @Override 
    public void draw(Graphics2D g2){
        int renderY = y - height;
        g2.setColor(new Color(139, 0, 0));
        g2.fillRect(x, renderY, width, height);
        g2.setColor(Color.BLACK);
        g2.drawRect(x, renderY, width, height);
    }
}