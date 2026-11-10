package src.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.awt.Rectangle;

public class PatrolEnemy extends Enemy{

    public PatrolEnemy(int x, int y, int width, int height, int mapID, double velX){
        super(x, y, width, height, mapID, velX);
        this.type = "PATROL";
        if(this.velX != 0){
            this.velX = velX;
        }else{
            this.velX = 8.5;
        }
    }

    @Override 
    public void aiTick(ArrayList<GameObject> objects){
        if(!this.isGrounded){
           this.velY += 0.5;
        }

        int lookAheadCheckX = (this.velX > 0) ? (int) (this.x + this.width + this.velX) : (int)(this.x - this.velX);
        int lookAheadCheckY = this.y + 4; // y is bottom-left
        Rectangle edgeSensorBox = new Rectangle(lookAheadCheckX, lookAheadCheckY, (int) Math.abs(this.velX), 8);

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
            this.velX = -this.velX;
        }
        updatePhysics(objects);
    }

    @Override
    protected void handleWallBump(){
        this.velX = -this.velX;
    }
    @Override 
    public void draw(Graphics2D g2){
        int renderY = this.y - this.height;
        g2.setColor(new Color(139, 0, 0));
        g2.fillRect(this.x, renderY, this.width, this.height);
        g2.setColor(Color.BLACK);
        g2.drawRect(this.x, renderY, this.width, this.height);
    }
}