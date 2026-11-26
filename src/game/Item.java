package src.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.awt.Point;

public class Item {
    public enum ItemType{ WEAPON, CONSUMABLE, MISC, NONE}

    public String name;
    public ItemType type;
    public Color color;
    public int startX, startY;
    public int x, y;
    public int width, height;
    public boolean isInInventory = false;
    public boolean isEquipped = false;
    public int mapID;
    public String effects;
    public ArrayList<Integer> attackX, attackY;
    public ArrayList<Double> speed;
    public ArrayList<Boolean> isSpeed;

    public boolean isAttacking = false;
    public int attackWaypointIndex = 0;
    public double attackProgress = 0.0;
    public double currentAttackX, currentAttackY;

    public boolean reverseDirection;
    public ArrayList<Point> activeAttackPoints = new ArrayList<>();
    public int inventoryNumber = 0;

    public Item(String name, ItemType type, Color color, int x, int y, int width, int height, int mapID, String effects, boolean reverse, ArrayList<Integer> attackX, ArrayList<Integer> attackY, ArrayList<Double> speed, ArrayList<Boolean> isSpeed){
        this.name = name;
        this.type = type;
        this.color = color;
        this.startX = x;
        this.startY = y;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.mapID = mapID;
        this.effects = effects;
        this.reverseDirection = reverse;
        this.attackX = attackX;
        this.attackY = attackY; 
        this.speed = speed;
        this.isSpeed = isSpeed;
    }
    public Rectangle getBounds(){
        if(isAttacking){
            return new Rectangle((int)currentAttackX, (int) currentAttackY - height, width, height);
        }
        return new Rectangle(x, y - height, width, height);
    }
    public void draw(Graphics2D g2){
        if (isInInventory && !isAttacking) return;

        int renderX = isAttacking ? (int) currentAttackX : x;
        int renderY = (isAttacking ? (int) currentAttackY : y) - height;

        g2.setColor(color);
        g2.fillRect(renderX, renderY, width, height);
        g2.setColor(Color.BLACK);
        g2.drawRect(renderX, renderY, width, height);
    }

    public void resetToStart(){
        this.x = this.startX;
        this.y = this.startY;
        this.isInInventory = false;
        this.isEquipped = false;
        this.isAttacking = false;
        this.attackWaypointIndex = 0;
        this.attackProgress = 0.0;
        this.inventoryNumber = 0;
        if(activeAttackPoints != null){
            activeAttackPoints.clear();
        }else{
            activeAttackPoints = new ArrayList<>();
        }
    }
}
