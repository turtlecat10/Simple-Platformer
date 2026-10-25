package src.game;
import java.awt.Rectangle;
import java.util.ArrayList;

public class Movement{
    //world
    private final int tileSize;
    private final int worldWidth;
    private final int worldHeight;
    private final int playerSpeed;
    private final ArrayList<GameObject> objects;
    
    //tracking where you are
    private int spawnX;
    private int spawnY;
    private int playerX;
    private int playerY;

    //inputs 
    private boolean left, right, up, down, zoom, dash, reset;

    //gravity
    private double velocityY = 0;
    private final double terminalVelocity = 20;
    private double gravityApproach = 0.03;

    //dashing
    private int dashDuration = 10;
    private int dashCooldown= 45;
    private int dashCounter = 0;
    private int dashCooldownCounter = 0;
    private boolean isDashing = false;
    private int dashDirectionX = 0;
    private int dashDirectionY = 0;
    private boolean dashAble = false;

    //jumping
    private final double initialJumpForce = -9;
    private final double holdJumpBoost = -.9;
    private final int maxJumpHoldTime = 30;
    private int jumpCounter = 0;
    private boolean stoppedJumping = true;
    private boolean hasDoubleJumped = false;
    private boolean isJumping = false;
    private boolean isGrounded = false;
    private boolean isDroppingThrough = false;
    
    //fps
    private int totalframes = 0;
    private int fps = 0;
    private long lastFrameCheck = System.nanoTime();
     
    public Movement(int tileSize, int worldWidth, int worldHeight, ArrayList<GameObject> objects, int startGridX, int startGridY, int speed){
        this.tileSize = tileSize;
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.objects = objects;
        this.playerSpeed = speed;

        this.playerX = startGridX * tileSize;
        this.playerY = startGridY * tileSize;
    }
    
    public void update(int currentMap){
        totalframes++;
        //frames per second
        if(System.nanoTime() - lastFrameCheck >= 1_000_000_000L){
            fps = totalframes;
            totalframes = 0;
            lastFrameCheck = System.nanoTime();
        }
        // move(currentMap);
        //checking if on the ground
        if(isGrounded){
            hasDoubleJumped = false;
            if(velocityY > 0) velocityY = 0;
        }
        if(dashCooldownCounter > 0){
            dashCooldownCounter--;
        }

        if(dash && !isDashing && dashCooldownCounter == 0 && dashAble){
            dashDirectionX = 0;
            int currentDirX = 0;
            if(left != right) currentDirX = left ? -1 : 1;

            if(currentDirX != 0){
                dashAble = false;
                isDashing = true;
                dashDirectionX = currentDirX;
                dashCounter = 0;
                dashCooldownCounter = dashCooldown;
                velocityY = 0;
            }
            dashDirectionY = 0;
            int currentDirY = 0;
            if(up != down) currentDirY = up ? -1 : 1;
            // System.out.println(currentDirY);
            if(currentDirY != 0){
                dashAble = false;
                isDashing = true;
                dashDirectionY = currentDirY;
                dashCounter = 0;
                dashCooldownCounter = dashCooldown;
            }
        }

        //applying gravity  
        if(!isDashing){
            if(!isGrounded){
                velocityY += Math.abs((terminalVelocity - velocityY)) * gravityApproach;
                if(velocityY > terminalVelocity){
                    velocityY = terminalVelocity;
                }
            }
            jump();
        }else{
            dashCounter++;
            if(dashCounter >= dashDuration){
                isDashing = false;
            }
        }
        move(currentMap);
        if(reset){
            respawnPlayer(spawnX, spawnY);
        }
    }

    private void jump(){
        //if jumping
        if(isGrounded){
            hasDoubleJumped = false;
        }
        
        if(up && isGrounded){
            velocityY = initialJumpForce;
            isGrounded = false;
            isJumping = true;
            jumpCounter = 0;
            stoppedJumping = false;
        }
        //for increasing height based on how long you hold jump
        if(up && isJumping){
            if(jumpCounter < maxJumpHoldTime){
                if(hasDoubleJumped){
                    velocityY += holdJumpBoost*.9;
                }else{
                    velocityY += holdJumpBoost;
                }
                jumpCounter++;
            }else{
                isJumping = false;
            }
        }
        //to stop pressing up
        if(!up){
            stoppedJumping = true;
            isJumping = false;
            jumpCounter = 0;
        }
        if(up && !isJumping && !isGrounded && !hasDoubleJumped && stoppedJumping){
            hasDoubleJumped = true;
            velocityY = initialJumpForce*.75;
            isJumping = true;
            jumpCounter = 0;
            // gravityY = 0;
        }
    }
    
