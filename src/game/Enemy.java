package src.game;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;

public abstract class Enemy {
    public int x, y; // bottom-left corner
    public int startX, startY;
    public int width, height;
    public double velX, velY;
    public int mapID;
    public boolean isGrounded;
    public String type;

    public Enemy(int x, int y, int width, int height, int mapID){
        this.x = x;
        this.y = y;
        this.startX = x;
        this.startY = y;
        this.width = width;
        this.height = height;
        this.mapID = mapID;
        this.velX = 0;
        this.velY = 0;
        this.isGrounded = false;
    }
    public abstract void aiTick(ArrayList<GameObject> objects);

    public abstract void draw(Graphics2D g2);

    public void updatePhysics(ArrayList<GameObject> objects){
        if(!this.isGrounded){
            this.velY += 0.4;
            if(this.velY > 12) this.velY = 12;
        }
        int moveY = (int) Math.round(this.velY);
        if(moveY != 0){
            this.y += moveY;
            this.isGrounded = false;
            Rectangle eBounds = getBounds(); // Uses y as bottom-left
            
            for(GameObject obj : objects){
                if(obj.mapID != this.mapID) continue;
                if(obj.isTriggered && !obj.triggeredDisplay) continue;
                if(obj.type != GameObject.Type.solid) continue;

                int objTop = obj.currentY - obj.height;
                int objBottom = obj.currentY;
                Rectangle blockBounds = new Rectangle(obj.currentX, objTop, obj.width, obj.height);
                
                if(eBounds.intersects(blockBounds)){
                    if(moveY > 0){
                        this.y = objTop; // land feet on top of block
                        this.isGrounded = true;
                    }else{
                        this.y = objBottom + this.height; // hit ceiling from below
                    }
                    this.velY = 0;
                    break;
                }
            }
        }
        int moveX = (int) Math.round(this.velX);
        if(moveX != 0){
            this.x += moveX;
            Rectangle eBounds = getBounds();

            for(GameObject obj : objects){
                if(obj.mapID != this.mapID) continue;
                if(obj.isTriggered && !obj.triggeredDisplay) continue;
                if(obj.type != GameObject.Type.solid) continue;

                int objTop = obj.currentY - obj.height;
                Rectangle blockBounds = new Rectangle(obj.currentX, objTop, obj.width, obj.height);

                if(eBounds.intersects(blockBounds)){
                    if(moveX > 0){
                        this.x = obj.currentX - this.width;
                    }else{
                        this.x = obj.currentX + obj.width;
                    }
                    handleWallBump();
                    break;
                }
            }
        }
    }
    protected void handleWallBump(){

    }
    public Rectangle getBounds(){
        // y is bottom-left, so top-left for Rectangle is y - height
        return new Rectangle(this.x, this.y - this.height, this.width, this.height);
    }
}