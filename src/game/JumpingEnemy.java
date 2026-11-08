package src.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;

public class JumpingEnemy extends Enemy{
    private int jumpTimerFrames = 0;
    private int nextJumpInterval;
    public JumpingEnemy(int x, int y, int mapID){
        super(x, y, 32, 32, mapID);
        this.type = "JUMPING";
        this.velX = 3.0;
        resetJumpInterval();
    }
    private void resetJumpInterval(){
        this.nextJumpInterval = (int) (Math.random() * 120) + 60;
        this.jumpTimerFrames = 0;
    }
    @Override
    public void aiTick(ArrayList<GameObject> objects){
        if(this.x <= 0) this.velX = Math.abs(this.velX);

        if(this.isGrounded){
            this.jumpTimerFrames++;
            if(this.jumpTimerFrames >= this.nextJumpInterval){
                this.velY = -15.5;
                this.isGrounded = false;
                resetJumpInterval();
            }
        }
        updatePhysics(objects);
    }
    @Override 
    protected void handleWallBump(){
        this.velX = -this.velX;
    }
    @Override 
    public void draw(Graphics2D g2){
        int renderY = this.y - height;
        g2.setColor(new Color(255, 69, 0));
        g2.fillRect(this.x, renderY, this.width, this.height);
        g2.setColor(Color.BLACK);
        g2.drawRect(this.x, renderY, this.width, this.height);
    }
}