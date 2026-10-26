package src.game;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class MapEditorWindow extends JFrame{

    private final String externalSavePath = "src/levels.json";
    
    private Graphics gameCanvas;
    private JPanel controlPanel;


    //making/deleting objects
    private GameObject selectedObject = null;
    private Point bottomLeftCorner = null;
    private Point topRightCorner = null;
    private Point currentHoverPoint = null;


    private GameObject.Type activeBrushType = GameObject.Type.solid;
    public int activeEditingMapID = 1;
    private int currentlyLoadedMapID = -1;

    //labels
    private JLabel lblX, lblY, lblWidth, lblHeight, lblBrush, lblSelectedInfo, lblFreePlace;
    private JTextField txtMapID;
    private JButton btnDelete;

    private JPanel mainContentCardContainer;
    private CardLayout windowLayoutSwitcher;
    private JPanel levelGridMenuPanel;

    private boolean freePlace;
    private JButton btnPlay = new JButton("Play");
    private JButton btnExit = new JButton("Exit to Editor");


    //moving camera around
    public int cameraX = 0;
    public int cameraY = 0;
    private final int cameraSpeed = 16;
    private boolean camLeft = false, camRight = false, camUp = false, camDown = false;
    private Timer editorCamTimer;
    public static MapEditorWindow instance;


    public MapEditorWindow(){
        instance = this;

        setTitle("Platformer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        windowLayoutSwitcher = new CardLayout();
        mainContentCardContainer = new JPanel(windowLayoutSwitcher);

        gameCanvas = new Graphics();
        gameCanvas.setOpaque(true);
        add(gameCanvas, BorderLayout.CENTER);

        levelGridMenuPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 15, 15));
        levelGridMenuPanel.setBackground(Color.DARK_GRAY);
        JScrollPane menuScrollPane = new JScrollPane(levelGridMenuPanel);
        menuScrollPane.setBorder(null);

        mainContentCardContainer.add(gameCanvas, "GAME_CANVAS");
        mainContentCardContainer.add(menuScrollPane, "LEVEL_MENU");

        add(mainContentCardContainer, BorderLayout.CENTER);


        createControlBar();

        setupEditorClickEngine();

        editorCamTimer = new Timer(16, e ->{
            if(gameCanvas != null && gameCanvas.getOriginalEditorModeStatus()) {
                int currentX = gameCanvas.getCameraX();
                int currentY = gameCanvas.getCameraY();

                if (camLeft)  currentX -= cameraSpeed;
                if (camRight) currentX += cameraSpeed;
                if (camUp)    currentY -= cameraSpeed;
                if (camDown)  currentY += cameraSpeed;

                if (currentX < 0) currentX = 0;
                if (currentY < 0) currentY = 0;
                
                gameCanvas.setCameraX(currentX);
                gameCanvas.setCameraY(currentY);
                gameCanvas.repaint();
            }
        });
        editorCamTimer.start();

        pack();
        setLocationRelativeTo(null);
        revalidate();
        repaint();

        setVisible(true); 

        gameCanvas.requestFocusInWindow();
    }
    
    public static void win() {
        if (instance == null) return;

        // Use our instance proxy to alter UI components safely from a static context
        instance.gameCanvas.setEditorMode(true);
        instance.gameCanvas.setCameraX(0);
        instance.gameCanvas.setCameraY(0);
        
        instance.btnPlay.setEnabled(true);
        instance.btnExit.setEnabled(false);

        JOptionPane.showMessageDialog(instance, "Level Complete! You Win!", "Victory!", JOptionPane.INFORMATION_MESSAGE);
    }

    private void createControlBar(){
        controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        controlPanel.setBackground(new Color(240, 240, 240));
        
        Dimension sideBarDimensions = new Dimension(240, 576);
        controlPanel.setPreferredSize(sideBarDimensions);
        controlPanel.setMinimumSize(sideBarDimensions);
        controlPanel.setMaximumSize(sideBarDimensions);


        lblBrush = new JLabel("Selected Brush: SOLID");
        lblBrush.setFont(new Font("Arial", Font.BOLD, 12));
        lblBrush.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        lblX = new JLabel("X Offset: --");
        lblX.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblY = new JLabel("Y Offset: --");
        lblY.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblWidth = new JLabel("Width Status: --");
        lblWidth.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblHeight = new JLabel("Height Status: --");
        lblHeight.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblSelectedInfo = new JLabel("Selection: none");
        lblSelectedInfo.setForeground(Color.BLUE);
        lblSelectedInfo.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        lblFreePlace = new JLabel("Selection mode: Grid");
        lblFreePlace.setForeground(Color.BLUE);
        lblFreePlace.setAlignmentX(Component.LEFT_ALIGNMENT);

        controlPanel.add(lblBrush);
        controlPanel.add(Box.createVerticalStrut(5));
        controlPanel.add(lblX);
        controlPanel.add(lblY);
        controlPanel.add(lblWidth);
        controlPanel.add(lblHeight);
        controlPanel.add(Box.createVerticalStrut(15));
        controlPanel.add(lblSelectedInfo);
        controlPanel.add(Box.createVerticalStrut(5));
        controlPanel.add(lblFreePlace);
        controlPanel.add(Box.createVerticalStrut(15));

        Dimension enumBtnSize = new Dimension(200,28);
        for(GameObject.Type type : GameObject.Type.values()){
            JButton btnType = new JButton("Select " + type.name());
            btnType.setMaximumSize(enumBtnSize);
            btnType.setAlignmentX(Component.LEFT_ALIGNMENT);
            btnType.addActionListener(e ->{
                activeBrushType = type;
                lblBrush.setText("Selected Brush: "+ type.name().toUpperCase());
            });
            controlPanel.add(btnType);
            controlPanel.add(Box.createVerticalStrut(5));
        }

        controlPanel.add(Box.createVerticalStrut(15));

        btnDelete = new JButton("delete selected object");
        btnDelete.setMaximumSize(new Dimension(200,32));
        btnDelete.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnDelete.setEnabled(false);

        btnDelete.addActionListener(e -> {
            if(selectedObject != null && gameCanvas.getGameObjects() != null){
                gameCanvas.getGameObjects().remove(selectedObject);
                selectedObject = null;
                lblSelectedInfo.setText("selection: Delete");
                btnDelete.setEnabled(false);
                gameCanvas.repaint();
            }
        });
        controlPanel.add(btnDelete);
        controlPanel.add(Box.createVerticalStrut(15));

        JPanel idPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        idPanel.setBackground(new Color(240, 240, 240));
        idPanel.setMaximumSize(new Dimension(200, 35));
        idPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        idPanel.add(new JLabel("Map ID: "));
        txtMapID = new JTextField("1", 4);
        idPanel.add(txtMapID);
        controlPanel.add(idPanel);

        controlPanel.add(Box.createVerticalStrut(15));
        
        JButton btnSave = new JButton("Save Level");
        JButton btnLoad = new JButton("Load stage");
        JButton btnFreePlace = new JButton("Free place");

        Dimension btnSize = new Dimension(200, 32);
        JButton[] actionButtons = {btnSave, btnPlay, btnExit, btnLoad, btnFreePlace};
        for (JButton btn : actionButtons) {
            btn.setMaximumSize(btnSize);
            btn.setAlignmentX(Component.LEFT_ALIGNMENT);
            controlPanel.add(btn);
            controlPanel.add(Box.createVerticalStrut(4));
        }
        btnExit.setEnabled(false);

        btnLoad.addActionListener(e -> {
            try {
                activeEditingMapID = Integer.parseInt(txtMapID.getText().trim());
                loadMapManual(externalSavePath);
                gameCanvas.loadLevel(activeEditingMapID); // Updates camera references
                currentlyLoadedMapID = activeEditingMapID;
                JOptionPane.showMessageDialog(this, "Map " + activeEditingMapID + " loaded successfully!");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Error: Invalid Map ID format.");
            }
        });

        btnSave.addActionListener(e -> {
            try {
                int targetSaveID = Integer.parseInt(txtMapID.getText().trim());

                if(currentlyLoadedMapID == -1){
                    JOptionPane.showMessageDialog(this, "ERROR: can't save, you must load a level to save", "Editor Safety Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if(targetSaveID != currentlyLoadedMapID){
                    JOptionPane.showMessageDialog(this, "ERROR: You are trying to save to Map ID " + targetSaveID + ", but you currently have Map ID " + currentlyLoadedMapID + " loaded\n"+"To edit Map " + targetSaveID + ", load it first to prevet overwriting data", "Editor Safety Error", JOptionPane.ERROR_MESSAGE);
                }

                activeEditingMapID = targetSaveID;
                saveMapManual(gameCanvas.getGameObjects(), externalSavePath);
                JOptionPane.showMessageDialog(this, "Level "+ activeEditingMapID +" changes successfully saved");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Error: Invalid numeric input format passed for Map ID");
            }
        });

        btnPlay.addActionListener(e->{
            try{
                activeEditingMapID = Integer.parseInt(txtMapID.getText().trim());
                gameCanvas.setEditorMode(false);
                gameCanvas.loadLevel(activeEditingMapID);
                btnPlay.setEnabled(false);
                btnExit.setEnabled(true);
                gameCanvas.requestFocusInWindow();
            }catch (Exception ex){
                // System.out.println("Loader sequence halted");
            }
        });

        btnExit.addActionListener(e -> {
            gameCanvas.setEditorMode(true);
            
            // gameCanvas.setCameraX(0);
            // gameCanvas.setCameraY(0);
            btnPlay.setEnabled(true);
            btnExit.setEnabled(false);
        });

        btnFreePlace.addActionListener(e -> {
            try{
                freePlace = !freePlace;
                if(freePlace){
                    lblFreePlace.setText("Selection mode: Free Place");
                }else{
                    lblFreePlace.setText("Selection mode: Grid");
                }
            }catch (Exception ex){
                // System.out.println("Cannot go into freeplace mode");
            }
        });

        JButton btnSelectLevelMenu = new JButton("Browse Levels UI");
        btnSelectLevelMenu.setMaximumSize(btnSize);
        btnSelectLevelMenu.setAlignmentX(Component.LEFT_ALIGNMENT);
        controlPanel.add(btnSelectLevelMenu);
        controlPanel.add(Box.createVerticalStrut(5));

        btnSelectLevelMenu.addActionListener(e ->{
            levelGridMenuPanel.removeAll();

            java.util.Set<Integer> registeredMaps = new java.util.HashSet<>();
            if(gameCanvas.getGameObjects() != null){
                for(GameObject obj : gameCanvas.getGameObjects()){
                    registeredMaps.add(obj.mapID);
                }
            }
            if(registeredMaps.isEmpty()){
                registeredMaps.add(1);
            }
            for(int id :registeredMaps){
                LevelPreview preview = new LevelPreview(id, this, gameCanvas);
                levelGridMenuPanel.add(preview);
            }
            levelGridMenuPanel.revalidate();
            levelGridMenuPanel.repaint();

            windowLayoutSwitcher.show(mainContentCardContainer, "LEVEL_MENU");
        });

        JScrollPane scrollPaneWrapper = new JScrollPane(controlPanel);
        scrollPaneWrapper.setPreferredSize(sideBarDimensions);
        scrollPaneWrapper.setMinimumSize(sideBarDimensions);
        scrollPaneWrapper.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.LIGHT_GRAY));
        
        add(scrollPaneWrapper, BorderLayout.EAST);
    }
    
    private Point getTransformedGamePoint(MouseEvent e){
        int panelWidth = gameCanvas.getWidth();
        int panelHeight = gameCanvas.getHeight();
        int virtualWidth = gameCanvas.virtualWidth;
        int virtualHeight = gameCanvas.virtualHeight;

        double scaleX = (double) panelWidth / virtualWidth;
        double scaleY = (double) panelHeight / virtualHeight;
        double scale = Math.min(scaleX, scaleY);

        int offsetX = (int) ((panelWidth - (virtualWidth * scale)) / 2);
        int offsetY = (int) ((panelHeight - (virtualHeight * scale)) / 2);

        int virtualMouseX = (int) ((e.getX() - offsetX) / scale);
        int virtualMouseY = (int) ((e.getY() - offsetY) / scale);

        return new Point(virtualMouseX + gameCanvas.getCameraX(), virtualMouseY + gameCanvas.getCameraY());
    }
    
    private void setupEditorClickEngine(){
        if(gameCanvas == null){
            return;
        }
        gameCanvas.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    bottomLeftCorner = null;
                    currentHoverPoint = null;
                    gameCanvas.repaint();
                }
            }
        });
        gameCanvas.addMouseListener(new MouseAdapter(){
            @Override
            public void mousePressed(MouseEvent e){

                gameCanvas.requestFocusInWindow();

                Point gamePoint = getTransformedGamePoint(e);

                if (gameCanvas == null || !gameCanvas.isEditorMode()) return;
                int snapSize = gameCanvas.getTileSize();
                int snapX;
                int snapY;
                if(!freePlace){
                    snapX = Math.floorDiv(gamePoint.x, snapSize) * snapSize;
                    snapY = Math.floorDiv(gamePoint.y, snapSize) * snapSize;
                }else{
                    snapX = gamePoint.x;
                    snapY = gamePoint.y;
                }

                if(SwingUtilities.isLeftMouseButton(e) || SwingUtilities.isRightMouseButton(e)){
                    if(bottomLeftCorner == null){
                        int finalY = freePlace ? snapY : (snapY + snapSize);
                        bottomLeftCorner = new Point(snapX, finalY);
                        lblX.setText("X Offset:" + snapX);
                        
                        int currentRightX = snapX + (freePlace? 0 : snapSize);
                        currentHoverPoint = new Point(currentRightX, snapY);
                        gameCanvas.repaint();
                    }else{
                        int currentRightX = snapX + (freePlace ? 0 : snapSize);
                
                        try { activeEditingMapID = Integer.parseInt(txtMapID.getText().trim()); } catch (NumberFormatException ex) { activeEditingMapID = 1; }
                        
                        int realX = Math.min(bottomLeftCorner.x, currentRightX);
                        int realY = Math.min(bottomLeftCorner.y, snapY);
                        int realWidth = Math.abs(currentRightX - bottomLeftCorner.x);
                        int realHeight = Math.abs(bottomLeftCorner.y - snapY);

                        if (realWidth == 0) realWidth = snapSize;
                        if (realHeight == 0) realHeight = snapSize;

                        if (gameCanvas.getGameObjects() != null) {
                            gameCanvas.getGameObjects().add(new GameObject(realX, realY, realWidth, realHeight, activeBrushType, activeEditingMapID));
                        }

                        // Reset parameters cleanly for the next placement
                        bottomLeftCorner = null;
                        currentHoverPoint = null;
                        gameCanvas.repaint();
                    }
                }
                    
                if(SwingUtilities.isMiddleMouseButton(e)){
                    if(bottomLeftCorner != null){
                        bottomLeftCorner = null;
                        currentHoverPoint = null;
                        gameCanvas.repaint();
                        return;
                    }
                    selectedObject = null;
                    btnDelete.setEnabled(false);
                    lblSelectedInfo.setText("Selection: None");

                    ArrayList<GameObject> allObjects = gameCanvas.getGameObjects();
                    if(allObjects != null){
                        for(int i = allObjects.size() - 1; i>= 0; i--){
                            GameObject obj = allObjects.get(i);
                            try{
                                activeEditingMapID = Integer.parseInt(txtMapID.getText().trim());
                            }catch(Exception ex){
                                activeEditingMapID = 1;
                            }
                            if(obj.mapID == activeEditingMapID && gamePoint.x >= obj.x && gamePoint.x <= (obj.x + obj.width) && gamePoint.y >= obj.y && gamePoint.y <= (obj.y + obj.height)){
                                selectedObject = obj;

                                activeBrushType = obj.type;
                                lblBrush.setText("Selected Brush: " + obj.type.name().toUpperCase());
                                lblSelectedInfo.setText(String.format("Selection: %s (%dx%d)", obj.type.name(), obj.width, obj.height));
                                btnDelete.setEnabled(true);
                                break;
                            }
                        }
                    }
                    return;
                }
            }
        });
        gameCanvas.addMouseMotionListener(new MouseMotionAdapter(){
            @Override 
            public void mouseMoved(MouseEvent e){
                if(gameCanvas == null || !gameCanvas.isEditorMode() || bottomLeftCorner == null) return;
                Point gamePoint = getTransformedGamePoint(e);
                int snapSize = gameCanvas.getTileSize();
                int snapX = !freePlace ? Math.floorDiv(gamePoint.x, snapSize) * snapSize : gamePoint.x;
                int snapY = !freePlace ? Math.floorDiv(gamePoint.y, snapSize) * snapSize : gamePoint.y;

                int currentRightX = snapX + (freePlace ? 0 : snapSize);
                currentHoverPoint = new Point(currentRightX, snapY);

                int realWidth = Math.abs(currentHoverPoint.x - bottomLeftCorner.x);
                int realHeight = Math.abs(bottomLeftCorner.y - currentHoverPoint.y);
                lblWidth.setText("Width Status: " + (realWidth == 0 ? snapSize : realWidth));
                lblHeight.setText("Height Status: " + (realHeight == 0 ? snapSize : realHeight));
                gameCanvas.repaint();

            }
        });
    }

    
    private void saveMapManual(ArrayList<GameObject> currentEditingObjects, String filePath){
        ArrayList<GameObject> masterList = new ArrayList<>();
        java.io.File file = new java.io.File(filePath);
        String json = "";

        if(file.exists()){
            try(java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))){
                StringBuilder content = new StringBuilder();
                String line;
                while((line = reader.readLine()) != null){
                    content.append(line);
                }
                json = content.toString().trim();
            }catch(Exception e){
                // System.err.println("");
            }
        }else{
            try(java.io.InputStream is = getClass().getResourceAsStream("/levels.json")){
                if(is != null){
                    try(java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8))){
                        StringBuilder content = new StringBuilder();
                        String line;
                        while((line = reader.readLine()) != null){
                            content.append(line);
                        }
                        json = content.toString().trim();
                    }
                }
            }catch(Exception e){
                // System.err.println("No backup map layout found");
            }
        }

        if(!json.isEmpty()){
            if(json.startsWith("[") && json.endsWith("]")){
                json = json.substring(1, json.length() - 1).trim();
            }

            if(!json.isEmpty()){
                String[] objectsRaw = json.split("\\},\\s*\\{");
                for(String rawObj: objectsRaw){
                    rawObj = rawObj.replace("{", "").replace("}", "").trim();
                    int x = 0, y = 0, width = 0, height = 0, mapID = 1;
                    GameObject.Type type = GameObject.Type.solid;
                    String[] properties = rawObj.split(",");
                    for(String prop : properties){

                        String[] kv = prop.split(":");
                        if(kv.length != 2) continue;

                        String key = kv[0].replace("\"", "").trim();
                        String val = kv[1].replace("\"", "").trim();
                        switch(key){
                            case "x": x = Integer.parseInt(val); break;
                            case "y": y = Integer.parseInt(val); break;
                            case "width": width = Integer.parseInt(val); break;
                            case "height": height = Integer.parseInt(val); break;
                            case "mapID": mapID = Integer.parseInt(val); break;
                            case "type": type = GameObject.Type.valueOf(val); break;
                        }
                    }
                    masterList.add(new GameObject(x, y, width, height, type, mapID));
                }
            }
        }
        masterList.removeIf(obj -> obj.mapID == activeEditingMapID);

        for(GameObject activeObj : currentEditingObjects){
            if(activeObj.mapID == activeEditingMapID){
                masterList.add(activeObj);
            }
        }

        try(java.io.FileWriter writer = new java.io.FileWriter(filePath)){
            writer.write("[\n");
            for(int i = 0; i < masterList.size(); i++){
                GameObject obj = masterList.get(i);
                String jsonStr = String.format("{\"x\":%d, \"y\":%d, \"width\":%d, \"height\":%d, \"type\":\"%s\", \"mapID\":%d}", obj.x, obj.y, obj.width, obj.height, obj.type.name(), obj.mapID);
                writer.write(" "+ jsonStr);
                if(i < masterList.size() - 1){
                    writer.write(",\n");
                }else{
                    writer.write("\n");
                }
            }
            writer.write("]");
        }catch (java.io.IOException e){
            // System.err.println("error");
        }
    }
    
    private void loadMapManual(String filePath) {
    ArrayList<GameObject> loadedObjects = new ArrayList<>();
    java.io.File file = new java.io.File(filePath);
    String jsonStringData = "";

    // 1. Read the data from either the hard drive or inside the JAR
    if (file.exists()) {
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
            jsonStringData = content.toString().trim();
        } catch (Exception e) {
            // System.err.println("Error reading external map file.");
        }
    } else {
        // Fallback: Read the raw template map embedded inside your JAR binary
        try (java.io.InputStream is = getClass().getResourceAsStream("/levels.json")) {
            if (is != null) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8))) {
                    StringBuilder content = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line);
                    }
                    jsonStringData = content.toString().trim();
                }
            }
        } catch (Exception e) {
            // System.err.println("Error reading internal map template asset.");
        }
    }

    // 2. Parse using layout engine
    if (!jsonStringData.isEmpty()) {
        if (jsonStringData.startsWith("[") && jsonStringData.endsWith("]")) {
            jsonStringData = jsonStringData.substring(1, jsonStringData.length() - 1).trim();
        }

        if (!jsonStringData.isEmpty()) {
            String[] objectsRaw = jsonStringData.split("\\},\\s*\\{");
            for (String rawObj : objectsRaw) {
                rawObj = rawObj.replace("{", "").replace("}", "").trim();
                int x = 0, y = 0, width = 0, height = 0, mapID = 1;
                GameObject.Type type = GameObject.Type.solid;
                String[] properties = rawObj.split(",");
                for (String prop : properties) {
                    String[] kv = prop.split(":");
                    if (kv.length != 2) continue;

                    String key = kv[0].replace("\"", "").trim();
                    String val = kv[1].replace("\"", "").trim();
                    switch (key) {
                        case "x": x = Integer.parseInt(val); break;
                        case "y": y = Integer.parseInt(val); break;
                        case "width": width = Integer.parseInt(val); break;
                        case "height": height = Integer.parseInt(val); break;
                        case "mapID": mapID = Integer.parseInt(val); break;
                        case "type": type = GameObject.Type.valueOf(val); break;
                    }
                }
                loadedObjects.add(new GameObject(x, y, width, height, type, mapID));
            }
        }
    }

    // 3. Clear canvas list and assign the fresh objects
    if (gameCanvas.getGameObjects() != null) {
        gameCanvas.getGameObjects().clear();
        gameCanvas.getGameObjects().addAll(loadedObjects);
        gameCanvas.repaint();
    }
}

    public void clearSelectionPoints() {
        this.bottomLeftCorner = null;
        this.topRightCorner = null;
        this.selectedObject = null;
        if (this.btnDelete != null) this.btnDelete.setEnabled(false);
        if (this.gameCanvas != null) this.gameCanvas.repaint();
    }

    public void updateMapIDField(String text){
        if(txtMapID != null) txtMapID.setText(text);
    }
    public void setCurrentlyLoadedMapID(int id){
        this.currentlyLoadedMapID = id;
    }
    public void restoreGameView(){
        if(windowLayoutSwitcher != null && mainContentCardContainer != null){
            windowLayoutSwitcher.show(mainContentCardContainer, "GAME_CANVAS");
            gameCanvas.requestFocusInWindow();
        }
    }

    public int getCameraX(){return this.cameraX;}
    public int getCameraY(){return this.cameraY;}
    public void setCamLeft(boolean state) { this.camLeft = state; }
    public void setCamRight(boolean state) { this.camRight = state; }
    public void setCamUp(boolean state) { this.camUp = state; }
    public void setCamDown(boolean state) { this.camDown = state; }

    public Point getBottomLeftCorner(){return this.bottomLeftCorner;}
    public Point getCurrentHoverPoint(){return this.currentHoverPoint;}
    public GameObject.Type getActiveBrushType(){ return this.activeBrushType;}

}