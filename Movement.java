import java.awt.Rectangle;
import java.util.ArrayList;
public class Movement{
    private final int tileSize;
    private final int worldWidth;
    private final int worldHeight;
    private final int playerSpeed;
    private final ArrayList<GameObject> objects;

    private int playerX;
    private int playerY;
    private boolean left, right, up, down;
    
    private double velocityY = 0;
    private final double gravity = .5;
    private final double terminalVelocity = 12;

    private final double initialJumpForce = -6.0;
    private final double holdJumpBoost = -.6;
    private final int maxJumpHoldTime = 32;
    private int jumpCounter = 0;

    private boolean isJumping = false;
    private boolean isGrounded = false;
    private boolean isDroppingThrough = false;
     

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
        this.velocityY = 0;
        this.isGrounded = false;
        this.isJumping = false;
        this.isGrounded = false;
        this.isDroppingThrough = false;
    }

    public void update(int currentMap){
        if(left != right){
            int directionX = left ? -1 : 1;
            for(int i = 0; i < playerSpeed; i++){
                playerX += directionX;
                if(playerX < 0 || playerX > worldWidth - tileSize || checkCollision(false, currentMap)){
                    playerX -= directionX;
                    break;
                }
            }
        }
        if(up && isGrounded){
            velocityY = initialJumpForce;
            isGrounded = false;
            isJumping = true;
            jumpCounter = 0;
        }
        if(up && isJumping){
            if(jumpCounter < maxJumpHoldTime){
                velocityY += holdJumpBoost;
                jumpCounter++;
            }else{
                isJumping = false;
            }
        }
        if(!up){
            isJumping = false;
        }

        if(!isGrounded){
            velocityY += gravity;
            if(velocityY > terminalVelocity){
                velocityY = terminalVelocity;
            }
        }
        if(velocityY != 0){
            int directionY = velocityY > 0 ? 1 : -1;
            boolean falling = (directionY == 1);

            boolean alreadyPurple = isEmbeddedInOneWay(currentMap);

            if(!alreadyPurple){
                isDroppingThrough = false;
            }

            boolean allowPurpleCollsion = falling && !down && !alreadyPurple && !isDroppingThrough;

            int steps = (int) Math.abs(velocityY);

            for(int i = 0; i < steps; i++){
                playerY += directionY;
                if(playerY < 0 || playerY > worldHeight - tileSize || checkCollision(allowPurpleCollsion, currentMap)){
                    playerY -= directionY;
                    if(falling){
                        isGrounded = true;
                    }
                    velocityY = 0;
                    break;
                }
            }
        }
        if(velocityY == 0 && !isDroppingThrough){
            playerY += 1;
            boolean standingOnSolid = checkCollision(false, currentMap);

            boolean standingOnOneWay = checkCollision(!down, currentMap);
            playerY -= 1;

            if(standingOnSolid){
                isGrounded = true;
            }else if(standingOnOneWay){
                if(down){
                    playerY += 2;
                    velocityY = 1;
                    isGrounded = false;
                    isDroppingThrough = true;
                }else{
                    isGrounded = true;
                }
            }else{
                isGrounded = false;
            }
            
        }
    }
    private boolean isOverlappingType(GameObject.Type targetType, int currentMap){
        Rectangle pBounds = new Rectangle(playerX, playerY, tileSize, tileSize);
        for(GameObject obj : objects){
            if(obj.mapID != currentMap){
                continue;
            }
            if(obj.type == targetType){
                if(pBounds.intersects(new Rectangle(obj.x, obj.y, obj.width, obj.height))){
                    return true;
                }
            }
        }
        return false;
    }
    private boolean isEmbeddedInOneWay(int currentMap){
         Rectangle torsoBounds = new Rectangle(playerX, playerY, tileSize, tileSize - 8);

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
    public void setLeft(boolean pressed) {this.left = pressed;}
    public void setRight(boolean pressed) {this.right = pressed;}
    public void setUp(boolean pressed) {this.up = pressed;}
    public void setDown(boolean pressed) {this.down = pressed;}

    public int getPlayerX(){ return playerX; }
    public int getPlayerY(){ return playerY; }
}