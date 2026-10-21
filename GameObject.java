public class GameObject {
    public enum Type{
        solid,
        oneWay,
        background,
        spawnPoint
    }
    public int x, y;
    public int width, height;
    public Type type;
    public int mapID;
    public GameObject(int x, int y, int width, int height, Type type, int mapID){
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.type = type;
        this.mapID = mapID;
    }
}
