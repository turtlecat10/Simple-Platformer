package src.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;

public class FlyingEnemy extends Enemy{

    private double homeX;
    private double homeY;

    private double angle = 0;

    private final double xRange = 64.0;
    private final double yRange = 32.0;
    private final double xSpeed = 0.04;
    private final double ySpeed = 0.06;

    private final double returnHomeSpeed = 0.05;


    public FlyingEnemy(int x, int y, int mapID){
        super(x, y, 32, 32, mapID);
        this.type = "FLYING";
        this.isGrounded = true;
        this.homeX = x;
        this.homeY = y;
    }

    @Override 
    public void aiTick(ArrayList<GameObject> objects){
        this.isGrounded = true;

        if(homeX != startX) homeX += (startX - homeX) * returnHomeSpeed;
        if(homeY != startY) homeY += (startY - homeY) * returnHomeSpeed;

        angle += 1.0;

        double targetX = homeX + Math.sin(angle * xSpeed) * xRange;
        double targetY = homeY + Math.cos(angle * ySpeed) * yRange;

        this.velX = (targetX - this.x) * 0.4;
        this.velY = (targetY - this.y) * 0.4;
    }
    @Override 
    protected void handleWallBump(){
        this.angle = -this.angle;

        this.homeX = this.x - Math.sin(angle * xSpeed) * xRange;
        this.homeY = this.y - Math.cos(angle * ySpeed) * yRange;
    }
    @Override 
    public void draw(Graphics2D g2){
        g2.setColor(new Color(0, 139, 0));
        g2.fillRect(x, y, width, height);
        g2.setColor(Color.BLACK);
        g2.drawRect(x, y, width, height);
    }
}
