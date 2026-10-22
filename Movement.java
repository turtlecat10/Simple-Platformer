import java.awt.Rectangle;
import java.util.ArrayList;
public class Movement{
    private final int tileSize;
    private final int worldWidth;
    private final int worldHeight;
    private final int playerSpeed;
    private final ArrayList<GameObject> objects;

    private int spawnX;
    private int spawnY;

    private int playerX;
    private int playerY;
    private boolean left, right, up, down;
    
    private double velocityY = 0;
    private double gravityY = 0;
    private final double gravity = .1;
    private final double terminalVelocity = 12;

    private final double initialJumpForce = -10.0;
    private final double holdJumpBoost = -1;
    private final int maxJumpHoldTime = 32;
    private int jumpCounter = 0;

    private boolean stoppedJumping = true;
    private boolean doubleJump = false;
    private boolean isJumping = false;
    private boolean isGrounded = false;
    private boolean isDroppingThrough = false;
    
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
    public void respawnPlayer(int gridX, int gridY){
        this.playerX = gridX * tileSize;
        this.playerY = gridY * tileSize;
        spawnX = gridX * tileSize;
        spawnY = gridY * tileSize;
        this.velocityY = 0;
        this.isGrounded = false;
        this.isJumping = false;
        this.isGrounded = false;
        this.isDroppingThrough = false;
    }
    public void update(int currentMap){
        totalframes++;
        //frames per second
        if(System.nanoTime() - lastFrameCheck >= 1_000_000_000L){
            fps = totalframes;
            totalframes = 0;
            lastFrameCheck = System.nanoTime();
            System.out.println("FPS is "+fps);
        }
        move(currentMap);
        //checking if on the ground
        if(isGrounded){
            gravityY = 0;
        }
        //jumping
        jump();
        //applying gravity
        if(!isGrounded){
            gravityY += gravity;
            velocityY += gravityY;
            if(velocityY > terminalVelocity){
                velocityY = terminalVelocity;
            }
        }
        //when falling
        }
    private ArrayList<GameObject> isOverlapping(int currentMap){
        Rectangle pBounds = new Rectangle(playerX, playerY, tileSize, tileSize);
        ArrayList<GameObject> objColliding = new ArrayList<>(); 
        int i = 0;
        for(GameObject obj : objects){
            if(obj.mapID != currentMap){
                continue;
            }
            if(pBounds.intersects(new Rectangle(obj.x, obj.y, obj.width, obj.height))){
                objColliding.add(obj);
            }
            i++;
        }
        return objColliding;
    }
    private boolean isEmbeddedInOneWay(int currentMap){
         Rectangle torsoBounds = new Rectangle(playerX, playerY, tileSize, tileSize);

        for (GameObject obj : objects) {
            if (obj.mapID != currentMap || obj.type != GameObject.Type.oneWay) continue;

            if (torsoBounds.intersects(new Rectangle(obj.x, obj.y, obj.width, obj.height))) {
                return true; 
            }
        }
        return false;
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
    private void jump(){
        //if jumping
        
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
                velocityY += holdJumpBoost;
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
        // System.out.println(up+" "+!isJumping+" "+!isGrounded+" "+doubleJump);
        if(up && !isJumping && !isGrounded && doubleJump && stoppedJumping){
            System.out.println("what");
            doubleJump = false;
            velocityY = initialJumpForce;
            isJumping = true;
            jumpCounter = 0;
            gravityY = 0;
        }
    }
    private void move(int currentMap){

        //synchronized movement
        int directionX = 0;
        int directionY = 0;
        if(left != right){
            directionX = left ? -1 : 1;
        }
        if(velocityY != 0){
            directionY = velocityY > 0 ? 1 : -1;
        }

        boolean falling = (directionY == 1);
        boolean alreadyPurple = isEmbeddedInOneWay(currentMap);
        if(!alreadyPurple){
            isDroppingThrough = false;
        }
        boolean allowPurpleCollsion = falling && !down && !alreadyPurple && !isDroppingThrough;

        int speedX = Math.abs(playerSpeed);
        int speedY = Math.abs((int) velocityY);
        int steps = Math.max(speedX, speedY);
        for(int i = 0; i < steps; i++){
            for(int X = 0; X <= 1; X++){
                if(X % 2 == 0){
                    if(speedX != 0){
                        speedX--;
                        playerX += directionX;
                    }else{
                        continue;
                    }
                }else{
                    if(speedY != 0){
                        speedY--;
                        playerY += directionY;
                    }else{
                        continue;
                    }
                }

                if(playerX < 0 || playerX > worldWidth - tileSize){
                    playerX -= directionX;
                    continue;
                }
                if(playerY < 0 || playerY > worldHeight - tileSize){
                    playerY -= directionY;
                    if(playerY > worldHeight - tileSize){
                        respawnPlayer(spawnX / tileSize, spawnY / tileSize);
                        return;
                    }
                    continue;
                }
                ArrayList<GameObject> objectsColliding = isOverlapping(currentMap);
                boolean collisionResolve = false;
                for(int l = 0; l < objectsColliding.size(); l++){
                    switch (objectsColliding.get(l).type){
                        case solid:
                            if(X % 2 == 0){
                                playerX -= directionX;
                            }else{
                                playerY -= directionY;
                                if(directionY == 1){
                                    isGrounded = true;
                                    velocityY = 0;
                                }else if(directionY == -1){
                                    velocityY = 0;
                                }
                            }
                            collisionResolve = true;
                            break;
                        case oneWay:
                            if(allowPurpleCollsion){
                                playerY -= directionY;
                                isGrounded = true;
                                velocityY = 0;
                                collisionResolve = true;
                            }
                            break;
                        case respawn:
                            spawnX = objectsColliding.get(l).x;
                            spawnY = objectsColliding.get(l).y;
                            break;
                        case death:
                            respawnPlayer(spawnX / tileSize, spawnY / tileSize);
                            return;
                        //don't matter
                        case background:
                            break;
                        default:
                            break;
                    }
                    if(collisionResolve){
                        break;
                    }
                }
            }
        }


        //for checking if standing on something
        if(velocityY == 0 && !isDroppingThrough){
            playerY += 1;

            boolean standingOnSolid = checkCollision(false, currentMap);
            
            boolean standingOnOneWay = checkCollision(true, currentMap);
            playerY -= 1;

            if(standingOnSolid){
                isGrounded = true;
                doubleJump = true;
            }else if(standingOnOneWay){
                if(down){
                    playerY += 2;
                    velocityY = 1;
                    isGrounded = false;
                    isDroppingThrough = true;
                }else{
                    isGrounded = true;
                    doubleJump = true;
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

    public int getPlayerX(){ return playerX; }
    public int getPlayerY(){ return playerY; }
}