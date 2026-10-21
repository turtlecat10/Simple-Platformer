import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;

public class Display {
    private final int tileSize;
    private final int virtualWidth;
    private final int virtualHeight;

    public Display(int tileSize, int virtualWidth, int virtualHeight){
        this.tileSize = tileSize;
        this.virtualWidth = virtualWidth;
        this.virtualHeight = virtualHeight;
    }

    public void render(Graphics2D g2, int panelWidth, int panelHeight, int cameraX, int cameraY, int playerX, int playerY, ArrayList<GameObject> objects, int currentMap){

        AffineTransform oldTransform = g2.getTransform();
        double scaleX = (double) panelWidth / virtualWidth;
        double scaleY = (double) panelHeight / virtualHeight;
        double scale = Math.min(scaleX,scaleY);

        int offsetX = (int) ((panelWidth - (virtualWidth * scale)) / 2);
        int offsetY = (int) ((panelHeight - (virtualHeight * scale)) / 2);

        g2.translate(offsetX,offsetY);
        g2.scale(scale,scale);

        for(GameObject obj : objects){
            if(obj.mapID != currentMap){
                continue;
            }
            if(obj.type == GameObject.Type.spawnPoint) continue;
            
            if(obj.type == GameObject.Type.solid){
                g2.setColor(Color.GREEN);
            }else if(obj.type == GameObject.Type.oneWay){
                g2.setColor(Color.MAGENTA);
            }else{
                g2.setColor(Color.BLUE);
            }
            g2.fillRect( obj.x - cameraX, obj.y - cameraY, obj.width, obj.height);
        }
        g2.setColor(Color.WHITE);
        int playerScreenX = playerX - cameraX;
        int playerScreenY = playerY - cameraY;
        g2.fillRect(playerScreenX,playerScreenY,tileSize,tileSize);

        g2.setTransform(oldTransform);
    }
}