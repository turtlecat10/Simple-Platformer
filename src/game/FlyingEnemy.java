package src.game;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.awt.Rectangle;;

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
    public void aiTick(ArrayList<GameObject> objects, double playerX, double playerY){
        this.isGrounded = true;
        if(hasPathfinding){
            double dx = playerX - this.x;
            double dy = playerY - this.y;
            double dist = Math.hypot(dx, dy);
            if(dist > 1){
                double desiredVx = (dx / dist) * Math.abs(this.xSpeed * 15);
                double desiredVy = (dy / dist) * Math.abs(this.ySpeed * 15);

                boolean blocked = false;
                for(GameObject obj : objects){
                    if(obj.mapID != this.mapID || obj.type != GameObject.Type.solid) continue;
                    Rectangle futureBox = new Rectangle((int)(this.x + desiredVx), (int)(this.y + desiredVy - this.height), this.width, this.height);
                    Rectangle blockBounds = new Rectangle(obj.currentX, obj.currentY - obj.height, obj.width, obj.height);
                    if(futureBox.intersects(blockBounds)){
                        blocked = true;
                        break;
                    }
                }
                if(blocked){
                    this.velX = -desiredVy * 0.5;
                    this.velY = desiredVx * 0.5;
                }else{
                    this.velX = desiredVx;
                    this.velY = desiredVy;
                }
            }
            this.x += (int)Math.round(this.velX);
            this.y += (int)Math.round(this.velY);
            return;
        }

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
        if(MapEditorWindow.instance.getSelectedEnemy() != this){
            g2.setColor(Color.BLACK);
            g2.drawRect(x, renderY, width, height);
        }else{
            g2.setColor(Color.BLUE);
            float thickness = 4.0f;
            g2.setStroke(new BasicStroke(thickness));
            g2.drawRect(x, renderY, width, height);
        }
        
    }
    @Override 
    public Color getParticleColor(){
        return new Color(0, 139, 0);
    }

    public double getAngle() {return this.angle;}
    public void setAngle(double angle){this.angle = angle;}
    public double getHomeX(){return this.homeX;}
    public double getHomeY(){ return this.homeY;}
    public double getXRange(){ return this.xRange;}
    public void setXRange(double xRange){ this.xRange = xRange;}
    public double getYRange(){return this.yRange;}
    public void setYRange(double yRange){ this.yRange = yRange;}
    public double getXSpeed(){return this.xSpeed;}
    public double getYSpeed(){return this.ySpeed;}
}