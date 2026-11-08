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
            // System.out.println(velocityY);
            velocityY = initialJumpForce;
            // System.out.println(velocityY);
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
                    // System.out.println(obj.deltaY);
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
        // System.out.println(moveY);

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

                    int objDeltaY = (int) Math.round(obj.deltaY);
                    int scanDepth = Math.max(8, Math.abs(objDeltaY) + 6);

                    Rectangle dynamicFeetSensor = new Rectangle((int)playerX, (int)playerY - 2, playerSize, scanDepth + 2);
                    Rectangle platBounds = new Rectangle(obj.currentX, obj.currentY - obj.height, obj.width, obj.height);
                    
                    if (dynamicFeetSensor.intersects(platBounds)) {
                        // System.out.println("Colliding");
                        // System.out.println(playerY+" "+(objTop - obj.deltaY));
                        if (playerY == (objTop - obj.deltaY)) {
                            if(bestPlatformMatch != null){
                            // System.out.println(obj.currentY+" "+bestPlatformMatch.currentY);
                            }
                            if (bestPlatformMatch == null || obj.deltaY < bestPlatformMatch.deltaY || (obj.deltaY == bestPlatformMatch.deltaY && Math.abs(obj.deltaX) > Math.abs(bestPlatformMatch.deltaX))) {
                                bestPlatformMatch = obj;
                                // System.out.println("diff");
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
            // System.out.println("null");
            platformCarriageX = platformRidden.deltaX;
            platformCarriageY = platformRidden.deltaY;
            standingOnPlatform = true;
            isGrounded = true;
            // System.out.println(platformRidden+" "+Math.random());
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
            if(platformRidden != null){
                resolveSafePlatformPush(platformRidden, currentMap);
                if(this.isDead) return;
            }
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
        if(totalMoveX != 0 || platformCarriageX != 0 || true){
            double targetX = playerX + totalMoveX + platformCarriageX;
            if(targetX < 0) {
                targetX = 0;
                currentVelocityX = 0;
            }

            ArrayList<GameObject> hitsX = checkCornerCollision((int)playerX, (int)playerY, (int)targetX, (int)playerY, currentMap);
            final double currentPX = playerX;
            hitsX.sort((a, b) -> Integer.compare(Math.abs(a.currentX - (int)currentPX), Math.abs(b.currentX - (int)currentPX)));
            if(!hitsX.isEmpty()){
            // System.out.println(hitsX+" "+Math.random());
            }

            boolean collidedX = false;
            for (GameObject obj : hitsX) {
                if (obj.type == GameObject.Type.solid) {

                    if(standingOnPlatform && obj == platformRidden) {/*System.out.println("Continuing");*/ continue;}

                    int objLeft = obj.currentX;
                    int objRight = obj.currentX + obj.width;
                    int objTop = obj.currentY - obj.height;
                    int objBottom = obj.currentY;

                    boolean verticalOverlap = (playerY > objTop) && ((playerY - playerSize) < objBottom);
                    
                    if(verticalOverlap){
                        int distLeft = (int) (objLeft - (playerX + playerSize));
                        int distRight = (int) (objRight - playerX);
                        if(Math.abs(distLeft) < Math.abs(distRight)){
                            playerX = objLeft - playerSize;
                        }else{
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
        if(moveY != 0 || platformCarriageY != 0 || true) {
            // System.out.println(platformCarriageY);s
            int totalMoveY = (int)(moveY + platformCarriageY);
            // System.out.println(totalMoveY+" "+Math.random());
            double targetY = playerY + totalMoveY;
            if(targetY - playerSize < 0) {
                targetY = playerSize;
                jumpCounter = maxJumpHoldTime;
                isJumping = false;
                velocityY = 1;
            }
            // System.out.println("Checking for objects");

            ArrayList<GameObject> hitsY = checkCornerCollision((int)playerX, (int)playerY, (int)playerX, (int)targetY, currentMap);
            // System.out.println(hitsY);
            
            if(moveY >= 0 && standingOnPlatform && platformRidden != null && !hitsY.contains(platformRidden)){
                hitsY.add(platformRidden);
            }
            final double currentPY = playerY;
            hitsY.sort((a, b) -> Integer.compare(Math.abs((a.currentY - a.height) - (int)currentPY), Math.abs((b.currentY - b.height) - (int)currentPY)));

            if(!hitsY.isEmpty()){
            // System.out.println(hitsY+" hitsY"+Math.random());
            }
            boolean collidedY = false;
            for (GameObject obj : hitsY) {
                if(obj == platformRidden && moveY < 0) continue;
                if(standingOnPlatform && obj == platformRidden && moveY == 0) continue;
                // System.out.println(obj);


                int objTop = obj.currentY - obj.height;
                int objBottom = obj.currentY;

                if (obj.type == GameObject.Type.solid) {
                    if (moveY >= 0 || (obj == platformRidden && playerY <= objTop + 6)) {
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
        int playerLeft = (int) Math.round(playerX);
        int playerRight = (int) Math.round(playerX + playerSize);
        int playerBottom = (int) Math.round(playerY);
        int playerTop = (int) Math.round(playerY - playerSize);

        int prevLeft = (int) Math.round(movingPlatform.currentX - movingPlatform.deltaX);
        int left = (int) Math.round(movingPlatform.currentX);
        int prevRight = (int) Math.round(movingPlatform.currentX + movingPlatform.width - movingPlatform.deltaX);
        int right = (int) Math.round(movingPlatform.currentX + movingPlatform.width);
        
        int prevTop = (int) Math.round(movingPlatform.currentY - movingPlatform.height - movingPlatform.deltaY);
        int top = (int) Math.round(movingPlatform.currentY - movingPlatform.height);
        int prevBottom = (int) Math.round(movingPlatform.currentY - movingPlatform.deltaY);
        int bottom = (int) Math.round(movingPlatform.currentY);

        if(movingPlatform == platformRidden){
            if(movingPlatform.deltaY < 0){
                for(GameObject obj : objects){
                    if(obj.mapID != currentMap || obj.type != GameObject.Type.solid || movingPlatform == obj || (obj.isTriggered && !obj.triggeredDisplay)) continue;
                    int objLeft = (int) (obj.currentX);
                    int objRight = (int) (obj.currentX + obj.width);
                    int objBottomPrev = (int) (obj.currentY - obj.deltaY);
                    int objBottom = (int) (obj.currentY);
                    if(playerRight > objLeft && playerLeft < objRight){
                        if((prevTop - objBottomPrev >= playerSize) && (top - objBottom < playerSize)){
                            triggerDeathSequence(currentMap);
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
                    System.out.println(playerBottom+" "+top+" "+playerTop+" "+bottom+" "+playerBottom+" "+topObj+" "+playerTop+" "+bottomObj);
                    System.out.println((prevLeftObj - prevRight)+" "+playerSize+" "+(leftObj - right) +" "+playerSize);
                    System.out.println(playerLeft+" "+prevRight+" "+playerRight+" "+leftObj);
                    if(playerBottom > top && playerTop < bottom && playerBottom > topObj && playerTop < bottomObj){
                        if ((prevLeftObj - prevRight >= playerSize) && (leftObj - right < playerSize)) {
                            if (playerLeft >= prevRight && playerRight <= leftObj) {
                                triggerDeathSequence(currentMap);
                                break;
                            }
                        }
                    }
                }else if(movingPlatform.deltaX < 0){
                    System.out.println(playerBottom+" "+top+" "+playerTop+" "+bottom+" "+playerBottom+" "+topObj+" "+playerTop+" "+bottomObj);
                    System.out.println((prevLeft - rightObj)+" "+playerSize+" "+(left - rightObj) +" "+playerSize);
                    System.out.println(playerLeft+" "+rightObj+" "+playerRight+" "+left);
                    System.out.println(isGrounded);
                    if(playerBottom > top && playerTop < bottom && playerBottom > topObj && playerTop < bottomObj){
                        if ((prevLeft - prevRightObj >= playerSize) && (left - rightObj < playerSize)) {
                            if (playerLeft >= rightObj && playerRight <= prevLeft) {
                                triggerDeathSequence(currentMap);
                                break;
                            }
                        }
                    }
                }
                if(movingPlatform.deltaY > 0){
                    if (playerRight > left && playerLeft < right && playerRight > leftObj && playerLeft < rightObj) {
                        if ((prevTopObj - prevBottom >= playerSize) && (topObj - bottom < playerSize)) {
                            triggerDeathSequence(currentMap);
                            return;
                        }
                    }
                }
            }
        }
    }
    
    private void resolveSafePlatformPush3(GameObject movingPlatform, int currentMap){
        if(movingPlatform == null) return;

        double dX = movingPlatform.deltaX;
        double dY = movingPlatform.deltaX;

        double rideDeltaX = (platformRidden != null) ? platformRidden.deltaX : 0;
        double rideDeltaY = (platformRidden != null) ? platformRidden.deltaY : 0;

        int predictedX = (int) (playerX + rideDeltaX);
        int predictedY = (int) (playerY + rideDeltaY);

        if(platformRidden != movingPlatform){

            int platLeft = movingPlatform.currentX;
            int platRight = movingPlatform.currentX + movingPlatform.width;
            int platBottom = movingPlatform.currentY;
            int platTop = movingPlatform.currentY - movingPlatform.height;

            Rectangle currentPlayerBox = new Rectangle(predictedX, predictedY - playerSize, playerSize, playerSize);
            Rectangle nextPlatBox = new Rectangle(platLeft, platTop, movingPlatform.width, movingPlatform.height);

            if(nextPlatBox.intersects(currentPlayerBox)){
                if(dX > 0){
                    int overlapX = platRight - predictedX;
                    if(overlapX > 0) predictedX += overlapX;
                }else if(dX < 0){
                    int overlapX = (predictedX + playerSize) - platLeft;
                    if(overlapX > 0) predictedX -= overlapX;
                }
                if(dY > 0){
                    int overlapY = platBottom - (predictedY - playerSize);
                    if(overlapY > 0) predictedY += overlapY;
                }else if(dY < 0){
                    int overlapY = predictedY - platTop;
                    if(overlapY > 0) predictedY -= overlapY;
                }
            }
        }

        Rectangle pBounds = new Rectangle(predictedX, predictedY - playerSize, playerSize, playerSize);

        for(GameObject obj : objects){
            if(obj == movingPlatform || obj.mapID != currentMap || obj.type != GameObject.Type.solid) continue;
            if(obj.isTriggered && !obj.triggeredDisplay) continue;
            if(obj == platformRidden) continue;

            int blockLeft = obj.currentX;
            int blockRight = obj.currentX + obj.width;
            int blockBottom = obj.currentY;
            int blockTop = obj.currentY - obj.height;

            Rectangle block = new Rectangle(blockLeft, blockTop, obj.width, obj.height);

            if(pBounds.intersects(block)){
                boolean pinchedHorizontal = false;
                boolean pinchedVertical = false;

                boolean isSideWall = (blockTop < pBounds.y + pBounds.height) && (blockBottom > pBounds.y);
                boolean isVerticalWall = (blockLeft < pBounds.x + pBounds.width) && (blockRight > pBounds.x);

                if(isSideWall && movingPlatform != platformRidden && dX != 0){
                    int platRight = movingPlatform.currentX + movingPlatform.width;
                    int platLeft = movingPlatform.currentX;

                    if(dX > 0 && platRight >= blockLeft && blockLeft <= pBounds.x + pBounds.width){
                        Rectangle rightWallSensor = new Rectangle(pBounds.x + pBounds.width - 2, pBounds.y, 4, pBounds.height);
                        for(GameObject wall : objects){
                            if (wall != movingPlatform && wall != obj && wall.mapID == currentMap && wall.type == GameObject.Type.solid) {
                                if (rightWallSensor.intersects(new Rectangle(wall.currentX, wall.currentY - wall.height, wall.width, wall.height))) {
                                    System.out.println("Pinched Right Side");
                                    pinchedHorizontal = true;
                                    break;
                                }
                            }
                        }
                    }
                    else if(dX < 0 && platLeft <= blockRight && blockRight >= pBounds.x){
                        Rectangle leftWallSensor = new Rectangle(pBounds.x - 2, pBounds.y, 4, pBounds.height);
                        for(GameObject wall : objects){
                            if (wall != movingPlatform && wall != obj && wall.mapID == currentMap && wall.type == GameObject.Type.solid) {
                                if(leftWallSensor.intersects(new Rectangle(wall.currentX, wall.currentY - wall.height, wall.width, wall.height))){
                                    System.out.println("Pinched Left side");
                                    pinchedHorizontal = true;
                                    break;
                                }
                            }
                        }
                    }
                }

                if(isVerticalWall){
                    Rectangle ceilingSensor = new Rectangle(pBounds.x, pBounds.y - playerSize, pBounds.width, 6);

                    for(GameObject wall : objects){
                        if(wall != movingPlatform && wall != obj && wall.mapID == currentMap && wall.type == GameObject.Type.solid && (wall.isTriggered && wall.triggeredDisplay || !wall.isTriggered)){
                            Rectangle wallBounds = new Rectangle(wall.currentX, wall.currentY - wall.height, wall.width, wall.height);

                            if(ceilingSensor.intersects(wallBounds)){
                                double wallDeltaY = (wall.move != GameObject.movement.never) ? wall.deltaY : 0;

                                if(wallDeltaY > 0){
                                    System.out.println("falling");
                                    pinchedVertical = true;
                                    break;
                                }
                                if(platformRidden != null && rideDeltaY < 0){
                                    if(rideDeltaY < wallDeltaY){
                                        pinchedVertical = true;
                                        break;
                                    }
                                }
                                double totalUpwardMovement = velocityY + rideDeltaY;
                                if(totalUpwardMovement < 0){
                                    pinchedVertical = true;
                                    break;
                                }
                            }
                        }
                    }
                }
                if(pinchedHorizontal || pinchedVertical){
                    triggerDeathSequence(currentMap);
                    return;
                }
            }
        }
    }

    private void resolveSafePlatformPush2(GameObject movingPlatform, int currentMap){
        if(movingPlatform == platformRidden){
            if(movingPlatform.deltaY == 0) return;
            int newX = (int) (playerX + movingPlatform.deltaX);
            int newY = (int) (playerY + movingPlatform.deltaY);
            Rectangle predictBox = new Rectangle(newX, (newY - playerSize), playerSize, 2);
            Rectangle sideCheckLeft = new Rectangle(newX, (newY - playerSize/2), 2, playerSize/2);
            Rectangle sideCheckRight = new Rectangle(newX + playerSize - 2, newY - playerSize/2, 2, playerSize/2);
            for(GameObject obj : objects){
                if(obj.mapID != currentMap || obj.type != GameObject.Type.solid || movingPlatform == obj || (obj.isTriggered && obj.triggeredDisplay)) continue;
                Rectangle objBox = new Rectangle(obj.currentX, obj.currentY - obj.height, obj.width, obj.height);
                if(objBox.intersects(predictBox) && (!objBox.intersects(sideCheckLeft) || !objBox.intersects(sideCheckRight))){
                    triggerDeathSequence(currentMap);
                    break;
                }
            }
        }else{
            int newX = (int) (playerX + movingPlatform.deltaX);
            int newY = (int) (playerY + movingPlatform.deltaY);
            if(movingPlatform.deltaX > 0){
                Rectangle predictBoxRight = new Rectangle(newX + playerSize - 2, (newY - playerSize), 2, playerSize);
                Rectangle topCheck = new Rectangle(newX, newY - playerSize, playerSize/2, 2);
                Rectangle bottomCheck = new Rectangle(newX, newY - 2, playerSize/2, 2);
                for(GameObject obj : objects){
                    if(obj.mapID != currentMap || obj.type != GameObject.Type.solid || movingPlatform == obj || (obj.isTriggered && obj.triggeredDisplay)) continue;
                    Rectangle objBox = new Rectangle(obj.currentX, obj.currentY - obj.height, obj.width, obj.height);
                    if(objBox.intersects(predictBoxRight) && (!objBox.intersects(topCheck) || !objBox.intersects(bottomCheck))){
                        triggerDeathSequence(currentMap);
                        break;
                    }
                }
            }else if(movingPlatform.deltaX < 0){
                Rectangle predictBoxLeft = new Rectangle(newX, (newY - playerSize), 2, playerSize);
                Rectangle topCheck = new Rectangle(newX + playerSize/2, newY - playerSize, playerSize/2, 2);
                Rectangle bottomCheck = new Rectangle(newX + playerSize/2, newY - 2, playerSize/2, 2);
                for(GameObject obj : objects){
                    if(obj.mapID != currentMap || obj.type != GameObject.Type.solid || movingPlatform == obj || (obj.isTriggered && obj.triggeredDisplay)) continue;
                    Rectangle objBox = new Rectangle(obj.currentX, obj.currentY - obj.height, obj.width, obj.height);
                    if(objBox.intersects(predictBoxLeft) && (!objBox.intersects(topCheck) || !objBox.intersects(bottomCheck))){
                        triggerDeathSequence(currentMap);
                        break;
                    }
                }
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