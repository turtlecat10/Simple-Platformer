package src.game;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class Graphics extends JPanel implements ActionListener {

    public final int tileSize = 48;
    public static final int screenWidth = 1536;
    public static final int screenHeight = 864;
    public static final int worldWidth = 2880;
    public static final int worldHeight = 200 * 48;

    private int startGridX = 2;
    private int startGridY = 5;
    private int speed = 8;

    private int cameraX = 0;
    private int cameraY = 0;
    public int getCameraX(){ return this.cameraX;}
    public int getCameraY(){return this.cameraY;}
    public void setCameraX(int x){ this.cameraX = x;}
    public void setCameraY(int y){ this.cameraY = y;}

    private int currentMap = 1;
    private ArrayList<GameObject> gameObjects;
    private boolean isEditorMode = true;
    private final Display display;
    private final Movement movement;
    private final Timer gameTimer;

    public Graphics(){
        Dimension canvasSize = new Dimension(screenWidth, screenHeight);
        this.setPreferredSize(canvasSize);
        this.setMinimumSize(canvasSize);
        this.setMaximumSize(canvasSize);
        this.setSize(canvasSize);

        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);

        this.gameObjects = new ArrayList<>();
        // initializeAllLevels();
        this.display = new Display(tileSize,screenWidth,screenHeight);

        this.movement = new Movement(tileSize, worldWidth, worldHeight, gameObjects, startGridX, startGridY, speed);

        setupKeyBindings();
        loadLevel(1);

        this.gameTimer = new Timer(16, this);
        if(!isEditorMode){
            this.gameTimer.start();
        }
    }
    
    public void loadLevel(int mapID){
        this.currentMap = mapID;
        boolean found = false;

        for(GameObject obj : gameObjects){
            if(obj.mapID == currentMap && obj.type == GameObject.Type.spawnPoint){
                found = true;
                movement.setInitialSpawn(obj.x, obj.y);
                break;
            }
        }
        if(!found){
            movement.setInitialSpawn(0,0);
        }
        repaint();
    }
    
    public void setEditorMode(boolean status){
        this.isEditorMode = status;
        if(status){
            this.gameTimer.stop();
        }else{
            this.gameTimer.start();
        }
        repaint();
    }
        
    private void centerCameraOnPLayer(){
        int targetCamX = movement.getPlayerX() - (screenWidth / 2) + (tileSize / 2);
        int targetCamY = movement.getPlayerY() - (screenHeight / 2) + (tileSize / 2);

        if(targetCamX < 0) targetCamX = 0;
        if(targetCamY < 0) targetCamY = 0;
        // if(targetCamX > worldWidth - screenWidth) targetCamX = worldWidth - screenWidth;
        // if(targetCamY > worldHeight - screenHeight) targetCamY = worldHeight - screenHeight;

        this.cameraX = targetCamX;
        this.cameraY = targetCamY;
    }
    
    @Override 
    public void actionPerformed(ActionEvent e){
        if(!isEditorMode){
            movement.update(currentMap);
            centerCameraOnPLayer();
            repaint();
        }
    }

    private void setupKeyBindings(){
        InputMap im = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = this.getActionMap();

        // --- LEFT ARROW ---
        im.put(KeyStroke.getKeyStroke("LEFT"), "pLeft");
        am.put("pLeft", new AbstractAction() {
            public void actionPerformed(ActionEvent e){
                if (isEditorMode) {
                    // Inform MapEditorWindow container shell to pan camera left
                    Container parent = getTopLevelAncestor();
                    if (parent instanceof MapEditorWindow) ((MapEditorWindow)parent).setCamLeft(true);
                } else {
                    movement.setLeft(true);
                }
            }
        });
        im.put(KeyStroke.getKeyStroke("released LEFT"), "rLeft");
        am.put("rLeft", new AbstractAction() {
            public void actionPerformed(ActionEvent e){
                Container parent = getTopLevelAncestor();
                if (parent instanceof MapEditorWindow) ((MapEditorWindow)parent).setCamLeft(false);
                movement.setLeft(false);
            } 
        });

        // --- RIGHT ARROW ---
        im.put(KeyStroke.getKeyStroke("RIGHT"), "pRight");
        am.put("pRight", new AbstractAction() {
            public void actionPerformed(ActionEvent e){
                if (isEditorMode) {
                    Container parent = getTopLevelAncestor();
                    if (parent instanceof MapEditorWindow) ((MapEditorWindow)parent).setCamRight(true);
                } else {
                    movement.setRight(true);
                }
            }
        });
        im.put(KeyStroke.getKeyStroke("released RIGHT"), "rRight");
        am.put("rRight", new AbstractAction() {
            public void actionPerformed(ActionEvent e){
                Container parent = getTopLevelAncestor();
                if (parent instanceof MapEditorWindow) ((MapEditorWindow)parent).setCamRight(false);
                movement.setRight(false);
            } 
        });

        // --- UP ARROW ---
        im.put(KeyStroke.getKeyStroke("UP"), "pUp");
        am.put("pUp", new AbstractAction() {
            public void actionPerformed(ActionEvent e){
                if (isEditorMode) {
                    Container parent = getTopLevelAncestor();
                    if (parent instanceof MapEditorWindow) ((MapEditorWindow)parent).setCamUp(true);
                } else {
                    movement.setUp(true);
                }
            }
        });
        im.put(KeyStroke.getKeyStroke("released UP"), "rUp");
        am.put("rUp", new AbstractAction() {
            public void actionPerformed(ActionEvent e){
                Container parent = getTopLevelAncestor();
                if (parent instanceof MapEditorWindow) ((MapEditorWindow)parent).setCamUp(false);
                movement.setUp(false);
            } 
        });

        // --- DOWN ARROW ---
        im.put(KeyStroke.getKeyStroke("DOWN"), "pDown");
        am.put("pDown", new AbstractAction() {
            public void actionPerformed(ActionEvent e){
                if (isEditorMode) {
                    Container parent = getTopLevelAncestor();
                    if (parent instanceof MapEditorWindow) ((MapEditorWindow)parent).setCamDown(true);
                } else {
                    movement.setDown(true);
                }
            }
        });
        im.put(KeyStroke.getKeyStroke("released DOWN"), "rDown");
        am.put("rDown", new AbstractAction() {
            public void actionPerformed(ActionEvent e){
                Container parent = getTopLevelAncestor();
                if (parent instanceof MapEditorWindow) ((MapEditorWindow)parent).setCamDown(false);
                movement.setDown(false);
            } 
        });

        // --- CORE MODIFIERS (Z & X Keys remain active during gameplay) ---
        im.put(KeyStroke.getKeyStroke("Z"), "pShift");
        am.put("pShift", new AbstractAction(){
            public void actionPerformed(ActionEvent e){
                movement.setZoom(true);
            }
        });
        im.put(KeyStroke.getKeyStroke("released Z"), "rShift");
        am.put("rShift", new AbstractAction() {
            public void actionPerformed(ActionEvent e){
                movement.setZoom(false);
            }
        });
        
        im.put(KeyStroke.getKeyStroke("pressed X"), "pX");
        am.put("pX", new AbstractAction(){
            public void actionPerformed(ActionEvent e){
                movement.setDash(true);
            }
        });
        im.put(KeyStroke.getKeyStroke("released X"), "rX");
        am.put("rX", new AbstractAction() {
            public void actionPerformed(ActionEvent e){
                movement.setDash(false);
            }
        });

        im.put(KeyStroke.getKeyStroke("pressed R"), "pR");
        am.put("pR", new AbstractAction(){
            public void actionPerformed(ActionEvent e){
                movement.setReset(true);
            }
        });
        im.put(KeyStroke.getKeyStroke("released R"), "rR");
        am.put("rR", new AbstractAction() {
            public void actionPerformed(ActionEvent e){
                movement.setReset(false);
            }
        });
        

        im.put(KeyStroke.getKeyStroke("ESCAPE"), "clearEditorSelection");
        am.put("clearEditorSelection", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Container parent = getTopLevelAncestor();
                if (parent instanceof MapEditorWindow) {
                    ((MapEditorWindow)parent).clearSelectionPoints();
                }
            }
        });
    }

    @Override
    protected void paintComponent(java.awt.Graphics g){
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        int camOffsetX = this.cameraX;
        int camOffsetY = this.cameraY;

        g2.translate(-camOffsetX, -camOffsetY);

        for(GameObject obj : gameObjects){
            if(obj.mapID != currentMap) continue;

            switch(obj.type){
                case solid: g2.setColor(Color.GREEN); break;
                case oneWay :g2.setColor(new Color(147, 112, 219)); break;
                case death: g2.setColor(Color.RED); break;
                case respawn: g2.setColor(Color.BLUE); break;
                case spawnPoint: g2.setColor(Color.YELLOW); break;
                case ladder: g2.setColor(new Color(150, 75, 0)); break;
                case win: g2.setColor(new Color(212, 175, 55)); break;
                default: g2.setColor(Color.GRAY); break;
            }
            g2.fillRect(obj.x, obj.y, obj.width, obj.height);
            g2.setColor(Color.black);
            g2.drawRect(obj.x, obj.y, obj.width, obj.height);
        }
        g2.setColor(Color.WHITE);
        g2.fillRect(movement.getPlayerX(), movement.getPlayerY(), tileSize, tileSize);
        g2.setColor(Color.BLUE);
        g2.drawRect(movement.getPlayerX(), movement.getPlayerY(), tileSize, tileSize);

        if(isEditorMode){
            g2.setColor(new Color(255, 255, 255, 30)); // Ultra faint transparent white tint
            
            // Draw grid lines matching your viewport camera shifts
            for (int x = camOffsetX; x < camOffsetX + screenWidth + tileSize; x += tileSize) {
                int snapGridX = (x / tileSize) * tileSize;
                g2.drawLine(snapGridX, camOffsetY, snapGridX, camOffsetY + screenHeight);
            }
            for (int y = camOffsetY; y < camOffsetY + screenHeight + tileSize; y += tileSize) {
                int snapGridY = (y / tileSize) * tileSize;
                g2.drawLine(camOffsetX, snapGridY, camOffsetX + screenWidth, snapGridY);
            }

            // Temporarily cancel translation offsets strictly to stamp the HUD text overlay flush
            g2.translate(camOffsetX, camOffsetY);
            g2.setColor(Color.GREEN);
            g2.setFont(new Font("Arial", Font.BOLD, 14));
            g2.drawString(String.format("✏ MAP CANVAS EDITOR ACTIVE [Cam: %d, %d]", camOffsetX, camOffsetY), 15, 25);
        }
        g2.dispose();
    }

    public int getTileSize(){return this.tileSize;}
    public ArrayList<GameObject> getGameObjects(){return this.gameObjects;}
    public boolean getOriginalEditorModeStatus(){return this.isEditorMode;}
    public boolean isEditorMode(){return this.isEditorMode;}
}
