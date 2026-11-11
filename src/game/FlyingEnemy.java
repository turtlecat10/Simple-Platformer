package src.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;

public class FlyingEnemy extends Enemy{

    private double homeX;
    private double homeY;

    private double angle = 0;

    private double xRange = 64.0;
    private double yRange = 32.0;
    private double xSpeed = 0.1;
    private double ySpeed = 0.15;

    private final double returnHomeSpeed = 0.05;

    public FlyingEnemy(int x, int y, int width, int height, int mapID, double velX){
        super(x, y, width, height, mapID, velX);
        this.type = "FLYING";
        this.isGrounded = true;
        this.homeX = x;
        this.homeY = y;
        if(velX != 0){
            this.xSpeed = velX;
            this.ySpeed = velX * 1.5;
        }
        
    }

    @Override 
    public void aiTick(ArrayList<GameObject> objects){
        this.isGrounded = true;

        if(this.homeX != this.startX) this.homeX += (this.startX - this.homeX) * this.returnHomeSpeed;
        if(this.homeY != this.startY) this.homeY += (this.startY - this.homeY) * this.returnHomeSpeed;

        this.angle += 1.0;

        double targetX = this.homeX + Math.sin(this.angle * this.xSpeed) * this.xRange;
        double targetY = this.homeY + Math.cos(this.angle * this.ySpeed) * this.yRange;

        this.velX = (targetX - this.x) * 0.4;
        this.velY = (targetY - this.y) * 0.4;
        updatePhysics(objects);
    }
    @Override 
    protected void handleWallBump(){
        this.angle = -this.angle;

        this.homeX = this.x - Math.sin(this.angle * this.xSpeed) * this.xRange;
        this.homeY = this.y - Math.cos(this.angle * this.ySpeed) * this.yRange;
    }
    @Override 
    public void draw(Graphics2D g2){
        int renderY = y - height;
        g2.setColor(new Color(0, 139, 0));
        g2.fillRect(x, renderY, width, height);
        g2.setColor(Color.BLACK);
        g2.drawRect(x, renderY, width, height);
    }
    public double getAngle() {return this.angle;}
    public double getHomeX(){return this.homeX;}
    public double getHomeY(){ return this.homeY;}
    public double getXRange(){ return this.xRange;}
    public void setXRange(double xRange){ this.xRange = xRange;}
    public double getYRange(){return this.yRange;}
    public void setYRange(double yRange){ this.yRange = yRange;}
    public double getXSpeed(){return this.xSpeed;}
    public double getYSpeed(){return this.ySpeed;}
}