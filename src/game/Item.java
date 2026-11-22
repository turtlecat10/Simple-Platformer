package src.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

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
    

    public Item(String name, ItemType type, Color color, int x, int y, int width, int height, int mapID, String effects){
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
    }
    public Rectangle getBounds(){
        return new Rectangle(x, y - height, width, height);
    }
    public void draw(Graphics2D g2){
        if (isInInventory) return;

        int renderTopY = y - height;
        g2.setColor(color);
        g2.fillRect(x, renderTopY, width, height);
        g2.setColor(Color.BLACK);
        g2.drawRect(x, renderTopY, width, height);
    }

    public void resetToStart(){
        if(!isInInventory){
            this.x = this.startX;
            this.y = this.startY;
        }
    }
}
