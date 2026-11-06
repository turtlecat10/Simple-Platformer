package src.game;
import java.awt.Rectangle;
import java.util.ArrayList;

public class Movement {
    private final int tileSize;
    private final int worldWidth;
    private final int worldHeight;
    private final int playerSpeed;
    private final ArrayList<GameObject> objects;
    private final ArrayList<Enemy> enemies = new ArrayList<>();
    private final int playerSize;
    private final java.util.HashSet<GameObject> savedCheckpointTriggeredObjects = new java.util.HashSet<>();
    private final java.util.HashSet<GameObject> sessionTriggeredObjects = new java.util.HashSet<>();

    public GameObject platformRidden = null;


    private double platformCarriageX = 0;
    private double platformCarriageY = 0;
    // Tracking position (bottom-left corner)
    private int spawnX;
    private int spawnY;
    private double playerX;
    private double playerY;

    // Inputs 
    private boolean left, right, up, down, zoom, dash, reset;

    private double velocityX = 0;
    private double velocityY = 0;
    private final double terminalVelocity = 20;
    private double currentVelocityX = 0.0;

    // Dashing
    private final int dashDuration = 10;
    private final int dashCooldown = 45;
    private int dashCounter = 0;
    private int dashCooldownCounter = 0;
    private boolean isDashing = false;
    private int dashDirectionX = 0;
    private int dashDirectionY = 0;
    private boolean dashAble = false;

    // Jumping
    private final double initialJumpForce = -7.2;
    private final double holdJumpBoost = -0.9;
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
    
    // FPS Tracking
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
        this.playerSize = (int) (tileSize * 0.75);