    private void move(int currentMap){
        if(isGrounded){
            hasDoubleJumped = false;
        }
        //get x direction
        int directionX = 0;
        if(left != right){
            directionX = left ? -1 : 1;
        }
        //for speed
        // double speedMult = zoom ? 1.5 : 1.0;
        int moveX = 0;
        int moveY = (int) velocityY;
        if(isDashing){
            int dashSpeedSpeed = 16;
            moveX = dashDirectionX * dashSpeedSpeed;
            moveY = dashDirectionY * dashSpeedSpeed;
        }else{
            double speedMult = zoom ? 1.5 : 1.0;
            moveX = (int) (Math.abs(playerSpeed) * directionX * speedMult);
        }

        //if not moving will change later
        if(moveX == 0 && moveY == 0){
            checkGroundStatus(currentMap);
            checkTriggerObjects(playerX, playerY, currentMap);
            return;
        }

        // One-Way Platform Tracking Mechanics
        boolean falling = (moveY > 0);
        Rectangle bodyBounds = new Rectangle(playerX, playerY, tileSize, tileSize);
        boolean embeddedInOneWay = false;
        for(GameObject obj : objects){
            if(obj.mapID == currentMap && obj.type == GameObject.Type.oneWay){
                if(bodyBounds.intersects(new Rectangle(obj.x, obj.y, obj.width, obj.height))){
                    embeddedInOneWay = true;
                    break;
                }
            }
        }
        if(!embeddedInOneWay){
            isDroppingThrough = false;
        }
        boolean allowOneWayCollision = falling && !down && !embeddedInOneWay && !isDroppingThrough;

        //horizontal movement
        if(moveX != 0){
            int targetX = playerX + moveX;

            if(targetX < 0) targetX = 0;
            // if(targetX > worldWidth - tileSize) targetX = worldWidth - tileSize;
            
            ArrayList<GameObject> hitsX = checkCornerCollision(playerX, playerY, targetX, playerY, currentMap);
            //sort
            final int currentPX = playerX;
            hitsX.sort((a, b) -> Integer.compare(Math.abs(a.x - currentPX), Math.abs(b.x - currentPX)));

            boolean collidedX = false;
            for (GameObject obj : hitsX) {
                if (obj.type == GameObject.Type.solid) {
                    if (moveX > 0) playerX = obj.x - tileSize;
                    else playerX = obj.x + obj.width;
                    collidedX = true;
                    break; // Stop evaluating immediately upon striking a wall
                }
            }

            if (!collidedX) {
                playerX = targetX; // Safe to move
            }
        }
        //vertical movement
        if(moveY != 0){
            int targetY = playerY + moveY;

            if(targetY < 0) {
                targetY = 0;
                // System.out.println("What!");
                jumpCounter = maxJumpHoldTime;
                isJumping = false;
                velocityY = 1;
            }

            ArrayList<GameObject> hitsY = checkCornerCollision(playerX, playerY, playerX, targetY, currentMap);
            
            final int currentPY = playerY;
            hitsY.sort((a, b) -> Integer.compare(Math.abs(a.y - currentPY), Math.abs(b.y - currentPY)));

            boolean collidedY = false;
            for (GameObject obj : hitsY) {
                if (obj.type == GameObject.Type.solid) {
                    if (falling) {
                        playerY = obj.y - tileSize;
                        isGrounded = true;
                        dashAble = true;
                        hasDoubleJumped = false;         
                    } else {
                        playerY = obj.y + obj.height;
                        isJumping = false; // Cancel jump hold
                    }
                    velocityY = 0;
                    collidedY = true;
                    break;
                }

                if (obj.type == GameObject.Type.oneWay && allowOneWayCollision) {
                    if (playerY + tileSize <= obj.y) {
                        playerY = obj.y - tileSize;
                        dashAble = true;
                        isGrounded = true;
                        hasDoubleJumped = false;         
                        velocityY = 0;
                        collidedY = true;
                        break;
                    }
                }
            }
            if (!collidedY) {
                // trigger pit death
                if (targetY > worldHeight - tileSize) {
                    respawnPlayer(spawnX, spawnY);
                    return;
                }
                if(targetY < 0){
                    isJumping = false;
                }
                playerY = targetY; // Safe to move 
                isGrounded = false;
            }
        }
        checkTriggerObjects(playerX, playerY, currentMap);
        checkGroundStatus(currentMap);
    }

