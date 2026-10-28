package src.game;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class MapEditorWindow extends JFrame{

    private final String externalSavePath = "src/levels.json";
    
    public Graphics gameCanvas;
    private JPanel controlPanel;


    //making/deleting objects
    private GameObject selectedObject = null;
    private Point bottomLeftCorner = null;
    private Point currentHoverPoint = null;
    private Point pathHoverPoint = null;

    private GameObject propertyEditingObject = null;
    private int highlightedBroadcastChannel = 0;

    private JDialog propertyDialog = null;
    private JTextField txtTriggeredBy, txtActivatesChannel;
    private JToggleButton btnToggleDisplay;

    private JTextField txtTimeLimit;
    private enum PathClickMode { NONE, OSCILLATION, TRIGGERED_MOVE, OSCILLATE_AFTER_TRIGGER}
    private PathClickMode currentPathClickMode = PathClickMode.NONE;



    private GameObject.Type activeBrushType = GameObject.Type.solid;
    public int activeEditingMapID = 1;
    private int currentlyLoadedMapID = -1;

    //labels
    private JLabel lblX, lblY, lblWidth, lblHeight, lblBrush, lblSelectedInfo, lblFreePlace;
    private JTextField txtMapID;
    private JButton btnDelete;
    private JButton btnEditProperties;

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

        try{
            loadMapManual(externalSavePath);
            gameCanvas.loadLevel(activeEditingMapID);
            currentlyLoadedMapID = activeEditingMapID;
            if(gameCanvas.getMovement() != null){
                gameCanvas.getMovement().resetTriggerTrackingState();
            }
        }catch(Exception ex){

        }

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
        JButton btnNew = new JButton("New map");
        JButton btnSelectLevelMenu = new JButton("Browse Levels");

        Dimension btnSize = new Dimension(200, 32);
        JButton[] actionButtons = {btnSave, btnPlay, btnExit, btnLoad, btnFreePlace, btnNew, btnSelectLevelMenu};
        for (JButton btn : actionButtons) {
            btn.setMaximumSize(btnSize);
            btn.setAlignmentX(Component.LEFT_ALIGNMENT);
            controlPanel.add(btn);
            controlPanel.add(Box.createVerticalStrut(4));
        }
        btnExit.setEnabled(false);

        btnLoad.addActionListener(e -> {
            try {
                if (gameCanvas.getMovement() != null) {
                    gameCanvas.getMovement().resetTriggerTrackingState();
                }
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
                if (gameCanvas.getMovement() != null) {
                    gameCanvas.getMovement().resetTriggerTrackingState();
                }
                int targetSaveID = Integer.parseInt(txtMapID.getText().trim());

                if(currentlyLoadedMapID == -1){
                    JOptionPane.showMessageDialog(this, "ERROR: can't save, you must load a level to save", "Editor Safety Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if(targetSaveID != currentlyLoadedMapID){
                    JOptionPane.showMessageDialog(this, "ERROR: You are trying to save to Map ID " + targetSaveID + ", but you currently have Map ID " + currentlyLoadedMapID + " loaded\n"+"To edit Map " + targetSaveID + ", load it first to prevet overwriting data", "Editor Safety Error", JOptionPane.ERROR_MESSAGE);
                }
                ArrayList<GameObject> liveObjects = gameCanvas.getGameObjects();
                if (liveObjects != null) {
                    for (GameObject obj : liveObjects) {
                        obj.isTriggered = false;
                    }
                }
                gameCanvas.repaint();

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
                btnSave.setEnabled(false);
                btnLoad.setEnabled(false);
                btnFreePlace.setEnabled(false);
                btnNew.setEnabled(false);
                btnSelectLevelMenu.setEnabled(false);
                gameCanvas.requestFocusInWindow();
                gameCanvas.getMovement().resetTriggerTrackingState();
            }catch (Exception ex){
                // System.out.println("Loader sequence halted");
            }
        });

        btnExit.addActionListener(e -> {
            if (gameCanvas.getMovement() != null) {
                gameCanvas.getMovement().resetTriggerTrackingState();
            }
            gameCanvas.setEditorMode(true);
            ArrayList<GameObject> allObjects = gameCanvas.getGameObjects();
            if(allObjects != null){
                for(GameObject obj : allObjects){
                    obj.isTriggered = false;
                    obj.timeElapsed = 0;
                    obj.currentX = obj.x;
                    obj.currentY = obj.y;
                    obj.lastX = obj.x;
                    obj.lastY = obj.y;
                    obj.deltaX = 0;
                    obj.deltaY = 0;
                }
            }
            
            // gameCanvas.setCameraX(0);
            // gameCanvas.setCameraY(0);
            btnPlay.setEnabled(true);
            btnExit.setEnabled(false);
            btnSave.setEnabled(true);
            btnLoad.setEnabled(true);
            btnFreePlace.setEnabled(true);
            btnNew.setEnabled(true);
            btnSelectLevelMenu.setEnabled(true);
            gameCanvas.repaint();
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

        btnNew.addActionListener(e -> {
            int nextID = getNextUnusedMapID();
            activeEditingMapID = nextID;
            currentlyLoadedMapID = nextID;
            txtMapID.setText(String.valueOf(nextID));
            gameCanvas.setCameraX(0);
            gameCanvas.setCameraY(0);
            loadMapManual(externalSavePath);
            gameCanvas.loadLevel(activeEditingMapID);
            gameCanvas.repaint();
            JOptionPane.showMessageDialog(this, "New Map " + activeEditingMapID + " created succesfully");
        });

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
                if(propertyEditingObject != null){
                    if(SwingUtilities.isLeftMouseButton(e) || SwingUtilities.isRightMouseButton(e)){
                        if (currentPathClickMode == PathClickMode.OSCILLATION) {
                            // Route 1: Constant/Standard background looping limits
                            propertyEditingObject.moveX = snapX;
                            propertyEditingObject.moveY = snapY;
                            currentPathClickMode = PathClickMode.NONE;
                            JOptionPane.showMessageDialog(propertyDialog, "Standard Oscillation Target set to: (" + snapX + ", " + snapY + ")");
                            
                        } else if (currentPathClickMode == PathClickMode.TRIGGERED_MOVE) {
                            // Route 2: Single-activation baseline slide point destination 
                            propertyEditingObject.triggeredX = snapX;
                            propertyEditingObject.triggeredY = snapY;
                            currentPathClickMode = PathClickMode.NONE;
                            JOptionPane.showMessageDialog(propertyDialog, "Triggered Movement Target set to: (" + snapX + ", " + snapY + ")");
                            
                        } else if (currentPathClickMode == PathClickMode.OSCILLATE_AFTER_TRIGGER) {
                            // Route 3: Secondary looping boundaries configuration parameters
                            // This allows you to set moveX/Y relative to where it arrives when triggered
                            propertyEditingObject.triggerMoveX = snapX;
                            propertyEditingObject.triggerMoveY = snapY;
                            currentPathClickMode = PathClickMode.NONE;
                            JOptionPane.showMessageDialog(propertyDialog, "Post-Trigger Oscillation Target set to: (" + snapX + ", " + snapY + ")");
                            
                        } else {
                            // Default Fallback
                            propertyEditingObject.triggeredX = snapX;
                            propertyEditingObject.triggeredY = snapY;
                        }
                        gameCanvas.repaint();
                        return;
                    }
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

                        lblWidth.setText("Width Status: " + realWidth);
                    lblHeight.setText("Height Status: " + realHeight);
                    lblX.setText("X Offset: " + realX);
                    lblY.setText("Y Offset: " + realY);
                    
                    if (gameCanvas.getGameObjects() != null) {
                        // Create the object configuration block
                        GameObject newlyCreatedObject = new GameObject(realX, realY, realWidth, realHeight, activeBrushType, activeEditingMapID);
                        gameCanvas.getGameObjects().add(newlyCreatedObject);
                        showPropertyConfigDialog(newlyCreatedObject);
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

                    propertyEditingObject = null;
                    highlightedBroadcastChannel = 0;

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

                                propertyEditingObject = obj;

                                if(obj.type == GameObject.Type.trigger){
                                    highlightedBroadcastChannel = obj.activatesChannel;
                                }

                                activeBrushType = obj.type;
                                lblBrush.setText("Selected Brush: " + obj.type.name().toUpperCase());
                                lblSelectedInfo.setText(String.format("Selection: %s (%dx%d)", obj.type.name(), obj.width, obj.height));
                                btnDelete.setEnabled(true);
                                break;
                            }
                        }
                    }
                    gameCanvas.repaint();
                    return;
                }
            }
        });
        gameCanvas.addMouseMotionListener(new MouseMotionAdapter(){
            @Override 
            public void mouseMoved(MouseEvent e){
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

                int rawX = virtualMouseX + gameCanvas.getCameraX();
                int rawY = virtualMouseY + gameCanvas.getCameraY();

                int snapSize = gameCanvas.getTileSize();
                int snapX = !freePlace ? Math.floorDiv(rawX, snapSize) * snapSize : rawX;
                int snapY = !freePlace ? Math.floorDiv(rawY, snapSize) * snapSize : rawY;

                //Record path hover coordinate changes
                if (propertyEditingObject != null && currentPathClickMode != PathClickMode.NONE) {
                    pathHoverPoint = new Point(snapX, snapY);
                } else {
                    pathHoverPoint = null;
                }

                if (bottomLeftCorner != null) {
                    int currentRightX = snapX + (freePlace ? 0 : snapSize);
                    currentHoverPoint = new Point(currentRightX, snapY);
                    int realWidth = Math.abs(currentHoverPoint.x - bottomLeftCorner.x);
                    int realHeight = Math.abs(bottomLeftCorner.y - currentHoverPoint.y);
                    lblWidth.setText("Width Status: " + (realWidth == 0 ? snapSize : realWidth));
                    lblHeight.setText("Height Status: " + (realHeight == 0 ? snapSize : realHeight));
                }
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
                    boolean isTriggered= false, triggeredDisplay = true;
                    int triggeredX = -1, triggeredY = -1, triggeredBy = 0, activatesChannel = 0;
                    int time = 0, timeElapsed = 0, moveX = 0, moveY = 0, triggerMoveX = 0, triggerMoveY = 0;
                    GameObject.Type type = GameObject.Type.solid;
                    GameObject.movement move = GameObject.movement.never;

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
                            case "isTriggered": isTriggered = Boolean.parseBoolean(val); break;
                            case "triggeredX": triggeredX = Integer.parseInt(val); break;
                            case "triggeredY": triggeredY = Integer.parseInt(val); break;
                            case "triggeredDisplay": triggeredDisplay = Boolean.parseBoolean(val); break;
                            case "triggeredBy": triggeredBy = Integer.parseInt(val); break;
                            case "activatesChannel": activatesChannel = Integer.parseInt(val); break;
                            case "time": time = Integer.parseInt(val); break;
                            case "timeElapsed": timeElapsed = Integer.parseInt(val); break;
                            case "move": move = GameObject.movement.valueOf(val); break;
                            case "moveX": moveX = Integer.parseInt(val); break;
                            case "moveY": moveY = Integer.parseInt(val); break;
                            case "triggerMoveX": triggerMoveX = Integer.parseInt(val); break;
                            case "triggerMoveY": triggerMoveY = Integer.parseInt(val); break;

                        }
                    }
                    if(triggeredX == -1 && triggeredY == -1){ triggeredX = x; triggeredY = y; }
                    if (moveX == 0 && moveY == 0) { moveX = x; moveY = y; }
                    if (triggerMoveX == 0 && triggerMoveY == 0) { triggerMoveX = triggeredX; triggerMoveY = triggeredY; }

                     masterList.add(new GameObject(x, y, width, height, type, mapID, isTriggered, triggeredX, triggeredY, 
                        triggeredDisplay, triggeredBy, activatesChannel, time, timeElapsed, move, moveX, moveY, triggerMoveX, triggerMoveY, x, y));
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
                String jsonStr = String.format(
                    "{\"x\":%d, \"y\":%d, \"width\":%d, \"height\":%d, \"type\":\"%s\", \"mapID\":%d, \"isTriggered\":%b, " +
                    "\"triggeredX\":%d, \"triggeredY\":%d, \"triggeredDisplay\":%b, \"triggeredBy\":%d, \"activatesChannel\":%d, " +
                    "\"time\":%d, \"timeElapsed\":%d, \"move\":\"%s\", \"moveX\":%d, \"moveY\":%d, \"triggerMoveX\":%d, \"triggerMoveY\":%d}", 
                    obj.x, obj.y, obj.width, obj.height, obj.type.name(), obj.mapID, obj.isTriggered, 
                    obj.triggeredX, obj.triggeredY, obj.triggeredDisplay, obj.triggeredBy, obj.activatesChannel,
                    obj.time, obj.timeElapsed, obj.move.name(), obj.moveX, obj.moveY, obj.triggerMoveX, obj.triggerMoveY
                );
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
                boolean isTriggered = false, triggeredDisplay = true;
                int triggeredX = -1, triggeredY = -1, triggeredBy = 0, activatesChannel = 0;
                int time = 0, timeElapsed = 0, moveX = 0, moveY = 0, triggerMoveX = 0, triggerMoveY = 0;
                GameObject.Type type = GameObject.Type.solid;
                GameObject.movement move = GameObject.movement.never;



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
                        case "isTriggered": isTriggered = Boolean.parseBoolean(val); break;
                        case "triggeredX": triggeredX = Integer.parseInt(val); break;
                        case "triggeredY": triggeredY = Integer.parseInt(val); break;
                        case "triggeredDisplay": triggeredDisplay = Boolean.parseBoolean(val); break;
                        case "triggeredBy": triggeredBy = Integer.parseInt(val); break;
                        case "activatesChannel": activatesChannel = Integer.parseInt(val); break;
                        case "time": time = Integer.parseInt(val); break;
                        case "timeElapsed": timeElapsed = Integer.parseInt(val); break;
                        case "move": move = GameObject.movement.valueOf(val); break;
                        case "moveX": moveX = Integer.parseInt(val); break;
                        case "moveY": moveY = Integer.parseInt(val); break;
                        case "triggerMoveX": triggerMoveX = Integer.parseInt(val); break;
                        case "triggerMoveY": triggerMoveY = Integer.parseInt(val); break;
                    }
                }
                if (triggeredX == -1 && triggeredY == -1) { triggeredX = x; triggeredY = y; }
                if (moveX == 0 && moveY == 0) { moveX = x; moveY = y; }
                if (triggerMoveX == 0 && triggerMoveY == 0) { triggerMoveX = triggeredX; triggerMoveY = triggeredY; }
                loadedObjects.add(new GameObject(x, y, width, height, type, mapID, isTriggered, triggeredX, triggeredY,
                    triggeredDisplay, triggeredBy, activatesChannel, time, timeElapsed, move, moveX, moveY, triggerMoveX, triggerMoveY, x, y));
            }
        }
    }

    // 3. Clear canvas list and assign the fresh objects
    if (gameCanvas.getGameObjects() != null) {
        gameCanvas.getGameObjects().clear();
        gameCanvas.getGameObjects().addAll(loadedObjects);
        for(GameObject obj : gameCanvas.getGameObjects()){
            obj.currentX = obj.x;
            obj.currentY = obj.y;
            obj.lastX = obj.x;
            obj.lastY = obj.y;
            obj.deltaX = 0;
            obj.deltaY = 0;
            obj.timeElapsed = 0;
        }
        
        gameCanvas.repaint();
    }
}

    public void clearSelectionPoints() {
        this.bottomLeftCorner = null;
        // this.topRightCorner = null;
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
    
    private void showPropertyConfigDialog(GameObject obj){
        this.propertyEditingObject = obj;
        this.currentPathClickMode = PathClickMode.NONE;

        propertyDialog = new JDialog(this, "Configure object properties", false);

        propertyDialog.setLayout(new GridLayout(9, 2, 10, 10));
        propertyDialog.getRootPane().setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Field 1: Channel Listener
        propertyDialog.add(new JLabel("Triggered By Channel ID:"));
        txtTriggeredBy = new JTextField(String.valueOf(obj.triggeredBy));
        propertyDialog.add(txtTriggeredBy);

        propertyDialog.add(new JLabel("Activates Channel ID:"));
        txtActivatesChannel = new JTextField(String.valueOf(obj.activatesChannel));
        txtActivatesChannel.setEnabled(obj.type == GameObject.Type.trigger);
        propertyDialog.add(txtActivatesChannel);

        //2. Timeline
        propertyDialog.add(new JLabel("Travel Duraction (In Frames 60 FPS):"));
        txtTimeLimit = new JTextField(String.valueOf(obj.time < 0 ? 60 : obj.time));
        propertyDialog.add(txtTimeLimit);

        // Field 3: Post-Trigger Appearance
        propertyDialog.add(new JLabel("Visible When Triggered:"));
        btnToggleDisplay = new JToggleButton(obj.triggeredDisplay ? "TRUE (Stay Visible)" : "FALSE (Hide Block)", obj.triggeredDisplay);
        btnToggleDisplay.addActionListener(e -> {
            btnToggleDisplay.setText(btnToggleDisplay.isSelected() ? "TRUE (Stay Visible)" : "FALSE (Hide Block)");
        });
        propertyDialog.add(btnToggleDisplay);

        // Button 1: Standard continuous looping movement
        JButton btnSetOscillate = new JButton("Place Standard Oscillation");
        btnSetOscillate.addActionListener(e -> {
            this.currentPathClickMode = PathClickMode.OSCILLATION;
            JOptionPane.showMessageDialog(propertyDialog, "Click on the main editor grid to set the standard home oscillation target (moveX, moveY).");
        });
        propertyDialog.add(btnSetOscillate);

        // Button 2: Slides once when channel flips to true
        JButton btnSetTriggerMove = new JButton("Place Triggered Move Target");
        btnSetTriggerMove.addActionListener(e -> {
            this.currentPathClickMode = PathClickMode.TRIGGERED_MOVE;
            JOptionPane.showMessageDialog(propertyDialog, "Click on the main editor grid to set the initial triggered arrival destination (triggerX, triggerY).");
        });
        propertyDialog.add(btnSetTriggerMove);

        // Button 3: Loop/Oscillate outward from its triggered location parameters
        JButton btnSetOscillateAfterTrigger = new JButton("Place Oscillation After Triggered");
        btnSetOscillateAfterTrigger.addActionListener(e -> {
            this.currentPathClickMode = PathClickMode.OSCILLATE_AFTER_TRIGGER;
            JOptionPane.showMessageDialog(propertyDialog, "Click on the main editor grid to define the secondary looping target path AFTER the trigger wakes it up.");
        });
        propertyDialog.add(btnSetOscillateAfterTrigger);

        // 5. activation condition
        propertyDialog.add(new JLabel("Movement Activation Condition:"));
        JButton btnCycleMoveCondition = new JButton(obj.move.name().toUpperCase());
        btnCycleMoveCondition.addActionListener(e -> {
            if(obj.move == GameObject.movement.never){
                obj.move = GameObject.movement.always;
            }else if(obj.move == GameObject.movement.always){
                obj.move = GameObject.movement.touch;
            }else if(obj.move == GameObject.movement.touch){
                obj.move = GameObject.movement.trigger;
            }else{
                obj.move = GameObject.movement.never;
            }
            btnCycleMoveCondition.setText(obj.move.name().toUpperCase());
        });
        propertyDialog.add(btnCycleMoveCondition);

        // Field 6: Finalize Configurations
        propertyDialog.add(new JLabel("<html><i>Tip: Tap a Place path button<br>then click the Grid map!</i></html>"));
        JButton btnConfirm = new JButton("Confirm & Close");
        btnConfirm.addActionListener(e -> {
            try {
                propertyEditingObject.triggeredBy = Integer.parseInt(txtTriggeredBy.getText().trim());
                if (propertyEditingObject.type == GameObject.Type.trigger) {
                    propertyEditingObject.activatesChannel = Integer.parseInt(txtActivatesChannel.getText().trim());
                }
                propertyEditingObject.triggeredDisplay = btnToggleDisplay.isSelected();
                propertyEditingObject.time = Integer.parseInt(txtTimeLimit.getText().trim());
                
                // Clean active editor workflow states
                this.propertyEditingObject = null;
                this.currentPathClickMode = PathClickMode.NONE;
                propertyDialog.dispose();
                gameCanvas.repaint();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(propertyDialog, "Please verify all numerical text entries are clean integer values.");
            }
        });
        propertyDialog.add(btnConfirm);

        propertyDialog.pack();
        propertyDialog.setLocationRelativeTo(this);
        propertyDialog.setVisible(true);

    }

    public int getNextUnusedMapID(){
        int highestID = 1;
        boolean found = false;
        if(gameCanvas != null && gameCanvas.getGameObjects() != null){
            while(!found){
                found = true;
                for(GameObject obj : gameCanvas.getGameObjects()){
                    if(obj.mapID == highestID){
                        found = false;
                        highestID++;
                    }
                }
            }
        }
        return highestID;
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
    public GameObject getPropertyEditingObject(){ return this.propertyEditingObject; }
    public int getHighlightedBroadcastChannel(){ return this.highlightedBroadcastChannel; }

    public Point getPathHoverPoint() { return this.pathHoverPoint; }
    public String getCurrentPathClickModeName() { return this.currentPathClickMode.name(); }
    public boolean isSettingPathMode() { return this.currentPathClickMode != PathClickMode.NONE; }

}