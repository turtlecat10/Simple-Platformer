import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

public class Graphics extends JPanel implements ActionListener {

    public static final int tileSize = 48;
    public static final int screenWidth = 32 * tileSize;
    public static final int screenHeight = 18 * tileSize;
    public static final int worldWidth = 60 * tileSize;
    public static final int worldHeight = 20 * tileSize;

    private int cameraX = 0;
    private int cameraY = 0;

    private int currentMap = 1;
    private final ArrayList<GameObject> gameObjects;

    private final Display display;
    private final Movement movement;
    private final Timer gameTimer;

    public Graphics(){

        this.setPreferredSize(new Dimension(screenWidth,screenHeight));
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);

        this.gameObjects = new ArrayList<>();
        initializeAllLevels();

        this.display = new Display(tileSize,screenWidth,screenHeight);
        this.movement = new Movement(tileSize, worldWidth, worldHeight, gameObjects, 2, 5, 4);

        setupKeyBindings();

        this.gameTimer = new Timer(16, this);
        this.gameTimer.start();
    }
    private void initializeAllLevels(){
        InputStream is = getClass().getResourceAsStream("/assets/levels.json");

        if(is == null){
            System.err.println("Error: 'assets/levels.json' file not found!");
            return;
        }
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(is))){
            String line;
            while((line = reader.readLine()) != null){
                line = line.trim();

                if(line.startsWith("[") || line.startsWith("]") || line.isEmpty()){
                    continue;
                }
                line  = line.replaceAll("[\\{\\}\\[\\]\\\"\\s]","");
                if(line.endsWith(",")){
                    line = line.substring(0, line.length() - 1);
                }
                
                if(line.trim().isEmpty()){
                    continue;
                }
                String[] pairs = line.split(",");

                int x = 0, y = 0, width = 1, height = 1, mapID = 1;
                GameObject.Type type = GameObject.Type.solid;

                for(String pair : pairs){
                    String[] keyValue = pair.split(":");
                    if(keyValue.length < 2){
                        continue;
                    }
                    String key = keyValue[0].trim();
                    String value = keyValue[1].trim();
                    switch(key){
                        case "x" -> x = Integer.parseInt(value);
                        case "y" -> y = Integer.parseInt(value);
                        case "width" -> width = Integer.parseInt(value);
                        case "height" -> height = Integer.parseInt(value);
                        case "mapID" -> mapID = Integer.parseInt(value);
                        case "type" ->{
                            if(value.equals("oneWay")) type = GameObject.Type.oneWay;
                            else if(value.equals("background")) type = GameObject.Type.background;
                            else if(value.equals("spawnPoint")) type = GameObject.Type.spawnPoint;
                            else type = GameObject.Type.solid;
                        }
                    }
                }
                gameObjects.add(new GameObject(x, y, width, height, type, mapID));
            }
        }catch (IOException e){
            System.err.println("error parsing");
            e.printStackTrace();
        }
    }
    public void loadLevel(int mapID){
        this.currentMap = mapID;
        for(GameObject obj : gameObjects){
            if(obj.mapID == currentMap && obj.type == GameObject.Type.spawnPoint){
                int gridX = obj.x / tileSize;
                int gridY = obj.y / tileSize;
                movement.respawnPlayer(gridX,gridY);
                break;
            }
        }
    }
    private void updateCamera(){
        cameraX = (movement.getPlayerX() + tileSize / 2) - (screenWidth / 2);
        cameraY = (movement.getPlayerY() + tileSize / 2) - (screenHeight / 2);

        if(cameraX < 0) cameraX = 0;
        if(cameraY < 0) cameraY = 0;
        if(cameraX > worldWidth - screenWidth) cameraX = worldWidth - screenWidth;
        if(cameraY > worldHeight - screenHeight) cameraY = worldHeight - screenHeight;
    }
    @Override 
    public void actionPerformed(ActionEvent e){
        movement.update(currentMap);
        updateCamera();

        if(currentMap == 1 && movement.getPlayerX() > worldWidth - 2 * tileSize){
            loadLevel(2);
        }
        repaint();
    }
    @Override
    protected void paintComponent(java.awt.Graphics g){
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        display.render(g2,this.getWidth(),this.getHeight(),cameraX,cameraY,movement.getPlayerX(),movement.getPlayerY(),gameObjects, currentMap);
        g2.dispose();
    }
    private void setupKeyBindings(){
        InputMap im = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = this.getActionMap();

        im.put(KeyStroke.getKeyStroke("LEFT"),"pLeft");
        am.put("pLeft",new AbstractAction() {
            public void actionPerformed(ActionEvent e){
                movement.setLeft(true);
            }
        });
        im.put(KeyStroke.getKeyStroke("released LEFT"),"rLeft");
        am.put("rLeft",new AbstractAction() {
            public void actionPerformed(ActionEvent e){
                movement.setLeft(false);
            } 
        });

        im.put(KeyStroke.getKeyStroke("RIGHT"),"pRight");
        am.put("pRight",new AbstractAction() {
            public void actionPerformed(ActionEvent e){
                movement.setRight(true);
            }
        });
        im.put(KeyStroke.getKeyStroke("released RIGHT"),"rRight");
        am.put("rRight",new AbstractAction() {
            public void actionPerformed(ActionEvent e){
                movement.setRight(false);
            } 
        });

        im.put(KeyStroke.getKeyStroke("UP"),"pUp");
        am.put("pUp",new AbstractAction() {
            public void actionPerformed(ActionEvent e){
                movement.setUp(true);
            }
        });
        im.put(KeyStroke.getKeyStroke("released UP"),"rUp");
        am.put("rUp",new AbstractAction() {
            public void actionPerformed(ActionEvent e){
                movement.setUp(false);
            } 
        });

        im.put(KeyStroke.getKeyStroke("DOWN"),"pDown");
        am.put("pDown",new AbstractAction() {
            public void actionPerformed(ActionEvent e){
                movement.setDown(true);
            }
        });
        im.put(KeyStroke.getKeyStroke("released DOWN"),"rDown");
        am.put("rDown",new AbstractAction() {
            public void actionPerformed(ActionEvent e){
                movement.setDown(false);
            } 
        });
    }
}