    public void setInitialSpawn(int x, int y){
        this.spawnX = x;
        this.spawnY = y;
        this.playerX = x;
        this.playerY = y;
        
        this.velocityY = 0;
        this.isGrounded = true;
        this.isDroppingThrough = false;
        this.hasDoubleJumped = false;
    }
    
    public void respawnPlayer(int gridX, int gridY){
        this.playerX = this.spawnX;
        this.playerY = this.spawnY;
        this.velocityY = 0;
        this.isGrounded = false;
        this.dashAble = false;
        this.isJumping = false;
        this.isDroppingThrough = false;
    }
        
    private ArrayList<GameObject> checkCornerCollision(int startX, int startY, int endX, int endY, int currentMap){
        ArrayList<GameObject> objColliding = new ArrayList<>();
        
        int minX = Math.min(startX, endX);
        int maxX = Math.max(startX, endX) + tileSize;
        int minY = Math.min(startY, endY);
        int maxY = Math.max(startY, endY) + tileSize;
        
        Rectangle sweepBox = new Rectangle(minX, minY, maxX - minX, maxY - minY);

        for(GameObject obj : objects){
            if(obj.mapID != currentMap){
                continue;
            }

            Rectangle tile = new Rectangle(obj.x, obj.y, obj.width, obj.height);

            if(sweepBox.intersects(tile)){
                objColliding.add(obj);
            }
        }

        return objColliding;
    }
    
    private boolean checkCollision(boolean checkOneWayPlatforms, int currentMap){
        Rectangle pBounds = new Rectangle(playerX, playerY, tileSize, tileSize);
        for(GameObject obj : objects){
            if(obj.mapID != currentMap){
                continue;
            }
            Rectangle oBounds = new Rectangle(obj.x, obj.y, obj.width, obj.height);
            if(pBounds.intersects(oBounds)){
                if(obj.type == GameObject.Type.solid){
                    return true;
                }
                if(obj.type != GameObject.Type.solid){
                    if(checkOneWayPlatforms && obj.type == GameObject.Type.oneWay && !isDroppingThrough){
                        if((playerY + tileSize - 1) >= obj.y && (playerY + tileSize - 1) <= obj.y){
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
        
    private void checkTriggerObjects(int checkX, int checkY, int currentMap){
        Rectangle playerBounds = new Rectangle(checkX, checkY, tileSize, tileSize);

        for(GameObject obj : objects){
            if(obj.mapID != currentMap) continue;

            Rectangle triggerBounds = new Rectangle(obj.x, obj.y, obj.width, obj.height);

            if(playerBounds.intersects(triggerBounds)){
                if(obj.type == GameObject.Type.death){
                    respawnPlayer(spawnX, spawnY);
                    return;
                }
                if(obj.type == GameObject.Type.respawn){
                    spawnX = obj.x;
                    spawnY = obj.y;
                }
                if(obj.type == GameObject.Type.ladder){
                    velocityY = 0;
                    if(up != down){
                        velocityY = (up) ? -5 : 5;
                    }
                    if(down){
                        isGrounded = false;
                    }else{
                        isGrounded = true;
                        dashAble = true;
                    }
                }
                if(obj.type == GameObject.Type.win){
                    MapEditorWindow.win();
                }
            }
        }
    }
    
    private void checkGroundStatus(int currentMap){
        if(velocityY == 0 && !isDroppingThrough){
            playerY += 1;
            boolean standingOnSolid = checkCollision(false, currentMap);
            boolean standingOnOneWay = checkCollision(true, currentMap);
            playerY -= 1;
            if(standingOnSolid){
                isGrounded = true;
                dashAble = true;
            }else if(standingOnOneWay){
                if(down){
                    playerY += 2;
                    velocityY = 1;
                    isGrounded = false;
                    isDroppingThrough = true;
                }else{
                    isGrounded = true;
                    dashAble = true;
                }
            }else{
                isGrounded = false;
            }
        }
    }
    
    public void setLeft(boolean pressed) {this.left = pressed;}
    public void setRight(boolean pressed) {this.right = pressed;}
    public void setUp(boolean pressed) {this.up = pressed;}
    public void setDown(boolean pressed) {this.down = pressed;}
    public void setZoom(boolean pressed) {this.zoom = pressed;}
    public void setDash(boolean pressed) {this.dash = pressed;}
    public void setReset(boolean pressed) {this.reset = pressed;}

    public int getPlayerX() { return playerX; }
    public int getPlayerY() { return playerY; }
    public double getVelocityY() { return velocityY; }
    public void setVelocityY(double val) { this.velocityY = val; }
    public boolean isGrounded() { return isGrounded; }
    public int getFps() { return fps; }
}