package src.game;

import java.awt.Color;
import java.util.ArrayList;

public enum ItemRegistry {
    NONE("NONE", Item.ItemType.NONE, null, 0, 0, ""),
    SWORD("SWORD", Item.ItemType.WEAPON, Color.ORANGE, 32, 32, "Damage: 10"),
    POTION("POTION", Item.ItemType.CONSUMABLE, Color.RED, 24, 24, "Heal: 50"),
    COIN("COIN", Item.ItemType.MISC, Color.YELLOW, 20, 20, "Value: 10"),
    GEM("GEM", Item.ItemType.MISC, Color.CYAN, 24, 24, "Value: 100");
    

    private final String displayName;
    private final Item.ItemType type;
    private final Color color;
    private final int width;
    private final int height;
    private final String effects;

    ItemRegistry(String displayName, Item.ItemType type, Color color, int width, int height, String effects){
        this.displayName = displayName;
        this.type = type;
        this.color = color;
        this.width = width;
        this.height = height;
        this.effects = effects;
    }

    // ItemRegistry(String displayName){
    //     this.displayName = displayName;
    //     this.type = Item.ItemType.MISC;
    //     this.color = Color.GRAY;
    // }

    public String getDisplayName(){ return displayName;}

    public Item create(int x, int y, int mapID){
        return new Item(name(), type, color, x, y, width, height, mapID, effects, new java.util.ArrayList<>(), new java.util.ArrayList<>(), new java.util.ArrayList<>(), new java.util.ArrayList<>());
    }

    public static String[] getDisplayChoices(){
        ArrayList<String> choices = new ArrayList<>();
        for(ItemRegistry r : values()){
            choices.add(r.getDisplayName());
        }
        return choices.toArray(new String[0]);
    }

    public static ItemRegistry fromDisplayName(String name){
        for(ItemRegistry r : values()){
            if(r.getDisplayName().equalsIgnoreCase(name)) return r;
        }
        return NONE;
    }
}
