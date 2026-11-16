package src.game;

import java.awt.Color;
import java.awt.Graphics2D;

public class Particle {
    public double x, y, velX, velY;
    public Color color;
    public int life, maxLife;
    public int size;

    public Particle(double x, double y, Color color){
        this.x = x;
        this.y = y;
        this.color = color;
        this.size = (int) (Math.random() * 10 + 5);
        this.maxLife = 35 + (int) (Math.random() * 40);
        this.life = this.maxLife;

        double angle = Math.random() * Math.PI * 2;
        double speed = 1.5 + Math.random() * 2.0;
        this.velX = Math.cos(angle) * speed;
        this.velY = Math.sin(angle) * speed - 3;
    }

    public void update(){
        this.x += this.velX;
        this.y += this.velY;
        this.velY += 0.25;
        this.life--;
    }

    public void draw(Graphics2D g2){
        float alpha = (float) this.life / this.maxLife;
        if(alpha < 0.0f) alpha = 0.0f;
        if(alpha > 1.0f) alpha = 1.0f;

        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (alpha * 255)));
        g2.fillRect((int) x, (int) (y - size), this.size, this.size);
    }
}