        this.playerX = startGridX * tileSize;
        this.playerY = startGridY * tileSize;
    }
    
    public void update(int currentMap){
        totalframes++;
        if(System.nanoTime() - lastFrameCheck >= 1_000_000_000L){
            fps = totalframes;
            totalframes = 0;
            lastFrameCheck = System.nanoTime();
        }

        if(dashCooldownCounter > 0){
            dashCooldownCounter--;
        }

        if(!isOnLadder && ladderGraceFrames > 0){
            ladderGraceFrames--;
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
                MapEditorWindow.instance.gameCanvas.spawnDeathExplosion((int)this.playerX, (int)this.playerY - playerSize);
            }
            for(Enemy enemy : enemies){
                if(enemy.mapID == currentMap){
                    enemy.x = enemy.startX;
                    enemy.y = enemy.startY;
                    enemy.velX = enemy.type.equals("PATROL") ? 1.5 : 1.0;
                    enemy.velY = 0;
                    enemy.isGrounded = false;
                }
            }
        }
    }
    
    private void jump(){
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
        if(up && isJumping){
            if(jumpCounter < maxJumpHoldTime){
                if(hasDoubleJumped){
                    velocityY += holdJumpBoost * 0.9;
                }else{
                    velocityY += holdJumpBoost;
                }
                jumpCounter++;
            }else{
                isJumping = false;
            }
        }
        if(!up){
            stoppedJumping = true;
            isJumping = false;
            jumpCounter = 0;
        }
        if(up && !isJumping && !isGrounded && !hasDoubleJumped && stoppedJumping){
            hasDoubleJumped = true;
            velocityY = initialJumpForce * 0.75;
            isJumping = true;
            jumpCounter = 0;
        }
    }
    
    private void move(int currentMap){
        if(isGrounded){
            hasDoubleJumped = false;
        }

        // --- 1. UPDATE MOVING PLATFORMS *BEFORE* COLLISION CHECKS ---
        if(objects != null && !this.isDead){
            for(GameObject obj : objects){
                if(obj.mapID == currentMap){
                    obj.updateMovement((int)playerX, (int)playerY, playerSize);           
                }
            }
        }

        // --- DASH INPUT TRIGGER ---
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
            if(currentDirY != 0){
                dashAble = false;
                isDashing = true;
                dashDirectionY = currentDirY;
                dashCounter = 0;
                dashCooldownCounter = dashCooldown;
            }
        }

        // --- GRAVITY & JUMP PHYSICS ---
        if(!isDashing){
            if(!isGrounded && !isOnLadder){
                if(velocityY >= 0){
                    double velocityRatio = velocityY / terminalVelocity;
                    if(velocityRatio > 1.0) velocityRatio = 1.0;
                    double speedDampeningMultiplier = 1.0 - velocityRatio;
                    if(speedDampeningMultiplier < 0.05) speedDampeningMultiplier = 0.05;
                    velocityY += 0.8 * speedDampeningMultiplier;
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
                        velocityX = left ? -4.5 : 4.5;
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
            int dashSpeed = 16;
            currentVelocityX = dashDirectionX * dashSpeed;
            if(dashDirectionY != 0){
                velocityY = dashDirectionY * (dashSpeed * 0.6);
            }
        }

        // --- HORIZONTAL SPEED & DIRECTION ---
        int directionX = 0;
        if(left != right){
            directionX = left ? -1 : 1;
        }

        int moveX = (int) Math.round(velocityX);
        int moveY = (int) Math.round(velocityY);

        if(isDashing){
            moveX = dashDirectionX * 16;
            moveY = dashDirectionY * 16;
            velocityX = moveX;
        }else{
            double speedMult = zoom ? 1.5 : 1.0;
            double targetTopSpeed = Math.abs(playerSpeed) * directionX * speedMult;
            double accelRate = isGrounded ? 0.8 : 0.6;
            double frictionRate = isGrounded ? 0.75 : 0.92;

            if(directionX != 0){
                if(directionX > 0){
                    velocityX += accelRate;
                    if(velocityX > targetTopSpeed) velocityX = targetTopSpeed;
                }else{
                    velocityX -= accelRate;
                    if(velocityX < targetTopSpeed) velocityX = targetTopSpeed;
                }
            }else{
                velocityX *= frictionRate;
                if(Math.abs(velocityX) < 0.1){
                    velocityX = 0;
                }
            }
            moveX = (int) Math.round(velocityX);
        }

        
        // --- PLATFORM SELECTION & RIDING ---
        GameObject bestPlatformMatch = null;
        for (GameObject obj : objects) {
            if (obj.mapID == currentMap && obj.move != GameObject.movement.never && !(obj.isTriggered && !obj.triggeredDisplay)) {
                if (obj.type == GameObject.Type.solid || obj.type == GameObject.Type.oneWay) {
                    int objTop = obj.currentY - obj.height;

                    Rectangle dynamicFeetSensor = new Rectangle((int)playerX, (int)playerY, playerSize, (int)obj.deltaY+1);
                    Rectangle platBounds = new Rectangle(obj.currentX, objTop, obj.width, obj.height);
                    
                    if (dynamicFeetSensor.intersects(platBounds)) {
                        if (playerY <= objTop + 4) {
                            if (bestPlatformMatch == null || obj.currentY < bestPlatformMatch.currentY) {
                                bestPlatformMatch = obj;
                            }
                        }
                    }
                }
            }
        }

        platformRidden = bestPlatformMatch;
        platformCarriageX = 0;
        platformCarriageY = 0;
        boolean standingOnPlatform = false;

        if(platformRidden != null){
            platformCarriageX += platformRidden.deltaX;
            platformCarriageY += platformRidden.deltaY;
            standingOnPlatform = true;
            isGrounded = true;
            System.out.println(platformRidden+" "+Math.random());
        }

        // --- ONE-WAY PLATFORM DROP-THROUGH ---
        boolean falling = (moveY > 0);
        Rectangle bodyBounds = new Rectangle((int)playerX, (int)playerY - playerSize, playerSize, playerSize);
        boolean embeddedInOneWay = false;
        for(GameObject obj : objects){
            if(obj.mapID == currentMap && obj.type == GameObject.Type.oneWay){
                Rectangle oneWayRect = new Rectangle(obj.currentX, obj.currentY - obj.height, obj.width, obj.height);
                if(bodyBounds.intersects(oneWayRect)){
                    embeddedInOneWay = true;
                    break;
                }
            }
        }
        if(!embeddedInOneWay){
            isDroppingThrough = false;
        }
        boolean allowOneWayCollision = falling && !down && !embeddedInOneWay && !isDroppingThrough;
        
        // --- MOVING PLATFORM CRUSH & PUSH RESOLUTION ---
        if(objects != null && !this.isDead){
            for(GameObject obj : objects){
                if(obj.mapID != currentMap || obj.type != GameObject.Type.solid || obj.move == GameObject.movement.never || (obj.isTriggered && !obj.triggeredDisplay)) continue;
                if(obj == platformRidden) continue;

                Rectangle pBounds = new Rectangle((int)playerX, (int)playerY - playerSize, playerSize, playerSize);
                int objTop = obj.currentY - obj.height;
                Rectangle platBounds = new Rectangle(obj.currentX, objTop, obj.width, obj.height);

                if(platBounds.intersects(pBounds)){
                    resolveSafePlatformPush(obj, currentMap);
                    if(this.isDead) return;
                }
            }
        }

        
        // --- HORIZONTAL MOVEMENT & PLATFORM CARRIAGE ---
        int totalMoveX = (int) (moveX);
        if(totalMoveX != 0 || platformCarriageX != 0){
            double targetX = playerX + totalMoveX + platformCarriageX;
            if(targetX < 0) {
                targetX = 0;
                currentVelocityX = 0;
            }

            ArrayList<GameObject> hitsX = checkCornerCollision((int)playerX, (int)playerY, (int)targetX, (int)playerY, currentMap);
            final double currentPX = playerX;
            hitsX.sort((a, b) -> Integer.compare(Math.abs(a.currentX - (int)currentPX), Math.abs(b.currentX - (int)currentPX)));

            boolean collidedX = false;
            for (GameObject obj : hitsX) {
                if (obj.type == GameObject.Type.solid) {
                    if(standingOnPlatform && obj == platformRidden) continue;

                    int objLeft = obj.currentX;
                    int objRight = obj.currentX + obj.width;
                    int objTop = obj.currentY - obj.height;
                    int objBottom = obj.currentY;

                    boolean verticalOverlap = (playerY > objTop) && ((playerY - playerSize) < objBottom);
                    if(verticalOverlap){
                        if (totalMoveX > 0) {
                            playerX = objLeft - playerSize;
                        } else {
                            playerX = objRight;
                        }
                        collidedX = true;
                        currentVelocityX = 0;
                        break;
                    }
                }
            }

            if(!collidedX){
                playerX = targetX;
            }
        }

        // --- VERTICAL MOVEMENT & PLATFORM CARRIAGE ---
        if(standingOnPlatform && moveY == 0){
            playerY = platformRidden.currentY - platformRidden.height;
            velocityY = 0;
            isGrounded = true;
            dashAble = true;
            hasDoubleJumped = false;
        } else {
            int totalMoveY = (int)(moveY + platformCarriageY);
            double targetY = playerY + totalMoveY;
            if(targetY - playerSize < 0) {
                targetY = playerSize;
                jumpCounter = maxJumpHoldTime;
                isJumping = false;
                velocityY = 1;
            }

            ArrayList<GameObject> hitsY = checkCornerCollision((int)playerX, (int)playerY, (int)playerX, (int)targetY, currentMap);
            if(standingOnPlatform && platformRidden != null && !hitsY.contains(platformRidden)){
                hitsY.add(platformRidden);
            }
            final double currentPY = playerY;
            hitsY.sort((a, b) -> Integer.compare(Math.abs((a.currentY - a.height) - (int)currentPY), Math.abs((b.currentY - b.height) - (int)currentPY)));

            boolean collidedY = false;
            for (GameObject obj : hitsY) {
                if(standingOnPlatform && obj == platformRidden && platformCarriageY >= 0) continue;

                int objTop = obj.currentY - obj.height;
                int objBottom = obj.currentY;

                if (obj.type == GameObject.Type.solid) {
                    if (totalMoveY >= 0) {
                        playerY = objTop;
                        isGrounded = true;
                        dashAble = true;
                        hasDoubleJumped = false;         
                    } else {
                        playerY = objBottom + playerSize;
                        isJumping = false; 
                    }
                    velocityY = 0;
                    collidedY = true;
                    break;
                }

                if (obj.type == GameObject.Type.oneWay && allowOneWayCollision) {
                    if (playerY <= objTop && (playerY + totalMoveY) >= objTop) { 
                        playerY = objTop;
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
                if (targetY > worldHeight) {
                    respawnPlayer(spawnX, spawnY);
                    return;
                }
                playerY = targetY;
                if(!standingOnPlatform && platformCarriageY == 0){
                    isGrounded = false;
                }
            }
        }
        checkTriggerObjects((int)playerX, (int)playerY, currentMap);
        checkGroundStatus(currentMap);
    }

    private void resolveSafePlatformPush(GameObject movingPlatform, int currentMap){
        int platLeft = movingPlatform.currentX;
        int platRight = movingPlatform.currentX + movingPlatform.width;
        int platBottom = movingPlatform.currentY;
        int platTop = movingPlatform.currentY - movingPlatform.height;

        double dX = movingPlatform.deltaX;
        double dY = movingPlatform.deltaY;

        // Fetch the carriage deltas of the platform you are currently riding on
        double rideDeltaX = (platformRidden != null) ? platformRidden.deltaX : 0;
        double rideDeltaY = (platformRidden != null) ? platformRidden.deltaY : 0;

        for(GameObject obj : objects){
            if(obj == movingPlatform || obj.mapID != currentMap || obj.type != GameObject.Type.solid) continue;
            if(obj.isTriggered && !obj.triggeredDisplay) continue;
            if(obj == platformRidden) continue;
            
            // --- PREDICTIVE COMBING BOUNDING BOX ---
            int predictedX = (int)(playerX + dX + rideDeltaX);
            int predictedY = (int)(playerY + dY + rideDeltaY);
            
            Rectangle pBounds = new Rectangle(predictedX, predictedY - playerSize, playerSize, playerSize);
            
            int blockLeft = obj.currentX;
            int blockRight = obj.currentX + obj.width;
            int blockBottom = obj.currentY;
            int blockTop = obj.currentY - obj.height;

            Rectangle block = new Rectangle(blockLeft, blockTop, obj.width, obj.height);

            if(pBounds.intersects(block)){
                boolean pinchedHorizontal = false;
                boolean pinchedVertical = false;
                
                boolean isSideWall = (blockTop < predictedY) && (blockBottom > (predictedY - playerSize));
                boolean isVerticalWall = (blockLeft < predictedX + playerSize) && (blockRight > predictedX);

                // --- HORIZONTAL SIDE CRUNCH CHECK ---
                if (isSideWall && dX != 0) {
                    if (dX > 0 && platRight >= blockLeft && blockLeft <= predictedX + playerSize) {
                        Rectangle rightWallSensor = new Rectangle(predictedX + playerSize, predictedY - playerSize, 2, playerSize);
                        for (GameObject wall : objects) {
                            if (wall != movingPlatform && wall != obj && wall.mapID == currentMap && wall.type == GameObject.Type.solid) {
                                if (rightWallSensor.intersects(new Rectangle(wall.currentX, wall.currentY - wall.height, wall.width, wall.height))) {
                                    pinchedHorizontal = true;
                                    break;
                                }
                            }
                        }
                    }
                    else if (dX < 0 && platLeft <= blockRight && blockRight >= predictedX) {
                        Rectangle leftWallSensor = new Rectangle(predictedX - 2, predictedY - playerSize, 2, playerSize);
                        for (GameObject wall : objects) {
                            if (wall != movingPlatform && wall != obj && wall.mapID == currentMap && wall.type == GameObject.Type.solid) {
                                if (leftWallSensor.intersects(new Rectangle(wall.currentX, wall.currentY - wall.height, wall.width, wall.height))) {
                                    pinchedHorizontal = true;
                                    break;
                                }
                            }
                        }
                    }
                }

                // --- VERTICAL CEILING/FLOOR CRUNCH CHECK ---
                if (isVerticalWall && dY != 0) {
                    if (dY < 0 && (predictedY - playerSize) >= blockTop && (predictedY - playerSize) <= blockBottom) {
                        pinchedVertical = true;
                    }
                    else if (dY > 0 && predictedY <= blockBottom && predictedY >= blockTop) {
                        pinchedVertical = true;
                    }
                }

                if(pinchedHorizontal || pinchedVertical){
                    triggerDeathSequence(currentMap);
                    return;
                }

                // --- PUSHBACK VELOCITY DIRECTION MATH ---
                int pushRight = blockRight - (int)playerX;
                int pushLeft = ((int)playerX + playerSize) - blockLeft;
                int pushDown = blockBottom - ((int)playerY - playerSize);
                int pushUp = (int)playerY - blockTop;

                // FIX: If the platform is moving purely horizontally, enforce a horizontal push out
                // This prevents sub-pixel rounding from picking pushUp/pushDown over side movement.
                boolean resolveAsHorizontal = (Math.abs(dX) > Math.abs(dY));
                if (dX != 0 && dY == 0) resolveAsHorizontal = true;
                if (dY != 0 && dX == 0) resolveAsHorizontal = false;

                if(resolveAsHorizontal){
                    if(pushRight < pushLeft) {
                        playerX = blockRight;
                    } else {
                        playerX = blockLeft - playerSize;
                    }
                    currentVelocityX = 0;
                } else {
                    if(pushDown < pushUp){
                        playerY = (double)blockBottom + playerSize;
                        velocityY = 0;
                    } else {
                        playerY = (double)blockTop;
                        isGrounded = true;
                        velocityY = 0;
                    }
                }
                
                // Sync positions back up securely
                pBounds.setLocation((int)playerX, (int)playerY - playerSize);
                return;
            }
        }
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
        this.playerX = spawnX;
        this.playerY = spawnY;
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
        int minY = Math.min(startY - playerSize, endY - playerSize);
        int maxY = Math.max(startY, endY);
        
        Rectangle sweepBox = new Rectangle(minX, minY, maxX - minX, maxY - minY);

        for(GameObject obj : objects){
            if(obj.mapID != currentMap) continue;
            if(obj.isTriggered && !obj.triggeredDisplay) continue;

            int actualX = obj.currentX;
            int actualTop = obj.currentY - obj.height;

            Rectangle tile = new Rectangle(actualX, actualTop, obj.width, obj.height);

            if(sweepBox.intersects(tile)){
                objColliding.add(obj);
            }
        }

        return objColliding;
    }
    
    private boolean checkCollision(boolean checkOneWayPlatforms, int currentMap){
        Rectangle pBounds = new Rectangle((int)playerX, (int)(playerY - playerSize), playerSize, playerSize);
        for(GameObject obj : objects){
            if(obj.mapID != currentMap) continue;
            if(obj.isTriggered && !obj.triggeredDisplay) continue;

            int actualX = obj.currentX;
            int objTop = obj.currentY - obj.height;
            Rectangle oBounds = new Rectangle(actualX, objTop, obj.width, obj.height);

            if(pBounds.intersects(oBounds)){
                if(obj.type == GameObject.Type.solid){
                    return true;
                }
                if(checkOneWayPlatforms && obj.type == GameObject.Type.oneWay && !isDroppingThrough){
                    if(Math.abs(playerY - objTop) <= 2){
                        return true;
                    }
                }
            }
        }
        return false;
    }
        
    private void checkTriggerObjects(int checkX, int checkY, int currentMap){
        Rectangle playerBounds = new Rectangle(checkX, checkY - playerSize, playerSize, playerSize);
        isOnLadder = false;

        for(GameObject obj : objects){
            if(obj.mapID != currentMap || (obj.isTriggered && !obj.triggeredDisplay)) continue;

            int actualX = obj.currentX;
            int objTop = obj.currentY - obj.height;
            Rectangle triggerBounds = new Rectangle(actualX, objTop, obj.width, obj.height);

            if(playerBounds.intersects(triggerBounds)){
                if(obj.type == GameObject.Type.death){
                    triggerDeathSequence(currentMap);
                    return;
                }
                if(obj.type == GameObject.Type.respawn || obj.type == GameObject.Type.spawnPoint){
                    spawnX = actualX;
                    spawnY = obj.currentY;
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
        
        for(Enemy enemy : enemies){
            if(enemy.mapID != currentMap) continue;
            if(playerBounds.intersects(enemy.getBounds())){
                triggerDeathSequence(currentMap);
                return;
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

    public int getPlayerSize(){ return playerSize;}
    public int getPlayerX() { return (int)playerX; }
    public int getPlayerY() { return (int)playerY; }
    public double getVelocityY() { return velocityY; }
    public void setVelocityY(double val) { this.velocityY = val; }
    public boolean isGrounded() { return isGrounded; }
    public int getFps() { return fps; }
    public int getDeathCount(){ return this.deathCount; }
    public void setDeathCount(int num){ this.deathCount = num;}

    public void resetTriggerTrackingState() {
        this.savedCheckpointTriggeredObjects.clear();
        this.sessionTriggeredObjects.clear();
    }
    public ArrayList<Enemy> getEnemies(){
        return this.enemies;
    }
}