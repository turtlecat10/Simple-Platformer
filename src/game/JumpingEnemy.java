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
        if(x <= 0) velX = Math.abs(velX);

        if(isGrounded){
            jumpTimerFrames++;
            if(jumpTimerFrames >= nextJumpInterval){
                this.velY = -15.5;
                this.isGrounded = false;
                resetJumpInterval();
            }
        }
    }
    @Override 
    protected void handleWallBump(){
        velX = -velX;
    }
    @Override 
    public void draw(Graphics2D g2){
        g2.setColor(new Color(255, 69, 0));
        g2.fillRect(x, y, width, height);
        g2.setColor(Color.BLACK);
        g2.drawRect(x, y, width, height);
    }
}
