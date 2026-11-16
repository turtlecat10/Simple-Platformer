package src.game;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.awt.Color;

public abstract class Enemy {
    public int x, y; // bottom-left corner
    public int startX, startY;
    public int width, height;
    public double velX, velY;
    public int mapID;
    public boolean isGrounded;
    public String type;
    public boolean isDead = false;
    public boolean hasPathfinding = false;
    public GameObject standingOnPlatform = null;

    public Enemy(int x, int y, int width, int height, int mapID, double velX){
        this.x = x;
        this.y = y;
        this.startX = x;
        this.startY = y;
        this.width = width;
        this.height = height;
        this.mapID = mapID;
        this.velX = velX;
        this.velY = 0;
        this.isGrounded = false;
    }
    
    public abstract void aiTick(ArrayList<GameObject> objects, double playerX, double playerY);

    public abstract void draw(Graphics2D g2);

    public abstract Color getParticleColor();

    public void updatePhysics(ArrayList<GameObject> objects){
        if(this.isDead) return;

        if(!this.isGrounded){
            this.velY += 0.4;
            if(this.velY > 12) this.velY = 12;
        }

        int moveY = (int) Math.round(this.velY);
        if(moveY != 0){
            this.y += moveY;
            this.isGrounded = false;
            this.standingOnPlatform = null;
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
                        this.standingOnPlatform = obj;
                    }else{
                        this.y = objBottom + this.height; // hit ceiling from below
                    }
                    this.velY = 0;
                    break;
                }
            }
        }else if(this.isGrounded){
            Rectangle footSensor = new Rectangle(this.x + 2, this.y, this.width - 4, 3);
            boolean supported = false;
            for(GameObject obj : objects){
                if(obj.mapID != this.mapID) continue;
                if (obj.isTriggered && !obj.triggeredDisplay) continue;
                if (obj.type != GameObject.Type.solid) continue;
                Rectangle blockBounds = new Rectangle(obj.currentX, obj.currentY - obj.height, obj.width, obj.height);
                if (footSensor.intersects(blockBounds)) {
                    supported = true;
                    this.standingOnPlatform = obj;
                    break;
                }
            }
            if(!supported){
                this.isGrounded = false;
                this.standingOnPlatform = null;
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
    
    public void resetToStart(){
        this.x = this.startX;
        this.y = this.startY;
        this.velY = 0;
        this.isDead = false;
        this.standingOnPlatform = null;
    }

    public void applyPlatformInteractions(ArrayList<GameObject> objects, int currentMap){
        if(this.isDead) return;

        if(this.isGrounded && this.standingOnPlatform != null){
            GameObject plat = this.standingOnPlatform;
            if(plat.mapID == this.mapID && (!plat.isTriggered || plat.triggeredDisplay)){
                this.x += (int) Math.round(plat.deltaX);
                this.y += (int) Math.round(plat.deltaY);
            }
        }
        for(GameObject obj : objects){
            if(obj.mapID != currentMap || obj.type != GameObject.Type.solid) continue;
            if(obj.deltaX == 0 && obj.deltaY == 0) continue;

            Rectangle eBounds = getBounds();
            int objTop = obj.currentY - obj.height;
            Rectangle blockBounds = new Rectangle(obj.currentX, objTop, obj.width, obj.height);

            if(eBounds.intersects(blockBounds)){
                resolveSafePlatformPush(obj, objects, currentMap);
                if(this.isDead) return;
            }
        }
        
        for(GameObject obj : objects){
            if (obj.mapID != currentMap || obj.type != GameObject.Type.solid || (obj.isTriggered && !obj.triggeredDisplay)) continue;
            if (obj.deltaX == 0 && obj.deltaY == 0) continue;

            Rectangle eBounds = getBounds();
            int objTop = obj.currentY - obj.height;
            Rectangle blockBounds = new Rectangle(obj.currentX, objTop, obj.width, obj.height);

            if(eBounds.intersects(blockBounds)){
                if(obj.deltaX > 0){
                    this.x = obj.currentX + obj.width;
                }else if(obj.deltaX < 0){
                    this.x = obj.currentX - this.width;
                }
                if(obj.deltaY > 0){
                    this.y = obj.currentY + this.height;
                    this.isGrounded = false;
                }else if(obj.deltaY < 0){
                    this.y = objTop;
                    this.isGrounded = true;
                    this.standingOnPlatform = obj;
                }
            }
        }

    }

    public void resolveSafePlatformPush(GameObject movingPlatform, ArrayList<GameObject> objects, int currentMap) {
        int enemyLeft = (int) Math.round(this.x);
        int enemyRight = (int) Math.round(this.x + this.width);
        int enemyBottom = (int) Math.round(this.y);
        int enemyTop = (int) Math.round(this.y - this.height);

        int prevLeft = (int) Math.round(movingPlatform.currentX - movingPlatform.deltaX);
        int left = (int) Math.round(movingPlatform.currentX);
        int prevRight = (int) Math.round(movingPlatform.currentX + movingPlatform.width - movingPlatform.deltaX);
        int right = (int) Math.round(movingPlatform.currentX + movingPlatform.width);
        
        int prevTop = (int) Math.round(movingPlatform.currentY - movingPlatform.height - movingPlatform.deltaY);
        int top = (int) Math.round(movingPlatform.currentY - movingPlatform.height);
        int prevBottom = (int) Math.round(movingPlatform.currentY - movingPlatform.deltaY);
        int bottom = (int) Math.round(movingPlatform.currentY);

        if(movingPlatform == standingOnPlatform){
            if(movingPlatform.deltaY < 0){
                for(GameObject obj : objects){
                    if(obj.mapID != currentMap || obj.type != GameObject.Type.solid || movingPlatform == obj || (obj.isTriggered && !obj.triggeredDisplay)) continue;
                    int objLeft = (int) (obj.currentX);
                    int objRight = (int) (obj.currentX + obj.width);
                    int objBottomPrev = (int) (obj.currentY - obj.deltaY);
                    int objBottom = (int) (obj.currentY);
                    if(enemyRight > objLeft && enemyLeft < objRight){
                        if((prevTop - objBottomPrev >= this.height) && (top - objBottom < this.height)){
                            this.isDead = true;
                            break;
                        }
                    }
                }
            }
        }else{
            for(GameObject obj : objects){
                if(obj.mapID != currentMap || obj.type != GameObject.Type.solid || movingPlatform == obj || (obj.isTriggered && !obj.triggeredDisplay)) continue;
                int prevLeftObj = (int) (obj.currentX - obj.deltaX);
                int leftObj = (int) (obj.currentX);
                int prevRightObj = (int) (obj.currentX + obj.width - obj.deltaX);
                int rightObj = (int) (obj.currentX + obj.width);
                int prevBottomObj = (int) (obj.currentY - obj.deltaY);
                int bottomObj = (int) (obj.currentY);
                int prevTopObj = (int) (obj.currentY - obj.height - obj.deltaY);
                int topObj = (int) (obj.currentY - obj.height);
                
                if(movingPlatform.deltaX > 0){
                    if(enemyBottom > top && enemyTop < bottom && enemyBottom > topObj && enemyTop < bottomObj){
                        if ((prevLeftObj - prevRight >= this.width) && (leftObj - right < this.width)) {
                            if (enemyLeft >= prevRight && enemyRight <= leftObj) {
                                this.isDead = true;
                                break;
                            }
                        }
                    }
                }else if(movingPlatform.deltaX < 0){
                    if(enemyBottom > top && enemyTop < bottom && enemyBottom > topObj && enemyTop < bottomObj){
                        if ((prevLeft - prevRightObj >= this.width) && (left - rightObj < this.width)) {
                            if (enemyLeft >= rightObj && enemyRight <= prevLeft) {
                                this.isDead = true;
                                break;
                            }
                        }
                    }
                }
                if(movingPlatform.deltaY > 0){
                    if (enemyRight > left && enemyLeft < right && enemyRight > leftObj && enemyLeft < rightObj) {
                        if ((prevTopObj - prevBottom >= this.height) && (topObj - bottom < this.height)) {
                            this.isDead = true;
                            break;
                        }
                    }
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