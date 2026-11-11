package src.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;

public class JumpingEnemy extends Enemy{
    private int jumpTimerFrames = 0;
    private int nextJumpInterval;
    private double jumpHeight = -15.5;
    private int intervalMax = 180;
    private int intervalMin = 60;
    public JumpingEnemy(int x, int y, int width, int height, int mapID, double velX){
        super(x, y, width, height, mapID, velX);
        this.type = "JUMPING";
        if(velX != 0){
            this.velX = velX;
        }else{
            this.velX = 3.0;
        }
        resetJumpInterval();
    }
    private void resetJumpInterval(){
        this.nextJumpInterval = (int) (Math.random() * (this.intervalMax-this.intervalMin)) + this.intervalMin;
        this.jumpTimerFrames = 0;
    }
    @Override
    public void aiTick(ArrayList<GameObject> objects){
        if(this.x <= 0) this.velX = Math.abs(this.velX);

        if(this.isGrounded){
            this.jumpTimerFrames++;
            if(this.jumpTimerFrames >= this.nextJumpInterval){
                this.velY = jumpHeight;
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

    public double getJumpHeight(){return this.jumpHeight;}
    public void setJumpHeight(double height){this.jumpHeight = height;}
    public int getIntervalMax(){ return this.intervalMax;}
    public void setIntervalMax(int max){this.intervalMax = max;}
    public int getIntervalMin(){return this.intervalMin;}
    public void setIntervalMin(int min){this.intervalMin = min;}
}