package src.game;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;


public abstract class Enemy {
    public int x, y;
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
        if(!isGrounded){
            velY += 0.4;
            if(velY > 12) velY = 12;
        }
        int moveY = (int) Math.round(velY);
        if(moveY != 0){
            y+= moveY;
            isGrounded = false;
            Rectangle eBounds = getBounds();
            
            for(GameObject obj : objects){
                if(obj.mapID != this.mapID) continue;
                if(obj.isTriggered && !obj.triggeredDisplay) continue;
                if(obj.type != GameObject.Type.solid) continue;

                int actualY = obj.currentY;
                Rectangle blockBounds = new Rectangle(obj.currentX, actualY, obj.width, obj.height);
                if(eBounds.intersects(blockBounds)){
                    if(moveY > 0){
                        y = actualY - height;
                        isGrounded = true;
                    }else{
                        y = actualY + obj.height;
                    }
                    velY = 0;
                    break;
                }
            }
        }
        int moveX = (int) Math.round(velX);
        if(moveX != 0){
            x += moveX;
            Rectangle eBounds = getBounds();

            for(GameObject obj : objects){
                if(obj.mapID != this.mapID) continue;
                if(obj.isTriggered && !obj.triggeredDisplay) continue;
                if(obj.type != GameObject.Type.solid) continue;

                Rectangle blockBounds = new Rectangle(obj.currentX, obj.currentY, obj.width, obj.height);

                if(eBounds.intersects(blockBounds)){
                    if(moveX > 0){
                        x = obj.currentX - width;
                    }else{
                        x = obj.currentX + obj.width;
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
        return new Rectangle(x, y, width, height);
    }
}
