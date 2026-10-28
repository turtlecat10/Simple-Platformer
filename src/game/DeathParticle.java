package src.game;
public class DeathParticle {
    public double x, y;
    public double velX, velY;
    public int size;
    public int alpha = 255;
    public DeathParticle(double x, double y){
        this.x = x;
        this.y = y;
        this.velX = (Math.random() - 0.5) * 8.0;
        this.velY = (Math.random() - 0.5) * 8.0 - 2;
        this.size = (int) (Math.random() * 4) +3;
    }
    public void update(){
        this.x += this.velX;
        this.y += this.velY;
        this.velY += 0.2;
        this.alpha -= 12;
        if(this.alpha < 0) this.alpha = 0;
    }
}