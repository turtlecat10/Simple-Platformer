package src.game;

import java.awt.Color;

public enum ItemRegistry {
    NONE("CANCEL/NONE"),
    SWORD("SWORD (weapon)", Item.ItemType.WEAPON, Color.ORANGE),
    POTION("POTION (Consumable)", Item.ItemType.CONSUMABLE, Color.RED),
    COIN("COIN (misc)", Item.ItemType.MISC, Color.YELLOW);

    private final String displayName;
    private final Item.ItemType type;
    private final Color defaultColor;

    ItemRegistry(String displayName, Item.ItemType type, Color defaultColor){
        this.displayName = displayName;
        this.type = type;
        this.defaultColor = defaultColor;
    }

    ItemRegistry(String displayName){
        this.displayName = displayName;
        this.type = Item.ItemType.MISC;
        this.defaultColor = Color.GRAY;
    }

    public String getDisplayName(){ return displayName;}

    public Item create(int x, int y, int mapID){

        return new Item(name(), type, defaultColor, x, y, 32, 32, mapID, "", new java.util.ArrayList<>(), new java.util.ArrayList<>(), new java.util.ArrayList<>(), new java.util.ArrayList<>());
    }

    public static String[] getDisplayChoices(){
        ItemRegistry[] values = values();
        String[] choices = new String[values.length];
        for(int i = 0; i < values.length; i++){
            choices[i] = values[i].getDisplayName();
        }
        return choices;
    }

    public static ItemRegistry fromDisplayName(String displayName){
        for(ItemRegistry type : values()){
            if(type.getDisplayName().equals(displayName)) return type;
        }
        return NONE;
    }
}
