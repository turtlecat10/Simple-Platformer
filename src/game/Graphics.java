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
    public final int playerSize = (int) (tileSize * 0.75);

    private int startGridX = 2;
    private int startGridY = 5;
    private int speed = 8;

    private int cameraX = 0;
    private int cameraY = 0;
    public int getCameraX(){ return this.cameraX;}
    public int getCameraY(){return this.cameraY;}
    public void setCameraX(int x){ this.cameraX = x;}
    public void setCameraY(int y){ this.cameraY = y;}

    public Rectangle lastIndicatorBounds = null;

    private int currentMap = 1;
    private ArrayList<GameObject> gameObjects;
    private boolean isEditorMode = true;
    private final Display display;
    private final Movement movement;
    private final Timer gameTimer;

    private final java.util.ArrayList<DeathParticle> deathParticles = new java.util.ArrayList<>();
    public final java.util.ArrayList<Enemy> activeEnemies = new java.util.ArrayList<>();
    private ArrayList<Particle> activeParticles = new ArrayList<>();


    public final int virtualWidth = 1920;
    public final int virtualHeight = 1080;

    public Graphics(){
        Dimension canvasSize = new Dimension(screenWidth, screenHeight);
        this.setPreferredSize(canvasSize);
        this.setMinimumSize(canvasSize);
        this.setMaximumSize(canvasSize);
        this.setSize(canvasSize);

        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);

        this.gameObjects = new ArrayList<>();
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
        int targetCamX = movement.getPlayerX() - (screenWidth / 2) + (playerSize / 2);
        int targetCamY = movement.getPlayerY() - (screenHeight / 2) + (playerSize / 2);

        if(targetCamX < 0) targetCamX = 0;
        if(targetCamY < 0) targetCamY = 0;

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

        // --- CORE MODIFIERS ---
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
    protected void paintComponent(java.awt.Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        drawLevelCanvas(g2, currentMap, true, getWidth(), getHeight());

        java.awt.geom.AffineTransform oldTransform = g2.getTransform();
        double scaleX = (double) getWidth() / virtualWidth;
        double scaleY = (double) getHeight() / virtualHeight;
        double scale = Math.min(scaleX, scaleY);
        int offsetX = (int) ((getWidth() - (virtualWidth * scale)) / 2);
        int offsetY = (int) ((getHeight() - (virtualHeight * scale)) / 2);
        
        g2.translate(offsetX, offsetY);
        g2.scale(scale, scale);
        g2.translate(-this.cameraX, -this.cameraY);

        for(int i = deathParticles.size() - 1; i >= 0; i--){
            DeathParticle p = deathParticles.get(i);
            p.update();
            if(p.alpha <= 0){
                deathParticles.remove(i);
                continue;
            }
            g2.setColor(new Color(255, (int)(Math.random() * 100), 0, p.alpha));
            g2.fillRect((int)p.x, (int)p.y, p.size, p.size);
        }


        g2.setTransform(oldTransform);

        g2.setFont(new Font("Arial", Font.BOLD, 14));

        if (isEditorMode) {
            g2.setColor(Color.GREEN);
            g2.drawString(String.format("✏ MAP CANVAS EDITOR ACTIVE [Cam: %d, %d]", this.cameraX, this.cameraY), 15, 25);
        } else {
            if(movement != null){
                g2.setColor(new Color(0, 0, 0, 160));
                g2.fillRect(8, 8, 120, 24);
                g2.setColor(Color.BLACK);
                g2.drawRect(8, 8, 120, 24);

                g2.setColor(Color.RED);
                g2.drawString(String.format("Deaths: %d", movement.getDeathCount()), 15, 25);
            }
        }
        
        g2.dispose();
    }
    
    public void spawnDeathExplosion(int x, int y){
        int particleCount = 35;
        for(int i = 0; i < particleCount; i++){
            deathParticles.add(new DeathParticle(x + (getPlayerSize() / 2), y + getPlayerSize() / 2));
        }
    }

    public void spawnDeathParticles(int x, int y, int width, int height, Color color){
        double centerX = x + width / 2.0;
        double centerY = y - height / 2.0;
        for(int i = 0; i < 18; i++){
            activeParticles.add(new Particle(centerX, centerY, color));
        }
    }

    public void updateAndRenderParticles(Graphics2D g2){
        for(int i = activeParticles.size() - 1; i >= 0; i--){
            Particle p = activeParticles.get(i);
            p.update();
            p.draw(g2);
            if(p.life <= 0){
                activeParticles.remove(i);
            }
        }
    }

    public void drawLevelCanvas(Graphics2D g2, int mapID, boolean drawPlayer, int panelWidth, int panelHeight){
        java.awt.geom.AffineTransform oldTransform = g2.getTransform();

        double scaleX = (double) panelWidth / virtualWidth;
        double scaleY = (double) panelHeight / virtualHeight;
        double scale = Math.min(scaleX, scaleY);

        int offsetX = (int) ((panelWidth - virtualWidth * scale)) / 2;
        int offsetY = (int) ((panelHeight - virtualHeight * scale)) / 2;

        g2.translate(offsetX, offsetY);
        g2.scale(scale, scale);
        g2.translate(-this.cameraX, -this.cameraY);

        if(gameObjects != null){
            for(GameObject obj : gameObjects){
                if(obj.mapID != mapID) continue;
                if(obj.isTriggered && !obj.triggeredDisplay) continue;

                int renderX = obj.currentX;
                int renderY = obj.currentY - obj.height; // Convert bottom-left Y to top-left Y for rendering

                switch(obj.type){
                    case solid: g2.setColor(Color.GREEN); break;
                    case oneWay: g2.setColor(new Color(147, 112, 219)); break;
                    case death: g2.setColor(Color.RED); break;
                    case respawn: g2.setColor(Color.BLUE); break;
                    case spawnPoint: g2.setColor(Color.YELLOW); break;
                    case ladder: g2.setColor(new Color(150, 75, 0)); break;
                    case win: g2.setColor(new Color(212, 175, 55)); break;
                    case trigger: g2.setColor(Color.ORANGE); break;
                    default: g2.setColor(Color.GRAY); break;
                }
                g2.fillRect(renderX, renderY, obj.width, obj.height);
                g2.setColor(Color.black);
                g2.drawRect(renderX, renderY, obj.width, obj.height);
            }
        }
        if(movement != null && movement.getEnemies() != null){
            for(Enemy enemy : movement.getEnemies()){
                if(enemy.mapID == mapID && enemy.isDead){
                    updateAndRenderParticles(g2);
                }
                if(enemy.mapID == mapID && !enemy.isDead){
                    enemy.draw(g2);

                    Enemy currentSelected = (MapEditorWindow.instance != null) ? MapEditorWindow.instance.getSelectedEnemy() : null;
                    if(enemy == currentSelected && isEditorMode){
                        Stroke oldStroke = g2.getStroke();
                        g2.setColor(Color.CYAN);
                        g2.setStroke(new java.awt.BasicStroke(4.0f, java.awt.BasicStroke.CAP_BUTT, java.awt.BasicStroke.JOIN_MITER, 1.0f, new float[]{4f}, 0.0f));
                        g2.drawRect(enemy.x, enemy.y - enemy.height, enemy.width, enemy.height);
                        g2.setStroke(oldStroke);
                    }
                }
            }
        }
        
        Enemy currentSelected = (MapEditorWindow.instance != null) ? MapEditorWindow.instance.getSelectedEnemy() : null;
        if(currentSelected != null && currentSelected.hasPathfinding && this.isEditorMode && currentSelected instanceof FlyingEnemy){
            FlyingEnemy fe = (FlyingEnemy) currentSelected;

            g2.setColor(new Color(0, 191, 255, 150));
            g2.setStroke(new java.awt.BasicStroke(1.5f, java.awt.BasicStroke.CAP_BUTT, java.awt.BasicStroke.JOIN_MITER, 1.0f, new float[]{4f}, 0.0f));

            double simX = fe.x;
            double simY = fe.y;

            Point prevPt = new Point((int) simX + fe.width / 2, (int) simY - fe.height / 2);

            for(int frame = 0; frame < 300; frame += 1){
                double dx = getMovement().getPlayerX() - simX;
                double dy = getMovement().getPlayerY() - simY;
                double dist = Math.hypot(dx, dy);
                if(dist> 1){
                    simX += (dx / dist) * Math.abs(fe.getXSpeed() * 15) * 0.5;
                    simY += (dy / dist) * Math.abs(fe.getYSpeed() * 15) * 0.5;
                }
                Point nextPt = new Point((int) simX + fe.width / 2, (int) simY - fe.height / 2);
                g2.drawLine(prevPt.x, prevPt.y, nextPt.x, nextPt.y);
                prevPt = nextPt;
            }
            


        }else if(this.isEditorMode && currentSelected instanceof FlyingEnemy && currentSelected.mapID == mapID){
            FlyingEnemy fe = (FlyingEnemy) currentSelected;

            g2.setColor(new Color(0, 191, 255, 150));
            g2.setStroke(new java.awt.BasicStroke(1.5f, java.awt.BasicStroke.CAP_BUTT, java.awt.BasicStroke.JOIN_MITER, 1.0f, new float[]{4f}, 0.0f));

            int prevX = -1;
            int prevY = -1;

            for(int t = 0; t <= 300; t += 1){
                double simAngle = fe.getAngle() + t;

                double targetX = fe.getHomeX() + Math.sin(simAngle * fe.getXSpeed()) * fe.getXRange();
                double targetY = fe.getHomeY() + Math.cos(simAngle * fe.getYSpeed()) * fe.getYRange();

                int currX = (int) Math.round(targetX) + (fe.width / 2);
                int currY = (int) Math.round(targetY) - (fe.height / 2);

                if(prevX != -1 && prevY != -1){
                    g2.drawLine(prevX, prevY, currX, currY);
                }
                prevX = currX;
                prevY = currY;
            }
        }

        Object selectedTarget = (MapEditorWindow.instance != null) ? (MapEditorWindow.instance.getPropertyEditingObject() != null ? MapEditorWindow.instance.getPropertyEditingObject() : MapEditorWindow.instance.getSelectedEnemy()) : null;

        if(this.isEditorMode && selectedTarget != null){
            int targetX = 0, targetY = 0, targetW = 36, targetH = 36;
            Color indicatorColor = Color.GRAY;

            if(selectedTarget instanceof GameObject){
                GameObject obj = (GameObject) selectedTarget;
                if(obj.mapID == mapID){
                    targetX = obj.x;
                    targetY = obj.y - obj.height;
                    targetW = obj.width;
                    targetW = obj.width;
                    targetH = obj.height;

                    switch(obj.type){
                        case solid: indicatorColor = Color.GREEN; break;
                        case oneWay: indicatorColor = new Color(147, 112, 219); break;
                        case death: indicatorColor = Color.RED; break;
                        case respawn: indicatorColor = Color.BLUE; break;
                        case spawnPoint: indicatorColor = Color.YELLOW; break;
                        case ladder: indicatorColor = new Color(150, 75, 0); break;
                        case win: indicatorColor = new Color(212, 175, 55); break;
                        case trigger: indicatorColor = Color.ORANGE; break;
                        default: indicatorColor = Color.GRAY; break;
                    }
                }
            }else if(selectedTarget instanceof Enemy){
                Enemy en = (Enemy) selectedTarget;
                if(en.mapID == mapID){
                    targetX = en.x;
                    targetY = en.y - en.height;
                    targetW = en.width;
                    targetH = en.height;
                    indicatorColor = en.getParticleColor();
                }
            }

            int centerX = targetX + targetW / 2;
            int centerY = targetY + targetH / 2;

            boolean isOffScreen = centerX < cameraX || centerX > cameraX + virtualWidth || centerY < cameraY || centerY > cameraY + virtualHeight;

            if(isOffScreen){
                int screenCamCenterX = cameraX + virtualWidth / 2;
                int screenCamCenterY = cameraY + virtualHeight / 2;

                double angle = Math.atan2(centerY - screenCamCenterY, centerX - screenCamCenterX);
                int dist = (int) Math.hypot(centerX - screenCamCenterX, centerY - screenCamCenterY);

                int arrowX = screenCamCenterX + (int) (Math.cos(angle) * (virtualWidth / 2 - 80));
                int arrowY = screenCamCenterY + (int) (Math.sin(angle) * (virtualHeight / 2 - 80));

                java.awt.geom.AffineTransform oldTrans = g2.getTransform();
                g2.translate(arrowX, arrowY);
                g2.rotate(angle);

                g2.setColor(indicatorColor);
                g2.setStroke(new java.awt.BasicStroke(2.5f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));

                g2.drawLine(16, 0, 6, -6);
                g2.drawLine(16, 0, 6, 6);

                g2.drawLine(-12, 0, 16, 0);

                g2.drawLine(-12, 0, -18, -6);
                g2.drawLine(-14, 0, -20, 0);
                g2.drawLine(-12, 0, -18, 6);

                g2.setTransform(oldTrans);

                String distStr = dist + "px";
                g2.setFont(new Font("Arial", Font.BOLD, 12));
                FontMetrics fm = g2.getFontMetrics();
                int strW = fm.stringWidth(distStr);

                g2.setColor(new Color(0, 0, 0, 200));
                g2.fillRect(arrowX - strW / 2 - 4, arrowY + 16, strW + 8, 20);
                g2.setColor(Color.YELLOW);
                g2.drawString(distStr, arrowX - strW / 2, arrowY + 30);

                lastIndicatorBounds = new Rectangle(arrowX - strW / 2 - 4, arrowY + 16, strW + 8, 20);
            }else{
                lastIndicatorBounds = null;
            }
        }else{
            lastIndicatorBounds = null;
        }

        if(drawPlayer && movement != null){
            if(!movement.isDead){
                g2.setColor(Color.WHITE);
                g2.fillRect(movement.getPlayerX(), movement.getPlayerY() - playerSize, playerSize, playerSize);
                g2.setColor(Color.BLUE);
                g2.drawRect(movement.getPlayerX(), movement.getPlayerY() - playerSize, playerSize, playerSize);
            }
        }

        if (this.isEditorMode && drawPlayer) {
            g2.setStroke(new java.awt.BasicStroke(1.0f));
            g2.setColor(new Color(255, 255, 255, 30));

            for (int x = this.cameraX; x < this.cameraX + virtualWidth + tileSize; x += tileSize) {
                int snapGridX = (x / tileSize) * tileSize;
                g2.drawLine(snapGridX, this.cameraY, snapGridX, this.cameraY + virtualHeight);
            }
            
            for (int y = this.cameraY; y < this.cameraY + virtualHeight + tileSize; y += tileSize) {
                int snapGridY = (y / tileSize) * tileSize;
                g2.drawLine(this.cameraX, snapGridY, this.cameraX + virtualWidth, snapGridY);
            }

            java.awt.Point anchor = MapEditorWindow.instance.getBottomLeftCorner();
            java.awt.Point hover = MapEditorWindow.instance.getCurrentHoverPoint();
            GameObject.Type brush = MapEditorWindow.instance.getActiveBrushType();
            if (anchor != null && hover != null && brush != null) {
                int previewX, previewY, previewWidth, previewHeight;
                if(!MapEditorWindow.instance.getFreePlace()){
                    int cellX = Math.floorDiv(hover.x, tileSize);
                    int cellY = Math.floorDiv(hover.y, tileSize);
                    
                    boolean leftOfStart = (cellX * tileSize) < anchor.x;
                    boolean aboveStart = (cellY * tileSize) < (anchor.y - tileSize);

                    int targetMinX = leftOfStart ? (cellX * tileSize) : anchor.x;
                    int targetMaxX = leftOfStart ? anchor.x : ((cellX + 1) * tileSize);
                    
                    int targetMinY = aboveStart ? (cellY * tileSize) : (anchor.y - tileSize);
                    int targetMaxY = aboveStart ? anchor.y : ((cellY + 1) * tileSize);

                    previewX = targetMinX;
                    previewY = targetMaxY; // top-left for rendering rectangle
                    previewWidth = targetMaxX - targetMinX;
                    previewHeight = targetMaxY - targetMinY;
                }else{
                    previewX = Math.min(anchor.x, hover.x);
                    int rawMinY = Math.min(anchor.y, hover.y);
                    int rawMaxY = Math.max(anchor.y, hover.y);
                    previewY = rawMaxY;
                    previewWidth = Math.abs(hover.x - anchor.x);
                    previewHeight = Math.abs(hover.y - anchor.y);
                }

                if (previewWidth == 0) previewWidth = tileSize;
                if (previewHeight == 0) previewHeight = tileSize;

                Color previewColor;
                switch (brush) {
                    case solid: previewColor = Color.GREEN; break;
                    case oneWay: previewColor = new Color(147, 112, 219); break;
                    case death: previewColor = Color.RED; break;
                    case respawn: previewColor = Color.BLUE; break;
                    case spawnPoint: previewColor = Color.YELLOW; break;
                    case ladder: previewColor = new Color(150, 75, 0); break;
                    case win: previewColor = new Color(212, 175, 55); break;
                    case trigger: previewColor = Color.ORANGE; break;
                    default: previewColor = Color.GRAY; break;
                }

                int renderPreviewY = previewY - previewHeight;
                g2.setColor(new Color(previewColor.getRed(), previewColor.getGreen(), previewColor.getBlue(), 100));
                g2.fillRect(previewX, renderPreviewY, previewWidth, previewHeight);

                g2.setColor(previewColor);
                g2.setStroke(new java.awt.BasicStroke(2f, java.awt.BasicStroke.CAP_BUTT, java.awt.BasicStroke.JOIN_MITER, 1.0f, new float[]{4f}, 0.0f));
                g2.drawRect(previewX, renderPreviewY, previewWidth, previewHeight);
            }
        }

        int activeChannel = MapEditorWindow.instance.getHighlightedBroadcastChannel();
        GameObject activeTriggerObj = MapEditorWindow.instance.getPropertyEditingObject();
        GameObject editingObj = MapEditorWindow.instance.getPropertyEditingObject();

        if(this.isEditorMode && activeChannel > 0 && activeTriggerObj != null && activeTriggerObj.mapID == mapID){
            int triggerCenterX = activeTriggerObj.x + (activeTriggerObj.width / 2);
            int triggerCenterY = (activeTriggerObj.y - activeTriggerObj.height) + (activeTriggerObj.height / 2);

            for (GameObject obj : gameObjects) {
                if (obj.mapID == mapID && obj.triggeredBy == activeChannel) {
                    int objTopY = obj.y - obj.height;
                    g2.setColor(new Color(0, 255, 255, 60));
                    g2.fillRect(obj.x, objTopY, obj.width, obj.height);
                    
                    g2.setColor(Color.CYAN);
                    g2.setStroke(new java.awt.BasicStroke(2.0f, java.awt.BasicStroke.CAP_BUTT, java.awt.BasicStroke.JOIN_MITER, 1.0f, new float[]{4f}, 0.0f));
                    g2.drawRect(obj.x, objTopY, obj.width, obj.height);

                    int targetCenterX = obj.x + (obj.width / 2);
                    int targetCenterY = objTopY + (obj.height / 2);

                    g2.setStroke(new java.awt.BasicStroke(1.5f));
                    g2.drawLine(triggerCenterX, triggerCenterY, targetCenterX, targetCenterY);
                    g2.fillOval(targetCenterX - 4, targetCenterY - 4, 8, 8);
                }
            }
        }

        if(MapEditorWindow.instance != null && isEditorMode){
            GameObject selectedObj = MapEditorWindow.instance.getPropertyEditingObject();
            if(selectedObj != null && selectedObj.mapID == mapID){
                renderSelectedObjectPath(g2, selectedObj, false);
                renderSelectedObjectPath(g2, selectedObj, true);
            }
        }

        GameObject activeEditingBlock = MapEditorWindow.instance.getPropertyEditingObject();
        Point pathCursor = MapEditorWindow.instance.getPathHoverPoint();
        
        if (this.isEditorMode && activeEditingBlock != null && pathCursor != null && activeEditingBlock.mapID == mapID) {
            String modeName = MapEditorWindow.instance.getCurrentPathClickModeName();
            
            int origCenterX = activeEditingBlock.x + (activeEditingBlock.width / 2);
            int origCenterY = (activeEditingBlock.y - activeEditingBlock.height) + (activeEditingBlock.height / 2);
            int trigStartCenterX = activeEditingBlock.triggeredX + (activeEditingBlock.width / 2);
            int trigStartCenterY = (activeEditingBlock.triggeredY - activeEditingBlock.height) + (activeEditingBlock.height / 2);
            int trigOscEndCenterX = activeEditingBlock.triggerMoveX + (activeEditingBlock.width / 2);
            int trigOscEndCenterY = (activeEditingBlock.triggerMoveY - activeEditingBlock.height) + (activeEditingBlock.height / 2);

            int cursorCenterX = pathCursor.x + (activeEditingBlock.width / 2);
            int cursorCenterY = (pathCursor.y - activeEditingBlock.height) + (activeEditingBlock.height / 2);

            Color hoverPreviewColor = Color.WHITE;
            String previewLabel = "";

            if (modeName.equals("OSCILLATION")) {
                hoverPreviewColor = new Color(138, 43, 226);
                previewLabel = "Always Oscillation Target";
                g2.drawLine(origCenterX, origCenterY, cursorCenterX, cursorCenterY);
            } else if (modeName.equals("TRIGGERED_MOVE")) {
                hoverPreviewColor = Color.ORANGE;
                previewLabel = "Triggered Move Destination";
                g2.drawLine(origCenterX, origCenterY, cursorCenterX, cursorCenterY);

                boolean hasTrigOscEnd = (activeEditingBlock.triggerMoveX != activeEditingBlock.x || activeEditingBlock.triggerMoveY != activeEditingBlock.y) && (activeEditingBlock.triggerMoveX != 0);
                if(hasTrigOscEnd){
                    g2.setColor(Color.CYAN);
                    g2.drawLine(cursorCenterX, cursorCenterY, trigOscEndCenterX, trigOscEndCenterY);
                }
            } else if (modeName.equals("OSCILLATE_AFTER_TRIGGER")) {
                hoverPreviewColor = Color.CYAN;
                previewLabel = "Post-Trigger Oscillation Target";
                boolean hasTrigStart = (activeEditingBlock.triggeredX != activeEditingBlock.x || activeEditingBlock.triggeredY != activeEditingBlock.y) && (activeEditingBlock.triggeredX != 0);
                if(hasTrigStart){
                    g2.drawLine(trigStartCenterX, trigStartCenterY, cursorCenterX, cursorCenterY);
                }else{
                    g2.drawLine(origCenterX, origCenterY, cursorCenterX, cursorCenterY);
                }
            }

            int cursorTopY = pathCursor.y - activeEditingBlock.height;
            g2.setColor(new Color(hoverPreviewColor.getRed(), hoverPreviewColor.getGreen(), hoverPreviewColor.getBlue(), 60));
            g2.fillRect(pathCursor.x, cursorTopY, activeEditingBlock.width, activeEditingBlock.height);

            g2.setColor(hoverPreviewColor);
            g2.setStroke(new java.awt.BasicStroke(2.0f, java.awt.BasicStroke.CAP_BUTT, java.awt.BasicStroke.JOIN_MITER, 1.0f, new float[]{5f}, 0.0f));
            g2.drawRect(pathCursor.x, cursorTopY, activeEditingBlock.width, activeEditingBlock.height);
            
            g2.setFont(new Font("Arial", Font.BOLD, 11));
            g2.setColor(Color.BLACK);
            g2.drawString(previewLabel, pathCursor.x + 2, cursorTopY - 4);
            g2.setColor(hoverPreviewColor);
            g2.drawString(previewLabel, pathCursor.x + 1, cursorTopY - 5);
        }
        
        g2.setTransform(oldTransform);
    }

    private void renderSelectedObjectPath(Graphics2D g2, GameObject obj, boolean triggered){
        ArrayList<Integer> px = triggered ? obj.triggeredPathX : obj.pathX;
        ArrayList<Integer> py = triggered ? obj.triggeredPathY : obj.pathY;
        ArrayList<Double> pSpd = triggered ? obj.triggeredPathSpeed : obj.pathSpeed;
        ArrayList<Boolean> pIsSpd = triggered ? obj.triggeredPathSpeedIsSpeed : obj.pathSpeedIsSpeed;

        int startX = triggered ? obj.triggeredX : obj.currentX;
        int startY = triggered ? obj.triggeredY : obj.currentY;
        Color startBoxColor = triggered ? Color.YELLOW : Color.WHITE;

        int renderStartTopY = startY - obj.height;
        g2.setColor(new Color(startBoxColor.getRed(), startBoxColor.getGreen(), startBoxColor.getBlue(), 90));
        g2.fillRect(startX, renderStartTopY, obj.width, obj.height);
        g2.setColor(startBoxColor);
        g2.setStroke(new java.awt.BasicStroke(1.5f, java.awt.BasicStroke.CAP_BUTT, java.awt.BasicStroke.JOIN_MITER, 1.0f, new float[]{2f}, 0f));
        g2.drawRect(startX, renderStartTopY, obj.width, obj.height);
         
        if(px == null || px.isEmpty()) return;

        ArrayList<Point> points = new ArrayList<>();
        points.add(new Point(startX, startY));
        for(int i = 0; i < px.size(); i++){
            points.add(new Point(px.get(i), py.get(i)));
        }

        
        int activeIndex = (MapEditorWindow.instance != null) ? MapEditorWindow.instance.selectedWaypointIndex : -1;

        Color objColor;
        switch(obj.type){
            case solid: objColor = Color.GREEN; break;
            case oneWay: objColor = new Color(147, 112, 219); break;
            case death: objColor = Color.RED; break;
            case respawn: objColor = Color.BLUE; break;
            case spawnPoint: objColor = Color.YELLOW; break;
            case ladder: objColor = new Color(150, 75, 0); break;
            case win: objColor = new Color(212, 175, 55); break;
            case trigger: objColor = Color.ORANGE; break;
            default: objColor = Color.GRAY; break;
        }

        for(int i = 0; i < points.size() - 1; i++){
            Point p1 = points.get(i);
            Point p2 = points.get(i + 1);

            int p1CenterX = p1.x + obj.width / 2;
            int p1CenterY = (p1.y - obj.height) + obj.height / 2;
            int p2CenterX = p2.x + obj.width / 2;
            int p2CenterY = (p2.y - obj.height) + obj.height / 2;

            g2.setColor(Color.MAGENTA);
            g2.setStroke(new java.awt.BasicStroke(3f, java.awt.BasicStroke.CAP_BUTT, java.awt.BasicStroke.JOIN_MITER, 1.0f, new float[]{8f}, 0f));
            g2.drawLine(p1CenterX, p1CenterY, p2CenterX, p2CenterY);

            g2.setColor(startBoxColor);
            g2.setStroke(new java.awt.BasicStroke(1.5f, java.awt.BasicStroke.CAP_BUTT, java.awt.BasicStroke.JOIN_MITER, 1.0f, new float[]{4f}, 0f));
            g2.drawLine(p1CenterX, p1CenterY, p2CenterX, p2CenterY);

            double val = (i < pSpd.size()) ? pSpd.get(i) : 2.0;
            boolean isSpd = (i < pIsSpd.size()) ? pIsSpd.get(i) : true;
            String labelText = val + (isSpd ? " px/f" : " frames");

            int midX = (p1CenterX + p2CenterX) / 2;
            int midY = (p1CenterY + p2CenterY) / 2 - 10;

            g2.setFont(new Font("Arial", Font.BOLD, 11));
            FontMetrics fm = g2.getFontMetrics();
            int textWidth = fm.stringWidth(labelText);

            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillRect(midX - textWidth / 2 - 3, midY - fm.getHeight() + 2, textWidth + 6, fm.getHeight());
            g2.setColor(Color.CYAN);
            g2.drawString(labelText, midX - textWidth / 2, midY);

            int waypointIdx = i;
            int renderTopY = p2.y - obj.height;
            g2.setColor(new Color(objColor.getRed(), objColor.getGreen(), objColor.getBlue(), 120));
            g2.fillRect(p2.x, renderTopY, obj.width, obj.height);

            boolean isThisWaypointActive = (waypointIdx == activeIndex && triggered == MapEditorWindow.instance.selectedWaypointIsTriggered);

            if (isThisWaypointActive) {
                g2.setColor(Color.CYAN);
                g2.setStroke(new java.awt.BasicStroke(4.5f));
            } else {
                g2.setColor(startBoxColor);
                g2.setStroke(new java.awt.BasicStroke(4.5f));
            }
            g2.drawRect(p2.x, renderTopY, obj.width, obj.height);
        }
    }

    public int getTileSize(){return this.tileSize;}
    public int getPlayerSize() {return (int) (this.tileSize * 0.75);}
    public ArrayList<GameObject> getGameObjects(){return this.gameObjects;}
    public boolean getOriginalEditorModeStatus(){return this.isEditorMode;}
    public boolean isEditorMode(){return this.isEditorMode;}
    public Movement getMovement(){ return this.movement; }
    public int getCurrentMap(){return currentMap;}
}