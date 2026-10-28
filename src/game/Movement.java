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
    private final int playerSize;
    private final java.util.HashSet<GameObject> savedCheckpointTriggeredObjects = new java.util.HashSet<>();
    private final java.util.HashSet<GameObject> sessionTriggeredObjects = new java.util.HashSet<>();

    
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
    // private double gravityApproach = 0.03;

    private double currentVelocityX = 0.0;

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
    private final double initialJumpForce = -7.2;
    private final double holdJumpBoost = -.9;
    private final int maxJumpHoldTime = 30;
    private int jumpCounter = 0;
    private boolean stoppedJumping = true;
    private boolean hasDoubleJumped = false;
    private boolean isJumping = false;
    private boolean isGrounded = false;
    private boolean isDroppingThrough = false;

    private boolean isOnLadder = false;
    private int ladderGraceFrames = 0;
    private final int maxLadderGrace = 6;
    
    //fps
    private int totalframes = 0;
    private int fps = 0;
    private long lastFrameCheck = System.nanoTime();

    private int deathCount = 0;
    private int respawnTimerFrames = 0;
    public boolean isDead = false;
     
    public Movement(int tileSize, int worldWidth, int worldHeight, ArrayList<GameObject> objects, int startGridX, int startGridY, int speed){
        this.tileSize = tileSize;
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.objects = objects;
        this.playerSpeed = speed;
        this.playerSize = (int) (tileSize * .75);

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

        if(!isOnLadder && ladderGraceFrames > 0){
            ladderGraceFrames--;
        }
        //applying gravity  
        if(!isDashing){
            if(!isGrounded && !isOnLadder){
                if(velocityY >= 0){
                    double velocityRatio = (double) velocityY / terminalVelocity;
                    if(velocityRatio > 1.0) velocityRatio = 1.0;

                    double speedDampeningMultiplier = 1.0 - velocityRatio;

                    if(speedDampeningMultiplier < 0.05) speedDampeningMultiplier = 0.05;

                    double rawGravityStrength = 0.8;

                    velocityY += rawGravityStrength * speedDampeningMultiplier;

                    if(velocityY > terminalVelocity){
                        velocityY = terminalVelocity;
                    }
                }else{
                    velocityY += 0.8;
                }
            }
            if(isOnLadder || ladderGraceFrames > 0){
                if(up && (left || right || ladderGraceFrames > 0)){
                    isOnLadder = false;
                    ladderGraceFrames = 0;
                    isGrounded = false;
                    isJumping = true;
                    jumpCounter = 0;
                    stoppedJumping = false;
                    hasDoubleJumped = false;
                    velocityY = initialJumpForce;
                    if(left != right){
                        currentVelocityX = left ? -4.5 : 4.5;
                    }
                }
            }else{
                jump();
            }
        }else{
            dashCounter++;
            if(dashCounter >= dashDuration){
                isDashing = false;
            }

            int dashSpeedSpeed = 16;
            this.currentVelocityX = this.dashDirectionX * dashSpeedSpeed;

            if(this.dashDirectionY != 0){
                this.velocityY = this.dashDirectionY * (dashSpeedSpeed * 0.6);
            }
        }
        if(isDead){
            respawnTimerFrames++;
            if(respawnTimerFrames >= 15){
                isDead = false;
                respawnTimerFrames = 0;
                respawnPlayer(spawnX, spawnY);
            }
            currentVelocityX = 0;
            velocityY = 0;
            return;
        }
        if (objects != null) {
            for (GameObject obj : objects) {
                if (obj.mapID == currentMap) {
                    obj.updateMovement(); // Computes timeline positions live frame-by-frame
                }
            }
        }
        applyPlatformCarriagePhysics(currentMap);

        move(currentMap);
        
        
        if(reset){
            triggerDeathSequence(currentMap);
            reset = false;
        }
    }
    
    private void triggerDeathSequence(int currentMap){
        if(!isDead){
            isDead = true;
            respawnTimerFrames = 0;
            deathCount++;
            if (objects != null) {
                for (GameObject obj : objects) {
                    if (obj.mapID == currentMap) {
                        // Revert activation statuses
                        if (obj.isTriggered && !savedCheckpointTriggeredObjects.contains(obj)) {
                            obj.isTriggered = false;
                        }
                        obj.timeElapsed = 0;
                        obj.currentX = obj.x;
                        obj.currentY = obj.y;
                    }
                }
            }
            sessionTriggeredObjects.clear();
            sessionTriggeredObjects.addAll(savedCheckpointTriggeredObjects);
            if(MapEditorWindow.instance != null && MapEditorWindow.instance.gameCanvas != null){
                MapEditorWindow.instance.gameCanvas.spawnDeathExplosion(this.playerX, this.playerY);
            }
            
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
        int moveX = 0;
        int moveY = (int) velocityY;

        if(isDashing){
            int dashSpeedSpeed = 16;
            moveX = dashDirectionX * dashSpeedSpeed;
            moveY = dashDirectionY * dashSpeedSpeed;
            currentVelocityX = moveX;
        }else{
            double speedMult = zoom ? 1.5 : 1.0;
            double targetTopSpeed = Math.abs(playerSpeed) * directionX * speedMult;
            double accelRate = isGrounded ? 0.8 : 0.6;
            double frictionRate = isGrounded ? 0.75 : 0.92;

            if(directionX != 0){
                if(directionX > 0){
                    currentVelocityX += accelRate;
                    if(currentVelocityX > targetTopSpeed) currentVelocityX = targetTopSpeed;
                }else{
                    currentVelocityX -= accelRate;
                    if(currentVelocityX < targetTopSpeed) currentVelocityX = targetTopSpeed;
                }
            }else{
                currentVelocityX *= frictionRate;
                if(Math.abs(currentVelocityX) < 0.1){
                    currentVelocityX = 0;
                }
            }
            moveX = (int) Math.round(currentVelocityX);
        }

        //if not moving will change later
        // if(moveX == 0 && moveY == 0){
        //     checkGroundStatus(currentMap);
        //     return;
        // }

        // One-Way Platform Tracking Mechanics
        boolean falling = (moveY > 0);
        Rectangle bodyBounds = new Rectangle(playerX, playerY, playerSize, playerSize);
        boolean embeddedInOneWay = false;
        for(GameObject obj : objects){
            if(obj.mapID == currentMap && obj.type == GameObject.Type.oneWay){
                int actualX = obj.isTriggered ? obj.triggeredX : obj.x;
                int actualY = obj.isTriggered ? obj.triggeredY : obj.y;
                if(bodyBounds.intersects(new Rectangle(actualX, actualY, obj.width, obj.height))){
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
        if(moveX != 0 || moveX == 0){
            int targetX = playerX + moveX;

            if(targetX < 0) {
                targetX = 0;
                currentVelocityX = 0;
            }
            
            ArrayList<GameObject> hitsX = checkCornerCollision(playerX, playerY, targetX, playerY, currentMap);
            //sort
            final int currentPX = playerX;
            hitsX.sort((a, b) -> Integer.compare(Math.abs(a.x - currentPX), Math.abs(b.x - currentPX)));

            boolean collidedX = false;
            for (GameObject obj : hitsX) {
                if (obj.type == GameObject.Type.solid) {
                    int actualX = obj.currentX;
                    if (moveX > 0) playerX = actualX - playerSize;
                    else playerX = actualX + obj.width;
                    collidedX = true;
                    currentVelocityX = 0;
                    break; // Stop evaluating immediately upon striking a wall
                }
            }

            if (!collidedX) {
                playerX = targetX; // Safe to move
            }
        }
        //vertical movement
        if(moveY != 0 || moveY == 0){
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
                    int actualY = obj.currentY;
                    if (falling) {
                        playerY = actualY - playerSize;
                        isGrounded = true;
                        dashAble = true;
                        hasDoubleJumped = false;         
                    } else {
                        playerY = actualY + obj.height;
                        isJumping = false; // Cancel jump hold
                    }
                    velocityY = 0;
                    collidedY = true;
                    break;
                }

                if (obj.type == GameObject.Type.oneWay && allowOneWayCollision) {
                    int actualY = obj.isTriggered ? obj.triggeredY : obj.y;
                    if (playerY + playerSize <= actualY) {
                        playerY = actualY - playerSize;
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
                if (targetY > worldHeight - playerSize) {
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
        int maxX = Math.max(startX, endX) + playerSize;
        int minY = Math.min(startY, endY);
        int maxY = Math.max(startY, endY) + playerSize;
        
        Rectangle sweepBox = new Rectangle(minX, minY, maxX - minX, maxY - minY);

        for(GameObject obj : objects){
            if(obj.mapID != currentMap){
                continue;
            }
            if(obj.isTriggered && !obj.triggeredDisplay){
                continue;
            }
            int actualX = obj.currentX;
            int actualY = obj.currentY;

            Rectangle tile = new Rectangle(actualX, actualY, obj.width, obj.height);

            if(sweepBox.intersects(tile)){
                objColliding.add(obj);
            }
        }

        return objColliding;
    }
    
    private boolean checkCollision(boolean checkOneWayPlatforms, int currentMap){
        Rectangle pBounds = new Rectangle(playerX, playerY, playerSize, playerSize);
        for(GameObject obj : objects){
            if(obj.mapID != currentMap){
                continue;
            }
            if(obj.isTriggered && !obj.triggeredDisplay){
                continue;
            }
            int actualX = obj.currentX;
            int actualY = obj.currentY;
            Rectangle oBounds = new Rectangle(actualX, actualY, obj.width, obj.height);
            if(pBounds.intersects(oBounds)){
                if(obj.type == GameObject.Type.solid){
                    return true;
                }
                if(obj.type != GameObject.Type.solid){
                    if(checkOneWayPlatforms && obj.type == GameObject.Type.oneWay && !isDroppingThrough){
                        if((playerY + playerSize - 1) >= obj.y && (playerY + playerSize - 1) <= obj.y){
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
        
    private void checkTriggerObjects(int checkX, int checkY, int currentMap){
        Rectangle playerBounds = new Rectangle(checkX, checkY, playerSize, playerSize);
        isOnLadder = false;

        for(GameObject obj : objects){
            if(obj.mapID != currentMap || (obj.isTriggered && !obj.triggeredDisplay)) continue;

            int actualX = obj.currentX;
            int actualY = obj.currentY;
            Rectangle triggerBounds = new Rectangle(actualX, actualY, obj.width, obj.height);
            isOnLadder = false;
            if(playerBounds.intersects(triggerBounds)){
                if(obj.type == GameObject.Type.death){
                    triggerDeathSequence(currentMap);
                    return;
                }
                if(obj.type == GameObject.Type.respawn || obj.type == GameObject.Type.spawnPoint){
                    spawnX = actualX;
                    spawnY = actualY;
                    savedCheckpointTriggeredObjects.addAll(sessionTriggeredObjects);
                }
                if(obj.type == GameObject.Type.ladder){
                    isOnLadder = true;
                    dashAble = true;
                    ladderGraceFrames = maxLadderGrace;
                    
                    if(!up && !down){
                        velocityY = 0;
                    }else{
                        velocityY = (up) ? -5 : 5;
                    }
                    isGrounded = false;
                }
                if(obj.type == GameObject.Type.win){
                    MapEditorWindow.win();
                }
                // System.out.println(obj.type+" "+obj.activatesChannel+" "+obj.isTriggered);
                if(obj.type == GameObject.Type.trigger && obj.activatesChannel > 0){
                    if(!obj.isTriggered){
                        obj.isTriggered = true;
                        for(GameObject target : objects){
                            if(target.mapID == currentMap && target.triggeredBy == obj.activatesChannel){
                                target.isTriggered = true;
                                sessionTriggeredObjects.add(target);
                                sessionTriggeredObjects.add(obj);
                            }
                        }
                    }
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
    
    private void applyPlatformCarriagePhysics(int currentMap) {
        Rectangle pBounds = new Rectangle(playerX, playerY, playerSize, playerSize);
        if(velocityY < 0){
            isGrounded = false;
            return;
        }
        
        // Expanded verification field box underneath player feet to detect solid ground rungs
        Rectangle feetBounds = new Rectangle(playerX, playerY + 5, playerSize, playerSize);

        for (GameObject obj : objects) {
            if (obj.mapID != currentMap || obj.move == GameObject.movement.never) continue;
            if (obj.isTriggered && !obj.triggeredDisplay) continue;

            int actualX = obj.currentX;
            int actualY = obj.currentY;
            Rectangle blockBounds = new Rectangle(actualX, actualY, obj.width, obj.height);

            // 1. ELEVATOR TRACKING & HORIZONTAL CARRIAGE (Riding on top smoothly)
            if (feetBounds.intersects(blockBounds) && (obj.type == GameObject.Type.solid || obj.type == GameObject.Type.oneWay)) {
                // Ensure player box bottom layer is cleanly riding on the top deck plane
                if (playerY + playerSize <= actualY + Math.max(5, Math.abs((int)obj.deltaY)) + 4) {
                    
                    // Inject delta shifts instantly into coordinate vectors
                    playerY += obj.deltaY;
                    playerX += obj.deltaX;
                    
                    // Pin feet securely down flush onto the deck to halt gravity jittering cycles
                    
                    if(obj.deltaY <= 0){
                        playerY = actualY - playerSize;
                    }
                    
                    isGrounded = true;
                    velocityY = 0;
                    hasDoubleJumped = false;
                    dashAble = true;
                    continue;
                }
            }

            // 2. SOLID PUSH-BACK ENGINE (Prevents clipping/passing through walls completely)
            Rectangle activeBounds = new Rectangle(playerX, playerY, playerSize, playerSize);
            if (activeBounds.intersects(blockBounds) && obj.type == GameObject.Type.solid) {
                // Calculate overlap depth geometry vectors to resolve intersection cleanly
                int overlapX1 = (actualX + obj.width) - playerX;
                int overlapX2 = (playerX + playerSize) - actualX;
                int overlapY1 = (actualY + obj.height) - playerY;
                int overlapY2 = (playerY + playerSize) - actualY;

                int minOverlapX = Math.min(overlapX1, overlapX2);
                int minOverlapY = Math.min(overlapY1, overlapY2);

                boolean isSideCollision = minOverlapX < minOverlapY;

                if (Math.abs(obj.deltaX) > 0 && playerY + playerSize - 4 > actualY && playerY + 4 < actualY + obj.height) {
                    isSideCollision = true;
                }

                if (isSideCollision) {
                    // Resolve horizontal wall crunch push-outs cleanly using relative centers
                    int playerCenterX = playerX + (playerSize / 2);
                    int blockCenterX = actualX + (obj.width / 2);
                    
                    if (playerCenterX > blockCenterX) {
                        playerX = actualX + obj.width; // Push cleanly right
                    } else {
                        playerX = actualX - playerSize; // Push cleanly left
                    }
                    currentVelocityX = 0;
                } else {
                    // Resolve vertical floor/ceiling crush push-outs
                    if (overlapY1 < overlapY2) {
                        playerY = actualY + obj.height;
                        if (velocityY < 0) velocityY = 1; // Direct ceiling strike bounce back weight
                    } else {
                        playerY = actualY - playerSize;
                        isGrounded = true;
                        velocityY = 0;
                    }
                }
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

    public int getPlayerSize(){ return playerSize;}
    public int getPlayerX() { return playerX; }
    public int getPlayerY() { return playerY; }
    public double getVelocityY() { return velocityY; }
    public void setVelocityY(double val) { this.velocityY = val; }
    public boolean isGrounded() { return isGrounded; }
    public int getFps() { return fps; }
    public int getDeathCount(){ return this.deathCount; }

    public void resetTriggerTrackingState() {
        this.savedCheckpointTriggeredObjects.clear();
        this.sessionTriggeredObjects.clear();
    }
}