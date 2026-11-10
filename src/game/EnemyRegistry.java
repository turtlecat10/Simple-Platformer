package src.game;

public enum EnemyRegistry {
    NONE("CANCEL/NONE", null),
    PATROL("PATROL (walking)", PatrolEnemy::new),
    JUMPING("JUMPING (leaping)", JumpingEnemy::new),
    FLYING("FLYING (oscillating)", FlyingEnemy::new);

    private final String displayName;
    private final EnemyCreator creator;

    EnemyRegistry(String displayName, EnemyCreator creator){
        this.displayName = displayName;
        this.creator = creator;
    }

    public String getDisplayName(){return displayName;}
    public Enemy create(int x, int y, int width, int height, int mapID, double velX){
        if(creator == null) return null;
        return creator.create(x, y, width, height, mapID, velX);
    }
    @FunctionalInterface 
    public interface EnemyCreator{
        Enemy create(int x, int y, int width, int height, int mapID, double velX);
    }

    public static Enemy createFromString(String type, int x, int y, int width, int height, int mapID, double velX){
        try{
            return EnemyRegistry.valueOf(type.toUpperCase()).create(x, y, width, height, mapID, velX);
        }catch(IllegalArgumentException e){
            return null;
        }
    }
    public static String[] getDisplayChoices(){
        EnemyRegistry[] values = values();
        String[] choices = new String[values.length];
        for(int i = 0; i < values.length; i++){
            choices[i] = values[i].getDisplayName();
        }
        return choices;
    }
    public static EnemyRegistry fromDisplayName(String displayName){
        for(EnemyRegistry type : values()){
            if(type.getDisplayName().equals(displayName)) return type;
        }
        return NONE;
    }
}