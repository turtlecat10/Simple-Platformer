package src.game;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.ArrayList;

public class MapEditorWindow extends JFrame{
    //saving
    private final String externalSavePath = "src/levels.json";
    private final String externalEnemySavePath = "src/enemies.json";

    //stuff
    public Graphics gameCanvas;
    private JPanel controlPanel;
    public static MapEditorWindow instance;

    //selected thing
    private GameObject selectedObject = null;
    private Enemy selectedEnemy = null;
    private enum BrushMode {OBJECT, ENEMY, ITEM}
    private BrushMode activeBrushMode = BrushMode.OBJECT;

    //mouse point
    private Point bottomLeftCorner = null;
    private Point currentHoverPoint = null;
    private Point pathHoverPoint = null;

    //moving waypoints
    public int selectedWaypointIndex = -1;
    private boolean isDraggingWaypoint = false;
    private int activeDraggedWaypointIndex = -1;
    private boolean activeDraggedIsTriggered = false;

    //object editing
    private JTextField dialogTxtX, dialogTxtY, dialogTxtSpeedVal;
    private JComboBox<String> dialogCmbSpeedorTime;
    private JComboBox<String> dialogSegmentDropdown;
    private boolean isDraggingStartPos = false;
    private boolean isDraggingTriggeredStart = false;
    public boolean isEditingTriggeredPath = false;
    public boolean selectedWaypointIsTriggered = false;
    private GameObject propertyEditingObject = null;
    private JDialog propertyDialog = null;
    private JTextField txtTriggeredBy, txtActivatesChannel;
    private JToggleButton btnToggleDisplay;
    private GameObject.Type activeBrushType = GameObject.Type.solid;
    public int activeEditingMapID = 1;
    private int currentlyLoadedMapID = -1;
    private int highlightedBroadcastChannel = 0;


    private JComboBox<String> cmbEnemyTemplate = new JComboBox<>();
    private JComboBox<String> cmbObjectTemplate = new JComboBox<>();

    //sidebar
    private long lastSegmentClickTime = 0;
    private int lastClickedSegmentIdx = -1;
    private boolean lastClickedIsTriggeredPath = false;
    private JLabel lblX, lblY, lblWidth, lblHeight, lblBrush, lblSelectedInfo, lblFreePlace;
    private JTextField txtMapID;
    private JButton btnEditProperties;
    private JPanel mainContentCardContainer;
    private CardLayout windowLayoutSwitcher;
    private JPanel levelGridMenuPanel;

    //buttons
    private boolean freePlace;
    private JButton btnDelete = new JButton("delete");
    private JButton btnPlay = new JButton("Play");
    private JButton btnExit = new JButton("Exit to Editor");
    private JButton btnSave = new JButton("Save Level");
    private JButton btnLoad = new JButton("Load stage");
    private JButton btnFreePlace = new JButton("Free place");
    private JButton btnNew = new JButton("New map");
    private JButton btnSelectLevelMenu = new JButton("Browse Levels");
    private JButton btnExportLevel = new JButton("Export level");
    private JButton btnImportLevel = new JButton("Import level");
    JButton btnDeleteLevel = new JButton("Delete Level");

    //enemy editing
    private Enemy propertyEditingEnemy = null;
    private JDialog enemyProperty = null;
    private JTextField txtSpeed, txtWidth, txtHeight;
    private JComboBox<String> enemyTypeDropdown;
    private JOptionPane enemyTypes;
    private JTextField txtTimeLimit;
    private enum PathClickMode { NONE, OSCILLATION, TRIGGERED_MOVE, OSCILLATE_AFTER_TRIGGER}
    private PathClickMode currentPathClickMode = PathClickMode.NONE;
    private EnemyRegistry activeEnemyBrush = EnemyRegistry.NONE;
    private Enemy draggedEnemy = null;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    //resizing
    public enum ResizeMode{ NONE, LEFT, RIGHT, TOP, BOTTOM, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT}
    private ResizeMode currentResizeMode = ResizeMode.NONE;
    private ResizeMode currentHoverResizeMode = ResizeMode.NONE;
    private int resizeOriginalX = 0;
    private int resizeOriginalY = 0;
    private int resizeOriginalWidth = 0;
    private int resizeOriginalHeight = 0;
    private int resizeAnchorX = 0;
    private int resizeAnchorY = 0;

    //camera
    public int cameraX = 0;
    public int cameraY = 0;
    private final int cameraSpeed = 16;
    private boolean camLeft = false, camRight = false, camUp = false, camDown = false;
    private Timer editorCamTimer;
    private JLabel lblZoom;

    //items
    private Item selectedItem = null;
    private Item propertyEditingItem = null;
    private ItemRegistry activeItemBrush = ItemRegistry.NONE;
    private Item draggedItem = null;
    private final String externalItemSavePath = "src/items.json";
    public int selectedItemWaypointIndex = -1;
    private JComboBox<String> itemSegmentDropdown;
    private JTextField itemTxtWpX, itemTxtWpY, itemTxtSpeedVal;
    private JComboBox<String> itemCmbSpeedOrTime;
    private boolean isDraggingItemWaypoint = false;
    private int activeDraggedItemWaypointIndex = -1;
    private JComboBox<Item.ItemType> cmbItemType = new JComboBox<>(Item.ItemType.values());
    private JComboBox<String> cmbItemTemplate = new JComboBox<>();

    private ArrayList<String> undoStack = new ArrayList<>();
    private ArrayList<String> redoStack = new ArrayList<>();    

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

                double zoom = gameCanvas.getEditorZoom();
                int currentSpeed = (int) Math.round(cameraSpeed / zoom);
                if(currentSpeed < 1) currentSpeed = 1;

                if (camLeft)  currentX -= currentSpeed;
                if (camRight) currentX += currentSpeed;
                if (camUp)    currentY -= currentSpeed;
                if (camDown)  currentY += currentSpeed;

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
            loadEnemiesManual(externalEnemySavePath);
            gameCanvas.loadLevel(activeEditingMapID);
            currentlyLoadedMapID = activeEditingMapID;
            if(gameCanvas.getMovement() != null){
                gameCanvas.getMovement().resetTriggerTrackingState();
            }
        }catch(Exception ex){}

        pack();
        setLocationRelativeTo(null);
        revalidate();
        repaint();
        setVisible(true); 
        gameCanvas.requestFocusInWindow();
    }
    
    //creating sidebar
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

        String[] objectTypeNames = new String[GameObject.Type.values().length];
        for(int i = 0; i < GameObject.Type.values().length; i++){
            objectTypeNames[i] = GameObject.Type.values()[i].name();
        }
        JComboBox<String> cmbObjectType = new JComboBox<>(objectTypeNames);
        cmbObjectType.setMaximumSize(new Dimension(200, 32));
        cmbObjectType.setAlignmentX(Component.LEFT_ALIGNMENT);
        cmbObjectType.setSelectedItem(activeBrushType.name());
        cmbObjectType.addActionListener(e -> {
            String selected = (String) cmbObjectType.getSelectedItem();
            if(selected != null){
                activeBrushType = GameObject.Type.valueOf(selected);
                activeBrushMode = BrushMode.OBJECT;
                lblBrush.setText("Selected Brush: "+activeBrushType.name().toUpperCase());
            }
        });
        controlPanel.add(cmbObjectType);
        controlPanel.add(Box.createVerticalStrut(5));

        String[] enemyTypeNames = java.util.Arrays.copyOf(EnemyRegistry.getDisplayChoices(), EnemyRegistry.getDisplayChoices().length);
        JComboBox<String> cmbEnemyType = new JComboBox<>(enemyTypeNames);
        cmbEnemyType.setMaximumSize(new Dimension(98, 32));
        cmbEnemyType.setAlignmentX(Component.LEFT_ALIGNMENT);
        cmbEnemyType.setSelectedItem(activeEnemyBrush.getDisplayName());

        cmbEnemyTemplate.setMaximumSize(new Dimension(98, 32));
        cmbEnemyTemplate.setAlignmentX(Component.LEFT_ALIGNMENT);

        Runnable updateEnemyTemplates = () -> {
            cmbEnemyTemplate.removeAllItems();
            String selectedDisplay = (String) cmbEnemyType.getSelectedItem();
            EnemyRegistry selectedType = EnemyRegistry.fromDisplayName(selectedDisplay);
            java.util.List<String> templates = getTemplatesForEnemy(selectedType);
            for(String tName : templates){
                cmbEnemyTemplate.addItem(tName);
            }
        };

        cmbEnemyType.addActionListener(e -> {
            String selected = (String) cmbEnemyType.getSelectedItem();
            if(selected != null){
                activeBrushMode = BrushMode.ENEMY;
                activeEnemyBrush = EnemyRegistry.fromDisplayName(selected);
                updateEnemyTemplates.run();
                lblBrush.setText("Selected Brush: ENEMY [" + activeEnemyBrush.name() + "]");
            }
        });

        cmbEnemyTemplate.addActionListener(e -> {
            activeBrushMode = BrushMode.ENEMY;
            if(cmbEnemyTemplate.getSelectedItem() != null){
                lblBrush.setText("Selected Brush: ENEMY [" + cmbEnemyTemplate.getSelectedItem() + "]");
            }
        });

        updateEnemyTemplates.run();

        JPanel enemyComboRow = new JPanel(new GridLayout(1, 2, 4, 0));
        enemyComboRow.setBackground(new Color(240, 240, 240));
        enemyComboRow.setMaximumSize(new Dimension(200, 32));
        enemyComboRow.setMinimumSize(new Dimension(200, 32));
        enemyComboRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        enemyComboRow.add(cmbEnemyType);
        enemyComboRow.add(cmbEnemyTemplate);

        controlPanel.add(enemyComboRow);
        controlPanel.add(Box.createVerticalStrut(5));

        cmbItemTemplate.setMaximumSize(new Dimension(98, 32));
        cmbItemTemplate.setAlignmentX(Component.LEFT_ALIGNMENT);
        cmbItemType.setMaximumSize(new Dimension(98, 32));
        cmbItemType.setAlignmentX(Component.LEFT_ALIGNMENT);

        Runnable updateTemplates = () -> {
            cmbItemTemplate.removeAllItems();
            Item.ItemType selectedType = (Item.ItemType) cmbItemType.getSelectedItem();
            java.util.List<String> templates = getTemplatesForType(selectedType);
            for(String tName : templates){
                cmbItemTemplate.addItem(tName);
            }
        };

        cmbItemType.addActionListener(e -> {
            activeBrushMode = BrushMode.ITEM;
            updateTemplates.run();
            lblBrush.setText("Selected Brush: ITEM [" + cmbItemType.getSelectedItem() + "]");
        });

        cmbItemTemplate.addActionListener(e -> {
            activeBrushMode = BrushMode.ITEM;
            if(cmbItemTemplate.getSelectedItem() != null){
                lblBrush.setText("Selected Brush: ITEM [" + cmbItemTemplate.getSelectedItem() + "]");
            }
        });

        updateTemplates.run();
        // activeBrushMode = BrushMode.OBJECT;
        JPanel comboRow = new JPanel(new GridLayout(1, 2, 4, 0));
        comboRow.setBackground(new Color(240, 240, 240));
        comboRow.setMaximumSize(new Dimension(200, 32));
        comboRow.setMinimumSize(new Dimension(200, 32));
        comboRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        comboRow.add(cmbItemType);
        comboRow.add(cmbItemTemplate);

        controlPanel.add(comboRow);
        controlPanel.add(Box.createVerticalStrut(5));

        btnDelete.setMaximumSize(new Dimension(200,32));
        btnDelete.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnDelete.setEnabled(false);

        btnDelete.addActionListener(e -> {
            saveStateForUndo();
            if(selectedObject != null && gameCanvas.getGameObjects() != null){
                gameCanvas.getGameObjects().remove(selectedObject);
                selectedObject = null;
                propertyEditingObject = null;
                lblSelectedInfo.setText("Selection: Delete");
                btnDelete.setEnabled(false);
                btnEditProperties.setEnabled(false);
                gameCanvas.repaint();
            }
            if(selectedEnemy != null && gameCanvas.getMovement().getEnemies() != null){
                gameCanvas.getMovement().getEnemies().remove(selectedEnemy);
                selectedEnemy = null;
                propertyEditingEnemy = null;
                lblSelectedInfo.setText("Selection: Delete");
                btnDelete.setEnabled(false);
                btnEditProperties.setEnabled(false);
                gameCanvas.repaint();
            }
            if(selectedItem != null && gameCanvas.getMovement().worldItems != null){
                gameCanvas.getMovement().worldItems.remove(selectedItem);
                selectedItem = null;
                propertyEditingItem = null;
                lblSelectedInfo.setText("Selection: Delete");
                btnDelete.setEnabled(false);
                btnEditProperties.setEnabled(false);
                gameCanvas.repaint();
            }
        });
        controlPanel.add(btnDelete);
        controlPanel.add(Box.createVerticalStrut(5));

        btnEditProperties = new JButton("edit object properties");
        btnEditProperties.setMaximumSize(new Dimension(200, 32));
        btnEditProperties.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnEditProperties.setEnabled(false);

        btnEditProperties.addActionListener(e -> {
            if(selectedObject != null){
                this.propertyEditingObject = selectedObject;
                showPropertyConfigDialog(selectedObject);
                gameCanvas.repaint();
            }else if(selectedEnemy != null){
                this.propertyEditingEnemy = selectedEnemy;
                showEnemyConfigDialog(propertyEditingEnemy);
                gameCanvas.repaint();
            }else if(selectedItem != null){
                this.propertyEditingItem = selectedItem;
                showItemConfigDialog(selectedItem);
                gameCanvas.repaint();
            }
        });
        controlPanel.add(btnEditProperties);
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

        lblZoom = new JLabel("Zoom: 100%");
        lblZoom.setForeground(new Color(0, 100, 200));
        lblZoom.setFont(new Font("Arial", Font.BOLD, 12));
        lblZoom.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblZoom.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblZoom.setToolTipText("Click to reset zoom to 100%");
        lblZoom.addMouseListener(new MouseAdapter() {
            @Override 
            public void mouseClicked(MouseEvent e){
                if(gameCanvas != null){
                    gameCanvas.setEditorZoom(1.0);
                    updateZoomLabel();
                    gameCanvas.repaint();
                }
            }
        });

        controlPanel.add(lblZoom);

        Dimension btnSize = new Dimension(200, 32);
        JButton[] actionButtons = {btnSave, btnPlay, btnExit, btnLoad, btnFreePlace, btnNew, btnSelectLevelMenu, btnExportLevel, btnImportLevel, btnDeleteLevel};
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
                loadEnemiesManual(externalEnemySavePath);
                loadItemsManual(externalItemSavePath);
                gameCanvas.loadLevel(activeEditingMapID);
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
                    JOptionPane.showMessageDialog(this, "ERROR: You are trying to save to Map ID " + targetSaveID + ", but you currently have Map ID " + currentlyLoadedMapID + " loaded", "Editor Safety Error", JOptionPane.ERROR_MESSAGE);
                    return;
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
                saveEnemiesManual(gameCanvas.getMovement().getEnemies(), externalEnemySavePath);
                saveItemsManual(gameCanvas.getMovement().worldItems, externalItemSavePath);
                JOptionPane.showMessageDialog(this, "Level "+ activeEditingMapID +" changes successfully saved");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Error: Invalid numeric input format passed for Map ID");
            }
        });

        btnPlay.addActionListener(e->{
            try{
                gameCanvas.setEditorZoom(1.0);
                updateZoomLabel();
                activeEditingMapID = Integer.parseInt(txtMapID.getText().trim());
                gameCanvas.setEditorMode(false);
                gameCanvas.loadLevel(activeEditingMapID);
                btnDelete.setEnabled(false);
                btnEditProperties.setEnabled(false);
                btnPlay.setEnabled(false);
                btnExit.setEnabled(true);
                btnSave.setEnabled(false);
                btnLoad.setEnabled(false);
                btnFreePlace.setEnabled(false);
                btnNew.setEnabled(false);
                btnSelectLevelMenu.setEnabled(false);
                selectedEnemy = null;
                selectedObject = null;
                selectedItem = null;
                for(Enemy enemy : gameCanvas.getMovement().getEnemies()){
                    if(enemy instanceof FlyingEnemy){
                        FlyingEnemy fe = (FlyingEnemy) enemy;
                        fe.setAngle(0);
                    }
                }
                gameCanvas.requestFocusInWindow();
                gameCanvas.getMovement().resetTriggerTrackingState();
            }catch (Exception ex){}
        });

        btnExit.addActionListener(e -> {
            if (gameCanvas.getMovement() != null) {
                gameCanvas.getMovement().resetTriggerTrackingState();

                Movement movement = gameCanvas.getMovement();
                if(movement.inventory != null && !movement.inventory.isEmpty()){
                    for(Item invItem : new ArrayList<>(movement.inventory)){
                        invItem.resetToStart();
                        movement.worldItems.add(invItem);
                    }
                    movement.inventory.clear();
                }
                if(movement.equippedWeapon != null){
                    movement.equippedWeapon = null;
                }
            }
            gameCanvas.getMovement().setDeathCount(0);
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
                    obj.resetToStart();
                }
            }
            ArrayList<Enemy> allEnemies = gameCanvas.getMovement().getEnemies();
            if(allEnemies != null){
                for(Enemy obj : allEnemies){
                    obj.resetToStart();
                    obj.currentHealth = obj.maxHealth;
                }
            }
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
            freePlace = !freePlace;
            if(freePlace){
                lblFreePlace.setText("Selection mode: Free Place");
            }else{
                lblFreePlace.setText("Selection mode: Grid");
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
            loadEnemiesManual(externalEnemySavePath);
            gameCanvas.loadLevel(activeEditingMapID);
            gameCanvas.repaint();
            JOptionPane.showMessageDialog(this, "New Map " + activeEditingMapID + " created successfully");
        });

        btnSelectLevelMenu.addActionListener(e -> {
            levelGridMenuPanel.removeAll();
            btnPlay.setEnabled(false);
            btnExit.setEnabled(false);
            btnSave.setEnabled(false);
            btnLoad.setEnabled(false);
            btnFreePlace.setEnabled(false);
            btnNew.setEnabled(false);

            java.util.Set<Integer> registeredMaps = new java.util.HashSet<>();
            if(gameCanvas.getGameObjects() != null){
                for(GameObject obj : gameCanvas.getGameObjects()){
                    registeredMaps.add(obj.mapID);
                }
            }
            if(registeredMaps.isEmpty()){
                registeredMaps.add(1);
            }
            for(int id : registeredMaps){
                LevelPreview preview = new LevelPreview(id, this, gameCanvas);
                levelGridMenuPanel.add(preview);
            }
            levelGridMenuPanel.revalidate();
            levelGridMenuPanel.repaint();

            windowLayoutSwitcher.show(mainContentCardContainer, "LEVEL_MENU");
        });

        btnExportLevel.addActionListener(e -> {
            openExportDialog();
        });

        btnImportLevel.addActionListener(e -> {
            openImportDialog();
        });

        btnDeleteLevel.addActionListener(e -> {
            saveStateForUndo();
            int choice = JOptionPane.showConfirmDialog(controlPanel, "Do you want to continue?", "Confirm Action", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if(choice == JOptionPane.YES_OPTION){
                propertyEditingEnemy = null;
                propertyEditingItem = null;
                propertyEditingObject = null;
                selectedEnemy = null;
                selectedItem = null;
                selectedObject = null;
                int currentMap = gameCanvas.getCurrentMap();
                gameCanvas.getGameObjects().removeIf(obj -> obj.mapID == currentMap);
                gameCanvas.getMovement().getEnemies().removeIf(enemy -> enemy.mapID == currentMap);
                gameCanvas.getMovement().worldItems.removeIf(item -> item.mapID == currentMap);
                gameCanvas.repaint();
            }
        });
        
        controlPanel.add(btnDeleteLevel);

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
        double effectiveScale = scale * gameCanvas.getEditorZoom();

        int offsetX = (int) ((panelWidth - (virtualWidth * scale)) / 2);
        int offsetY = (int) ((panelHeight - (virtualHeight * scale)) / 2);

        int virtualMouseX = (int) ((e.getX() - offsetX) / effectiveScale);
        int virtualMouseY = (int) ((e.getY() - offsetY) / effectiveScale);

        return new Point(virtualMouseX + gameCanvas.getCameraX(), virtualMouseY + gameCanvas.getCameraY());
    }
    
    //handling clicking
    private void setupEditorClickEngine(){
        if(gameCanvas == null) return;

        gameCanvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK), "UNDO_ACTION");
        gameCanvas.getActionMap().put("UNDO_ACTION", new AbstractAction() {
            @Override 
            public void actionPerformed(ActionEvent e){
                performUndo();
            }
        });

        gameCanvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_DOWN_MASK), "REDO_ACTION");
        gameCanvas.getActionMap().put("REDO_ACTION", new AbstractAction() {
            @Override 
            public void actionPerformed(ActionEvent e){
                performRedo();
            }
        });

        gameCanvas.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    bottomLeftCorner = null;
                    currentHoverPoint = null;
                    activeEnemyBrush = EnemyRegistry.NONE;
                    lblBrush.setText("selectedBrush: "+ activeBrushType.name());
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
                int snapX, snapY;
                if(!freePlace){
                    snapX = Math.floorDiv(gamePoint.x, snapSize) * snapSize;
                    snapY = Math.floorDiv(gamePoint.y, snapSize) * snapSize;
                }else{
                    snapX = gamePoint.x;
                    snapY = gamePoint.y;
                }

                if(gameCanvas.lastIndicatorBounds != null && gameCanvas.lastIndicatorBounds.contains(gamePoint)){
                    Object sel = selectedObject != null ? selectedObject : (selectedEnemy != null ? selectedEnemy : selectedItem);
                    if(sel != null){
                        int itemCenterX = 0, itemCenterY = 0;
                        if(sel instanceof GameObject){
                            GameObject o = (GameObject) sel;
                            itemCenterX = o.x + o.width / 2;
                            itemCenterY = (o.y - o.height / 2);
                        }else if(sel instanceof Enemy){
                            Enemy o = (Enemy) sel;
                            itemCenterX = o.x + o.width / 2;
                            itemCenterY = o.y - o.height / 2;
                        }else if(sel instanceof Item){
                            Item o = (Item) sel;
                            itemCenterX = o.x + o.width / 2;
                            itemCenterY = o.y - o.height / 2;
                        }

                        int targetCamX = Math.round((itemCenterX - gameCanvas.virtualWidth / 2) / 16.0f) * 16;
                        int targetCamY = Math.round((itemCenterY - gameCanvas.virtualHeight / 2) / 16.0f) * 16;

                        if(targetCamX < 0) targetCamX = 0;
                        if(targetCamY < 0) targetCamY = 0;

                        gameCanvas.setCameraX(targetCamX);
                        gameCanvas.setCameraY(targetCamY);
                        gameCanvas.repaint();
                        return;
                    }
                }

                Object activeSelTarget = propertyEditingObject != null ? propertyEditingObject : (selectedObject != null ? selectedObject : (selectedEnemy != null ? selectedEnemy : selectedItem));
                if(activeSelTarget != null && (SwingUtilities.isLeftMouseButton(e) || SwingUtilities.isRightMouseButton(e))){
                    int tX = 0, tY = 0, tW = 0, tH = 0;
                    if(activeSelTarget instanceof GameObject){
                        GameObject obj = (GameObject) activeSelTarget;
                        if(activeEditingMapID == obj.mapID){
                            tX = obj.x; tY = obj.y - obj.height; tW = obj.width; tH = obj.height;
                        }
                    }else if(activeSelTarget instanceof Enemy){
                        Enemy en = (Enemy) activeSelTarget;
                        if(activeEditingMapID == en.mapID){
                            tX = en.x; tY = en.y - en.height; tW = en.width; tH = en.height;
                        }
                    }else if(activeSelTarget instanceof Item){
                        Item it = (Item) activeSelTarget;
                        if(activeEditingMapID == it.mapID){
                            tX = it.x; tY = it.y - it.height; tW = it.width; tH = it.height;
                        }
                    }

                    int handleThreshold = 6;
                    Rectangle bounds = new Rectangle(tX+1, tY-1, tW+2, tH+2);
                    Rectangle innerBounds = new Rectangle(tX + handleThreshold, tY + handleThreshold, Math.max(1, tW - handleThreshold * 2), Math.max(1, tH - handleThreshold * 2));

                    if(bounds.contains(gamePoint) && !innerBounds.contains(gamePoint)){
                        saveStateForUndo();
                        boolean isLeft = Math.abs(gamePoint.x - tX) <= handleThreshold;
                        boolean isRight = Math.abs(gamePoint.x - (tX + tW)) <= handleThreshold;
                        boolean isTop = Math.abs(gamePoint.y - tY) <= handleThreshold;
                        boolean isBottom = Math.abs(gamePoint.y - (tY + tH)) <= handleThreshold;

                        if(isTop && isLeft) currentResizeMode = ResizeMode.TOP_LEFT;
                        else if(isTop && isRight) currentResizeMode = ResizeMode.TOP_RIGHT;
                        else if(isBottom && isLeft) currentResizeMode = ResizeMode.BOTTOM_LEFT;
                        else if(isBottom && isRight) currentResizeMode = ResizeMode.BOTTOM_RIGHT;
                        else if(isLeft) currentResizeMode = ResizeMode.LEFT;
                        else if(isRight) currentResizeMode = ResizeMode.RIGHT;
                        else if(isTop) currentResizeMode = ResizeMode.TOP;
                        else if(isBottom) currentResizeMode = ResizeMode.BOTTOM;

                        if(currentResizeMode != ResizeMode.NONE){
                            int left = tX;
                            int right = tX + tW;
                            int top = tY;
                            int bottom = tY + tH;

                            resizeAnchorX = (currentResizeMode == ResizeMode.LEFT || currentResizeMode == ResizeMode.TOP_LEFT || currentResizeMode == ResizeMode.BOTTOM_LEFT) ? right : left;
                            resizeAnchorY = (currentResizeMode == ResizeMode.TOP || currentResizeMode == ResizeMode.TOP_LEFT || currentResizeMode == ResizeMode.TOP_RIGHT) ? bottom : top;
                            gameCanvas.repaint();
                            return;
                        }
                    }
                }

                if (selectedObject != null && (SwingUtilities.isLeftMouseButton(e) || SwingUtilities.isRightMouseButton(e))) {
                    for(int pathType = 0; pathType < 2; pathType++){
                        boolean isTriggered = (pathType == 1);
                        ArrayList<Integer> px = isTriggered ? selectedObject.triggeredPathX : selectedObject.pathX;
                        ArrayList<Integer> py = isTriggered ? selectedObject.triggeredPathY : selectedObject.pathY;
                        ArrayList<Double> pSpd = isTriggered ? selectedObject.triggeredPathSpeed : selectedObject.pathSpeed;
                        ArrayList<Boolean> pIsSpd = isTriggered ? selectedObject.triggeredPathSpeedIsSpeed : selectedObject.pathSpeedIsSpeed;

                        if(px == null || px.isEmpty()) continue;

                        int startX = isTriggered ? selectedObject.triggeredX : selectedObject.currentX;
                        int startY = isTriggered ? selectedObject.triggeredY : selectedObject.currentY;

                        ArrayList<Point> points = new ArrayList<>();
                        points.add(new Point(startX, startY));
                        for(int i = 0; i < Math.min(px.size(), py.size()); i++){
                            points.add(new Point(px.get(i), py.get(i)));
                        }

                        for(int i = 0; i < points.size() - 1; i++){
                            Point p1 = points.get(i);
                            Point p2 = points.get(i+1);
                            
                            int p1CenterX = p1.x + selectedObject.width / 2;
                            int p1CenterY = (p1.y - selectedObject.height) + selectedObject.height / 2;
                            int p2CenterX = p2.x + selectedObject.width / 2;
                            int p2CenterY = (p2.y - selectedObject.height) + selectedObject.height / 2;

                            int midX = (p1CenterX + p2CenterX) / 2;
                            int midY = (p1CenterY + p2CenterY) / 2 - 10;

                            double val = (pSpd != null && i < pSpd.size()) ? pSpd.get(i) : 2.0;
                            boolean isSpd = (pIsSpd != null && i < pIsSpd.size()) ? pIsSpd.get(i) : true;
                            String labelText = val + (isSpd ? " px/f" : " frames");

                            int boxWidth = labelText.length() * 7 + 10;
                            int boxHeight = 16;
                            Rectangle labelRect = new Rectangle(midX - boxWidth / 2, midY - boxHeight + 2, boxWidth, boxHeight);

                            if(labelRect.contains(gamePoint)){
                                long now = System.currentTimeMillis();
                                if(now - lastSegmentClickTime <= 500 && lastClickedSegmentIdx == i && lastClickedIsTriggeredPath == isTriggered){
                                    while (pSpd.size() <= i) pSpd.add(2.0);
                                    while(pIsSpd.size() <= i) pIsSpd.add(true);

                                    int splitX = midX;
                                    if(gamePoint.x > splitX){
                                        boolean currentMode = pIsSpd.get(i);
                                        pIsSpd.set(i, !currentMode);
                                    }else{
                                        String response = JOptionPane.showInputDialog(MapEditorWindow.this, "Enter new Speed / Time value:", val);
                                        if(response != null && !response.trim().isEmpty()){
                                            try{
                                                double newVal = Double.parseDouble(response.trim());
                                                pSpd.set(i, newVal);
                                            }catch(NumberFormatException ex){
                                                JOptionPane.showMessageDialog(MapEditorWindow.this, "Invalid number format.");
                                            }
                                        }
                                    }
                                    updatePropertyDialogTextFields();
                                    gameCanvas.repaint();
                                    lastSegmentClickTime = 0;
                                    return;
                                }
                                lastSegmentClickTime = now;
                                lastClickedSegmentIdx = i;
                                lastClickedIsTriggeredPath = isTriggered;
                                return;
                            }
                        }
                    }

                    Rectangle startRect = new Rectangle(selectedObject.currentX, selectedObject.currentY - selectedObject.height, selectedObject.width, selectedObject.height);
                    if(startRect.contains(gamePoint)){
                        saveStateForUndo();
                        isDraggingStartPos = true;
                        isEditingTriggeredPath = false;
                        selectedWaypointIndex = -1;
                        dragOffsetX = gamePoint.x - selectedObject.currentX;
                        dragOffsetY = gamePoint.y - (selectedObject.currentY - selectedObject.height);
                        gameCanvas.repaint();
                        return;
                    }

                    Rectangle trigStartRect = new Rectangle(selectedObject.triggeredX, selectedObject.triggeredY - selectedObject.height, selectedObject.width, selectedObject.height);
                    if(trigStartRect.contains(gamePoint)){
                        saveStateForUndo();
                        isDraggingTriggeredStart = true;
                        isEditingTriggeredPath = true;
                        selectedWaypointIndex = -1;
                        dragOffsetX = gamePoint.x - selectedObject.triggeredX;
                        dragOffsetY = gamePoint.y - (selectedObject.triggeredY - selectedObject.height);
                        gameCanvas.repaint();
                        return;
                    }

                    // Check if clicking near an existing waypoint to drag it
                    int clickedIdx = -1;
                    boolean clickedTriggered = false;

                    for(int i = 0; i < selectedObject.pathX.size(); i++){
                        Rectangle wpRect = new Rectangle((int)selectedObject.pathX.get(i), selectedObject.pathY.get(i) - selectedObject.height, selectedObject.width, selectedObject.height);
                        if(wpRect.contains(gamePoint)){
                            clickedIdx = i;
                            clickedTriggered = false;
                            break;
                        }
                    }

                    if(clickedIdx == -1){
                        for(int i = 0; i < selectedObject.triggeredPathX.size(); i++){
                            Rectangle wpRect = new Rectangle(selectedObject.triggeredPathX.get(i), selectedObject.triggeredPathY.get(i) - selectedObject.height, selectedObject.width, selectedObject.height);
                            if(wpRect.contains(gamePoint)){
                                clickedIdx = i;
                                clickedTriggered = true;
                                break;
                            }
                        }
                    }


                    if (clickedIdx != -1) {
                        saveStateForUndo();
                        isDraggingWaypoint = true;
                        activeDraggedWaypointIndex = clickedIdx;
                        activeDraggedIsTriggered = clickedTriggered;
                        selectedWaypointIndex = clickedIdx;
                        selectedWaypointIsTriggered = clickedTriggered;
                        isEditingTriggeredPath = clickedTriggered;

                        ArrayList<Integer> px = clickedTriggered ? selectedObject.triggeredPathX : selectedObject.pathX;
                        ArrayList<Integer> py = clickedTriggered ? selectedObject.triggeredPathY : selectedObject.pathY;
                        dragOffsetX = gamePoint.x - px.get(clickedIdx);
                        dragOffsetY = gamePoint.y - (py.get(clickedIdx) - selectedObject.height);
                    } 
                    gameCanvas.repaint();
                    return;
                }

                if(selectedEnemy != null && (SwingUtilities.isLeftMouseButton(e) || SwingUtilities.isRightMouseButton(e))){
                    if(gameCanvas.getMovement() != null){
                        ArrayList<Enemy> enemies = gameCanvas.getMovement().getEnemies();
                        if(enemies != null){
                            for(int i = enemies.size() - 1; i >= 0; i--){
                                Enemy enemy = enemies.get(i);
                                int objTop = enemy.y - enemy.height;
                                if(enemy.mapID == activeEditingMapID && gamePoint.x >= enemy.x && gamePoint.x <= (enemy.x + enemy.width) && gamePoint.y >= objTop && gamePoint.y <= enemy.y){
                                    saveStateForUndo();
                                    draggedEnemy = enemy;
                                    selectedEnemy = enemy;
                                    dragOffsetX = gamePoint.x - enemy.x;
                                    dragOffsetY = gamePoint.y - objTop;
                                    btnDelete.setEnabled(true);
                                    btnEditProperties.setEnabled(true);
                                    gameCanvas.repaint();
                                    return;
                                }
                            }
                        }
                    }
                }

                if(selectedItem != null && (SwingUtilities.isLeftMouseButton(e) || SwingUtilities.isRightMouseButton(e))){
                    if(gameCanvas.getMovement().worldItems != null){
                        Item item = selectedItem;

                        if(item.attackX != null && !item.attackX.isEmpty() && item.mapID == activeEditingMapID){
                            ArrayList<Point> points = new ArrayList<>();
                            points.add(new Point(item.x, item.y));
                            for(int i = 0; i < item.attackX.size(); i++){
                                points.add(new Point(item.attackX.get(i), item.attackY.get(i)));
                            }
                            points.add(new Point(item.x, item.y));

                            for(int i = 0; i < points.size() - 1; i++){
                                Point p1 = points.get(i);
                                Point p2 = points.get(i + 1);

                                int p1CenterX = p1.x + item.width / 2;
                                int p1CenterY = p1.y - item.height / 2;
                                int p2CenterX = p2.x + item.width / 2;
                                int p2CenterY = p2.y - item.height / 2;

                                int midX = (p1CenterX + p2CenterX) / 2;
                                int midY = (p1CenterY + p2CenterY) / 2 - 10;

                                double val = (i < item.speed.size()) ? item.speed.get(i) : 2.0;
                                boolean isSpd = (i < item.isSpeed.size()) ? item.isSpeed.get(i) : true;
                                String labelText = val + (isSpd ? " px/f" : " frames");

                                int boxWidth = labelText.length() * 7 + 10;
                                int boxHeight = 16;
                                Rectangle labelRect = new Rectangle(midX - boxWidth / 2, midY - boxHeight + 2, boxWidth, boxHeight);

                                if(labelRect.contains(gamePoint)){long now = System.currentTimeMillis();
                                    if(now - lastSegmentClickTime <= 500 && lastClickedSegmentIdx == i && !lastClickedIsTriggeredPath){
                                        while(item.speed.size() <= i) item.speed.add(2.0);
                                        while(item.isSpeed.size() <= i) item.isSpeed.add(true);
                                    
                                        int splitX = midX;
                                        if(gamePoint.x > splitX){
                                            boolean currentMode = item.isSpeed.get(i);
                                            item.isSpeed.set(i, !currentMode);
                                        }else{
                                            String response = JOptionPane.showInputDialog(MapEditorWindow.this, "Enter new Speed / Time value:", val);
                                            if(response != null && !response.trim().isEmpty()){
                                                try{
                                                    double newVal = Double.parseDouble(response.trim());
                                                    item.speed.set(i, newVal);
                                                }catch(NumberFormatException ex){
                                                    JOptionPane.showMessageDialog(MapEditorWindow.this, "Invalid number format");
                                                }
                                            }
                                        }
                                        gameCanvas.repaint();
                                        lastSegmentClickTime = 0;
                                        return;
                                    }
                                    lastSegmentClickTime = now;
                                    lastClickedSegmentIdx = i;
                                    lastClickedIsTriggeredPath = false;
                                    return;
                                }
                            }
                        }


                        int objTop = item.y - item.height;
                        if(item.mapID == activeEditingMapID && gamePoint.x >=item.x && gamePoint.x <= (item.x + item.width) && gamePoint.y >= objTop && gamePoint.y <= item.y){
                            saveStateForUndo();
                            draggedItem = item;
                            dragOffsetX = gamePoint.x - item.x;
                            dragOffsetY = gamePoint.y - objTop;
                            gameCanvas.repaint();
                            return;
                        }
                        if(item.attackX != null && item.attackY != null && item.mapID == activeEditingMapID){
                            for(int i = 0; i < item.attackX.size(); i++){
                                int wpX = item.attackX.get(i);
                                int wpY = item.attackY.get(i);
                                Rectangle wpRect = new Rectangle(wpX, wpY - item.height, item.width, item.height);
                                if(wpRect.contains(gamePoint)){
                                    saveStateForUndo();
                                    isDraggingItemWaypoint = true;
                                    activeDraggedItemWaypointIndex = i;
                                    selectedItemWaypointIndex = i;
                                    dragOffsetX = gamePoint.x - wpX;
                                    dragOffsetY = gamePoint.y - (wpY - item.height);
                                    gameCanvas.repaint();
                                    return;
                                }
                            }
                        }
                    }
                }

                if(SwingUtilities.isLeftMouseButton(e) || SwingUtilities.isRightMouseButton(e)){
                    if(activeBrushMode == BrushMode.ITEM && cmbItemTemplate.getSelectedItem() != null && gameCanvas.getMovement() != null && selectedEnemy == null && selectedObject == null && selectedItem == null){
                        int placementY, placementX;
                        if(!freePlace){
                            int cellX = Math.floorDiv(gamePoint.x, snapSize);
                            int cellY = Math.floorDiv(gamePoint.y, snapSize);
                            placementX = cellX * snapSize;
                            placementY = (cellY + 1) * snapSize;
                        }else{
                            placementX = gamePoint.x;
                            placementY = gamePoint.y;
                        }
                        
                        Item.ItemType selType = (Item.ItemType) cmbItemType.getSelectedItem();
                        String selTemplate = (String) cmbItemTemplate.getSelectedItem();

                        saveStateForUndo();
                        Item newItem = createItemFromTemplate(selTemplate, selType, placementX, placementY, activeEditingMapID);
                        if(newItem != null){
                            gameCanvas.getMovement().worldItems.add(newItem);
                            selectedItem = newItem;
                            propertyEditingItem = selectedItem;
                            showItemConfigDialog(selectedItem);
                        }
                        lblBrush.setText("Selected Brush: ITEM [" + selTemplate + "]");
                        gameCanvas.repaint();
                        return;
                    }

                    if(activeBrushMode == BrushMode.ENEMY && gameCanvas.getMovement() != null && selectedEnemy == null && selectedObject == null && selectedItem == null){
                        int placementY, placementX;
                        if(!freePlace){
                            int cellX = Math.floorDiv(gamePoint.x, snapSize);
                            int cellY = Math.floorDiv(gamePoint.y, snapSize);
                            placementX = cellX * snapSize;
                            placementY = (cellY + 1) * snapSize;
                        }else{
                            placementX = gamePoint.x;
                            placementY = gamePoint.y;
                        }
                        ArrayList<Enemy> currentEnemyList = gameCanvas.getMovement().getEnemies();
                        String selEnemyTemplate = (String) cmbEnemyTemplate.getSelectedItem();
                        saveStateForUndo();
                        Enemy newEnemy = (selEnemyTemplate != null) ? 
                                createEnemyFromTemplate(selEnemyTemplate, placementX, placementY, activeEditingMapID)
                                : activeEnemyBrush.create(placementX, placementY, 36, 36, activeEditingMapID, 0);
                        if(newEnemy != null){
                            currentEnemyList.add(newEnemy);
                            selectedEnemy = newEnemy;
                            propertyEditingEnemy = selectedEnemy;
                            showEnemyConfigDialog(propertyEditingEnemy);
                            gameCanvas.repaint();
                        }
                        lblBrush.setText("Selected Brush: ENEMY [" + activeEnemyBrush.name().toUpperCase() + "]");
                        gameCanvas.repaint();
                        return;
                    } 

                    if(bottomLeftCorner == null && activeBrushMode == BrushMode.OBJECT && selectedEnemy == null && selectedObject == null && selectedItem == null){
                        int finalX = freePlace ? snapX : Math.floorDiv(gamePoint.x, snapSize) * snapSize;
                        int finalY = freePlace ? snapY : ((Math.floorDiv(gamePoint.y, snapSize) + 1) * snapSize);
                        bottomLeftCorner = new Point(finalX, finalY);
                        lblX.setText("X Offset:" + snapX);

                        currentHoverPoint = new Point(gamePoint.x, gamePoint.y);
                        gameCanvas.repaint();
                    }else if (activeBrushMode == BrushMode.OBJECT && bottomLeftCorner != null){
                        try { activeEditingMapID = Integer.parseInt(txtMapID.getText().trim()); } catch (NumberFormatException ex) { activeEditingMapID = 1; }
                        
                        int realX, realY, realWidth, realHeight;
                        if(!freePlace){
                            int cellX = Math.floorDiv(gamePoint.x, snapSize);
                            int cellY = Math.floorDiv(gamePoint.y, snapSize);

                            boolean leftOfStart = (cellX * snapSize) < bottomLeftCorner.x;
                            boolean aboveStart = (cellY * snapSize) < bottomLeftCorner.y - snapSize;

                            int targetMinX = leftOfStart ? (cellX * snapSize) : bottomLeftCorner.x;
                            int targetMaxX = leftOfStart ? bottomLeftCorner.x : ((cellX + 1) * snapSize);

                            int targetMinY = aboveStart ? (cellY * snapSize) : (bottomLeftCorner.y - snapSize);
                            int targetMaxY = aboveStart ? bottomLeftCorner.y : ((cellY + 1) * snapSize);

                            realX = targetMinX;
                            realY = targetMaxY;
                            realWidth = Math.max(snapSize, targetMaxX - targetMinX);
                            realHeight = Math.max(snapSize, targetMaxY - targetMinY);
                        }else{
                            realX = Math.min(bottomLeftCorner.x, gamePoint.x);
                            realY = Math.max(bottomLeftCorner.y, gamePoint.y);
                            realWidth = Math.abs(gamePoint.x - bottomLeftCorner.x);
                            realHeight = Math.abs(gamePoint.y - bottomLeftCorner.y);
                            if(realWidth == 0) realWidth = 1;
                            if(realHeight == 0) realHeight = 1;
                        }
                        lblWidth.setText("Width Status: " + realWidth);
                        lblHeight.setText("Height Status: " + realHeight);
                        lblX.setText("X Offset: " + realX);
                        lblY.setText("Y Offset: " + realY);

                        if(gameCanvas.getGameObjects() != null){
                            saveStateForUndo();
                            GameObject newlyCreatedObject = new GameObject(realX, realY, realWidth, realHeight, activeBrushType, activeEditingMapID);
                            gameCanvas.getGameObjects().add(newlyCreatedObject);
                            showPropertyConfigDialog(newlyCreatedObject);
                        }

                        bottomLeftCorner = null;
                        currentHoverPoint = null; 
                        gameCanvas.repaint();
                    }
                }
                
                if(SwingUtilities.isMiddleMouseButton(e)){
                    propertyEditingItem = null;
                    selectedEnemy = null;
                    selectedItem = null;
                    propertyEditingEnemy = null;
                    selectedObject = null;
                    propertyEditingObject = null;
                    highlightedBroadcastChannel = 0;

                    if(bottomLeftCorner != null){
                        bottomLeftCorner = null;
                        currentHoverPoint = null;
                        gameCanvas.repaint();
                        return;
                    }

                    btnDelete.setEnabled(false);
                    lblSelectedInfo.setText("Selection: None");
                    btnEditProperties.setEnabled(false);

                    propertyEditingObject = null;
                    highlightedBroadcastChannel = 0;

                    ArrayList<GameObject> allObjects = gameCanvas.getGameObjects();
                    if(allObjects != null){
                        for(int i = allObjects.size() - 1; i >= 0; i--){
                            GameObject obj = allObjects.get(i);
                            try{
                                activeEditingMapID = Integer.parseInt(txtMapID.getText().trim());
                            }catch(Exception ex){
                                activeEditingMapID = 1;
                            }
                            int objTop = obj.y - obj.height;
                            if(obj.mapID == activeEditingMapID && gamePoint.x >= obj.x && gamePoint.x <= (obj.x + obj.width) && gamePoint.y >= objTop && gamePoint.y <= obj.y){
                                selectedObject = obj;

                                if(obj.type == GameObject.Type.trigger){
                                    highlightedBroadcastChannel = obj.activatesChannel;
                                }

                                activeBrushType = obj.type;
                                lblBrush.setText("Selected Brush: " + obj.type.name().toUpperCase());
                                lblSelectedInfo.setText(String.format("Selection: %s (%dx%d)", obj.type.name(), obj.width, obj.height));
                                lblWidth.setText("Width Status: " + obj.width);
                                lblHeight.setText("Height Status: " + obj.height);

                                btnDelete.setEnabled(true);
                                btnEditProperties.setEnabled(true);
                                break;
                            }
                        }
                    }
                    ArrayList<Enemy> allEnemies = gameCanvas.getMovement().getEnemies();
                    if(selectedEnemy == null){
                        for(int i = allEnemies.size() - 1; i >= 0; i--){
                            Enemy obj = allEnemies.get(i);
                            int objTop = obj.y - obj.height;
                            if(obj.mapID == activeEditingMapID && gamePoint.x >= obj.x && gamePoint.x <= (obj.x + obj.width) && gamePoint.y >= objTop && gamePoint.y <= obj.y){
                                selectedEnemy = obj;
                                lblSelectedInfo.setText(String.format("Selection: Enemy (%dx%d)", obj.width, obj.height));
                                lblWidth.setText("Width Status: " + obj.width);
                                lblHeight.setText("Height Status: " + obj.height);
                                btnDelete.setEnabled(true);
                                btnEditProperties.setEnabled(true);
                                break;
                            }
                        }
                    }
                    ArrayList<Item> allItems = gameCanvas.getMovement().worldItems;
                    if(selectedItem == null){
                        for(int i = allItems.size() - 1; i >= 0; i--){
                            Item obj = allItems.get(i);
                            int objTop = obj.y - obj.height;
                            if(obj.mapID == activeEditingMapID && gamePoint.x >= obj.x && gamePoint.x <= (obj.x + obj.width) && gamePoint.y >= objTop && gamePoint.y <= obj.y){
                                selectedItem = obj;
                                activeItemBrush = null;
                                activeEnemyBrush = EnemyRegistry.NONE;
                                lblSelectedInfo.setText(String.format("Selection: Item (%dx%d)", obj.width, obj.height));
                                lblWidth.setText("Width Status: " + obj.width);
                                lblHeight.setText("Height Status: " + obj.height);
                                btnDelete.setEnabled(true);
                                btnEditProperties.setEnabled(true);
                                break;
                            }
                        }
                    }
                    gameCanvas.repaint();
                    return;
                }
            }
            @Override 
            public void mouseReleased(MouseEvent e){
                saveStateForUndo();
                draggedEnemy = null;
                draggedItem = null;
                isDraggingStartPos = false;
                isDraggingTriggeredStart = false;
                isDraggingItemWaypoint = false;
                activeDraggedItemWaypointIndex = -1;
                currentResizeMode = ResizeMode.NONE;
                if(isDraggingWaypoint && propertyEditingObject != null){
                    isDraggingWaypoint = false;
                    if (propertyEditingObject != null && selectedWaypointIndex >= 0) {
                        updatePropertyDialogTextFields();
                    }
                    gameCanvas.repaint();
                }
            }
        });
        
        gameCanvas.addMouseMotionListener(new MouseMotionAdapter(){
            @Override 
            public void mouseDragged(MouseEvent e){
                Point gamePoint = getTransformedGamePoint(e);
                int snapSize = gameCanvas.getTileSize();

                // saveStateForUndo();

                Object target = propertyEditingObject != null ? propertyEditingObject : (selectedObject != null ? selectedObject : (selectedEnemy != null ? selectedEnemy : selectedItem));
                if(target != null && currentResizeMode != ResizeMode.NONE){
                    int newX = freePlace ? gamePoint.x : Math.round(gamePoint.x / (float) snapSize) * snapSize;
                    int newY = freePlace ? gamePoint.y : Math.round(gamePoint.y / (float) snapSize) * snapSize;

                    boolean resizeX = currentResizeMode != ResizeMode.TOP && currentResizeMode != ResizeMode.BOTTOM;
                    boolean resizeY = currentResizeMode != ResizeMode.LEFT && currentResizeMode != ResizeMode.RIGHT;

                    int currentX = (target instanceof GameObject) ? ((GameObject) target).x : ((target instanceof Enemy) ? ((Enemy) target).x : ((Item) target).x);
                    int currentY = (target instanceof GameObject) ? ((GameObject) target).y : ((target instanceof Enemy) ? ((Enemy) target).y : ((Item) target).y);
                    int currentW = (target instanceof GameObject) ? ((GameObject) target).width : ((target instanceof Enemy) ? ((Enemy) target).width : ((Item) target).width);
                    int currentH = (target instanceof GameObject) ? ((GameObject) target).height : ((target instanceof Enemy) ? ((Enemy) target).height : ((Item) target).height);
                    
                    int newXPos = currentX;
                    int newWidth = currentW;

                    if(resizeX){
                        newXPos = Math.min(resizeAnchorX, newX);
                        newWidth = Math.abs(newX - resizeAnchorX);
                        int minW = freePlace ? 1 : snapSize;
                        if(newWidth < minW){
                            newWidth = minW;
                            newXPos = (newX < resizeAnchorX) ? resizeAnchorX - minW : resizeAnchorX;
                        }

                        boolean isLeft = newX < resizeAnchorX;
                        if(currentResizeMode == ResizeMode.LEFT || currentResizeMode == ResizeMode.RIGHT){
                            currentResizeMode = isLeft ? ResizeMode.LEFT : ResizeMode.RIGHT;
                        }else{
                            boolean isTop = (currentResizeMode == ResizeMode.TOP_LEFT || currentResizeMode == ResizeMode.TOP_RIGHT);
                            currentResizeMode = isTop ? (isLeft ? ResizeMode.TOP_LEFT : ResizeMode.TOP_RIGHT) : (isLeft ? ResizeMode.BOTTOM_LEFT : ResizeMode.BOTTOM_RIGHT);
                        }
                    }

                    int newBottomY = currentY;
                    int newHeight = currentH;
                    if(resizeY){
                        int newTopY = Math.min(resizeAnchorY, newY);
                        newHeight = Math.abs(newY - resizeAnchorY);
                        int minH = freePlace ? 1: snapSize;
                        if(newHeight < minH){
                            newHeight = minH;
                            newTopY = (newY < resizeAnchorY) ? resizeAnchorY - minH : resizeAnchorY;
                        }
                        newBottomY = newTopY + newHeight;

                        boolean isTop = newY < resizeAnchorY;
                        if(currentResizeMode == ResizeMode.TOP || currentResizeMode == ResizeMode.BOTTOM){
                            currentResizeMode = isTop ? ResizeMode.TOP : ResizeMode.BOTTOM;
                        }else{
                            boolean isLeft = (currentResizeMode == ResizeMode.TOP_LEFT || currentResizeMode == ResizeMode.BOTTOM_LEFT);
                            currentResizeMode = isTop ? (isLeft ? ResizeMode.TOP_LEFT : ResizeMode.TOP_RIGHT) : (isLeft ? ResizeMode.BOTTOM_LEFT : ResizeMode.BOTTOM_RIGHT);
                        }
                    }

                    if (target instanceof GameObject) {
                        GameObject obj = (GameObject) target;
                        obj.x = newXPos;
                        obj.y = newBottomY;
                        obj.width = newWidth;
                        obj.height = newHeight;
                        obj.currentX = newXPos;
                        obj.currentY = newBottomY;
                    } else if (target instanceof Enemy) {
                        Enemy en = (Enemy) target;
                        en.x = newXPos;
                        en.y = newBottomY;
                        en.startX = newXPos;
                        en.startY = newBottomY;
                        en.width = newWidth;
                        en.height = newHeight;
                    }else if(target instanceof Item){
                        Item it = (Item) target;
                        it.x = newXPos;
                        it.y = newBottomY;
                        it.startX = newXPos;
                        it.startY = newBottomY;
                        it.width = newWidth;
                        it.height = newHeight;
                    }
                    lblWidth.setText("Width Status: " + newWidth);
                    lblHeight.setText("Height Status: " + newHeight);
                    gameCanvas.repaint();
                    return;
                }
                
                if (selectedObject != null || propertyEditingObject != null) {
                    GameObject obj = propertyEditingObject != null ? propertyEditingObject : selectedObject;
                    int rawX = gamePoint.x - dragOffsetX;
                    int rawTopLeftY = gamePoint.y - dragOffsetY;
                    int newX = freePlace ? rawX : Math.round(rawX / (float) snapSize) * snapSize;
                    int newTopLeftY = freePlace ? rawTopLeftY : Math.round(rawTopLeftY / (float) snapSize) * snapSize;
                    int newY = newTopLeftY + obj.height;
                    if(isDraggingStartPos){
                        int deltaX = newX - obj.x;
                        int deltaY = newY - obj.y;
                        obj.x = newX;
                        obj.y = newY;
                        obj.currentX = newX;
                        obj.currentY = newY;

                        if(e.isControlDown()){
                            for(int i = 0; i < obj.pathX.size(); i++){
                                obj.pathX.set(i, obj.pathX.get(i) + deltaX);
                                obj.pathY.set(i, obj.pathY.get(i) + deltaY);
                            }
                        }

                        gameCanvas.repaint();
                        return;
                    }

                    if(isDraggingTriggeredStart){
                        int deltaX = newX - obj.triggeredX;
                        int deltaY = newY - obj.triggeredY;
                        obj.triggeredX = newX;
                        obj.triggeredY = newY;
                        if(e.isControlDown()){
                            for(int i = 0; i < obj.triggeredPathX.size(); i++){
                                obj.triggeredPathX.set(i, obj.triggeredPathX.get(i) + deltaX);
                                obj.triggeredPathY.set(i, obj.triggeredPathY.get(i) + deltaY);
                            }
                        }
                        gameCanvas.repaint();
                        return;
                    }

                    if(isDraggingWaypoint && activeDraggedWaypointIndex >= 0){
                        ArrayList<Integer> px = activeDraggedIsTriggered ? obj.triggeredPathX : obj.pathX;
                        ArrayList<Integer> py = activeDraggedIsTriggered ? obj.triggeredPathY : obj.pathY;

                        if (activeDraggedWaypointIndex < px.size()) {
                            int oldX = px.get(activeDraggedWaypointIndex);
                            int oldY = py.get(activeDraggedWaypointIndex);
                            int deltaX = newX - oldX;
                            int deltaY = newY - oldY;

                            px.set(activeDraggedWaypointIndex, newX);
                            py.set(activeDraggedWaypointIndex, newY);

                            if(e.isControlDown()){
                                if(!activeDraggedIsTriggered){
                                    obj.x += deltaX;
                                    obj.y += deltaY;
                                    obj.currentX += deltaX;
                                    obj.currentY += deltaY;
                                    for(int i = 0; i < obj.pathX.size(); i++){
                                        if(i != activeDraggedWaypointIndex){
                                            obj.pathX.set(i, obj.pathX.get(i) + deltaX);
                                            obj.pathY.set(i, obj.pathY.get(i) + deltaY);
                                        }
                                    }
                                }else{
                                    obj.triggeredX += deltaX;
                                    obj.triggeredY += deltaY;
                                    for(int i = 0; i < obj.pathX.size(); i++){
                                        if(i != activeDraggedWaypointIndex){
                                            obj.triggeredPathX.set(i, obj.triggeredPathX.get(i) + deltaX);
                                            obj.triggeredPathY.set(i, obj.triggeredPathY.get(i) + deltaY);
                                        }
                                    }
                                }
                            }

                            updatePropertyDialogTextFields();
                        }
                        gameCanvas.repaint();
                    }
                }else if(draggedEnemy != null){
                    int rawX = gamePoint.x - dragOffsetX;
                    int rawTopLeftY = gamePoint.y - dragOffsetY;
                    int newX = freePlace ? rawX : Math.round(rawX / (float) snapSize) * snapSize;
                    int newTopLeftY = freePlace ? rawTopLeftY : Math.round(rawTopLeftY / (float) snapSize) * snapSize;
                    int newY = newTopLeftY + draggedEnemy.height;

                    draggedEnemy.x = newX;
                    draggedEnemy.y = newY;
                    draggedEnemy.startX = newX;
                    draggedEnemy.startY = newY;
                    gameCanvas.repaint();
                }else if(selectedItem != null && activeDraggedItemWaypointIndex == -1 && (SwingUtilities.isLeftMouseButton(e) || SwingUtilities.isRightMouseButton(e))){
                    Item it = selectedItem;
                    int rawX = gamePoint.x - dragOffsetX;
                    int rawTopLeftY = gamePoint.y - dragOffsetY;
                    int newX = freePlace ? rawX : Math.round(rawX / (float) snapSize) * snapSize;
                    int newTopLeftY = freePlace ? rawTopLeftY : Math.round(rawTopLeftY / (float) snapSize) * snapSize;
                    int newY = newTopLeftY + it.height;

                    int deltaX = newX - it.x;
                    int deltaY = newY - it.y;

                    it.x = newX;
                    it.y = newY;
                    it.startX = newX;
                    it.startY = newY;

                    if(e.isControlDown()){
                        for(int i = 0; i < it.attackX.size(); i++){
                            it.attackX.set(i, it.attackX.get(i) + deltaX);
                            it.attackY.set(i, it.attackY.get(i) + deltaY);
                        }
                    }
                    gameCanvas.repaint();
                    return;
                }
                else if(isDraggingItemWaypoint && activeDraggedItemWaypointIndex >= 0 && selectedItem != null){
                    Item it = selectedItem;
                    int rawX = gamePoint.x - dragOffsetX;
                    int rawTopLeftY = gamePoint.y - dragOffsetY;
                    int newX = freePlace ? rawX : Math.round(rawX / (float) snapSize) * snapSize;
                    int newTopLeftY = freePlace ? rawTopLeftY : Math.round(rawTopLeftY / (float) snapSize) * snapSize;
                    int newY = newTopLeftY + it.height;

                    int oldX = it.attackX.get(activeDraggedItemWaypointIndex);
                    int oldY = it.attackY.get(activeDraggedItemWaypointIndex);
                    int deltaX = newX - oldX;
                    int deltaY = newY - oldY;

                    it.attackX.set(activeDraggedItemWaypointIndex, newX);
                    it.attackY.set(activeDraggedItemWaypointIndex, newY);

                    if(e.isControlDown()){
                        it.x += deltaX;
                        it.y += deltaY;
                        it.startX += deltaX;
                        it.startY += deltaY;
                        for(int i = 0; i < it.attackX.size(); i++){
                            if(i != activeDraggedItemWaypointIndex){
                                it.attackX.set(i, it.attackX.get(i) + deltaX);
                                it.attackY.set(i, it.attackY.get(i) + deltaY);
                            }
                        }
                    }
                    gameCanvas.repaint();
                    return;
                }
            }

            @Override 
            public void mouseMoved(MouseEvent e){
                Point gamePoint = getTransformedGamePoint(e);
                int snapSize = gameCanvas.getTileSize();

                currentHoverResizeMode = ResizeMode.NONE;
                Object target = propertyEditingObject != null ? propertyEditingObject : (selectedObject != null ? selectedObject : (selectedEnemy != null ? selectedEnemy : selectedItem));
                if(target != null){
                    int tX = 0, tY = 0, tW = 0, tH = 0;
                    if(target instanceof GameObject){
                        GameObject o = (GameObject) target;
                        tX = o.x; tY = o.y - o.height; tW = o.width; tH = o.height;
                    }else if(target instanceof Enemy){
                        Enemy en = (Enemy) target;
                        tX = en.x; tY = en.y - en.height; tW = en.width; tH = en.height;
                    }else if(target instanceof Item){
                        Item it = (Item) target;
                        tX = it.x; tY = it.y - it.height; tW = it.width; tH = it.height;
                    }

                    int handleThresh = 6;
                    Rectangle bounds = new Rectangle(tX + 1, tY - 1, tW + 2, tH + 2);
                    Rectangle inner = new Rectangle(tX + handleThresh, tY + handleThresh, Math.max(1, tW - handleThresh * 2), Math.max(1, tH - handleThresh * 2));

                    if(bounds.contains(gamePoint) && !inner.contains(gamePoint)){
                        boolean isLeft = Math.abs(gamePoint.x - tX) <= handleThresh;
                        boolean isRight = Math.abs(gamePoint.x - (tX + tW)) <= handleThresh;
                        boolean isTop = Math.abs(gamePoint.y - tY) <= handleThresh;
                        boolean isBottom = Math.abs(gamePoint.y - (tY + tH)) <= handleThresh;

                        if (isTop && isLeft) currentHoverResizeMode = ResizeMode.TOP_LEFT;
                        else if (isTop && isRight) currentHoverResizeMode = ResizeMode.TOP_RIGHT;
                        else if (isBottom && isLeft) currentHoverResizeMode = ResizeMode.BOTTOM_LEFT;
                        else if (isBottom && isRight) currentHoverResizeMode = ResizeMode.BOTTOM_RIGHT;
                        else if (isLeft) currentHoverResizeMode = ResizeMode.LEFT;
                        else if (isRight) currentHoverResizeMode = ResizeMode.RIGHT;
                        else if (isTop) currentHoverResizeMode = ResizeMode.TOP;
                        else if (isBottom) currentHoverResizeMode = ResizeMode.BOTTOM;
                    }
                }
                if (currentHoverResizeMode != ResizeMode.NONE) {
                    switch (currentHoverResizeMode) {
                        
                        case TOP: gameCanvas.setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR)); break;
                        case LEFT: gameCanvas.setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR)); break;
                        case RIGHT: gameCanvas.setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)); break;
                        case BOTTOM: gameCanvas.setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR)); break;
                        case TOP_LEFT: gameCanvas.setCursor(Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR)); break;
                        case TOP_RIGHT: gameCanvas.setCursor(Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR)); break;
                        case BOTTOM_LEFT: gameCanvas.setCursor(Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR)); break;
                        case BOTTOM_RIGHT: gameCanvas.setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR)); break;
                        default: gameCanvas.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)); break;
                    }
                } else {
                    gameCanvas.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }

                if(selectedObject != null && currentPathClickMode != PathClickMode.NONE){
                    pathHoverPoint = new Point(gamePoint.x, gamePoint.y);
                }else{
                    pathHoverPoint = null;
                }

                if(bottomLeftCorner != null){
                    currentHoverPoint = new Point(gamePoint.x, gamePoint.y);
                    int realWidth, realHeight;
                    if(!freePlace){
                        int cellX = Math.floorDiv(gamePoint.x, snapSize);
                        int cellY = Math.floorDiv(gamePoint.y, snapSize);
                        
                        boolean leftOfStart = (cellX * snapSize) < bottomLeftCorner.x;
                        boolean aboveStart = (cellY * snapSize) < (bottomLeftCorner.y - snapSize);

                        int targetMinX = leftOfStart ? (cellX * snapSize) : bottomLeftCorner.x;
                        int targetMaxX = leftOfStart ? bottomLeftCorner.x : ((cellX + 1) * snapSize);
                        
                        int targetMinY = aboveStart ? (cellY * snapSize) : (bottomLeftCorner.y - snapSize);
                        int targetMaxY = aboveStart ? bottomLeftCorner.y : ((cellY + 1) * snapSize);

                        realWidth = Math.max(snapSize, targetMaxX - targetMinX);
                        realHeight = Math.max(snapSize, targetMaxY - targetMinY);
                    } else {
                        realWidth = Math.abs(gamePoint.x - bottomLeftCorner.x);
                        realHeight = Math.abs(gamePoint.y - bottomLeftCorner.y);
                    }
                    lblWidth.setText("Width Status: " + (realWidth == 0 ? snapSize : realWidth));
                    lblHeight.setText("Height Status: " + (realHeight == 0 ? snapSize : realHeight));
                }
                gameCanvas.repaint();
            }
        });
    }
    
    //saving + loading objects
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
            }catch(Exception e){}
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
            }catch(Exception e){}
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
                    boolean isTriggered = false, triggeredDisplay = true;
                    int triggeredX = -1, triggeredY = -1, triggeredBy = 0, activatesChannel = 0;
                    int time = 0, timeElapsed = 0, moveX = 0, moveY = 0, triggerMoveX = 0, triggerMoveY = 0;
                    GameObject.Type type = GameObject.Type.solid;
                    GameObject.movement move = GameObject.movement.never;
                    boolean returnReverse = false;

                    ArrayList<Integer> pX = new ArrayList<>();
                    ArrayList<Integer> pY = new ArrayList<>();
                    ArrayList<Double> pSpd = new ArrayList<>();
                    ArrayList<Boolean> pIsSpd = new ArrayList<>();

                    ArrayList<Integer> tPX = new ArrayList<>();
                    ArrayList<Integer> tPY = new ArrayList<>();
                    ArrayList<Double> tPSpd = new ArrayList<>();
                    ArrayList<Boolean> tPIsSpd = new ArrayList<>();

                    String[] properties = rawObj.split(",(?![^\\[]*\\])");
                    for(String prop : properties){
                        int colonIdx = prop.indexOf(":");
                        if(colonIdx == -1) continue;

                        String key = prop.substring(0, colonIdx).replace("\"", "").trim();
                        String val = prop.substring(colonIdx + 1).replace("\"", "").trim();
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
                            case "returnReverse" : returnReverse = Boolean.parseBoolean(val); break;
                            case "moveX": moveX = Integer.parseInt(val); break;
                            case "moveY": moveY = Integer.parseInt(val); break;
                            case "triggerMoveX": triggerMoveX = Integer.parseInt(val); break;
                            case "triggerMoveY": triggerMoveY = Integer.parseInt(val); break;
                            case "pathX":
                                if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) pX.add(Integer.parseInt(s.trim()));
                                break;
                            case "pathY":
                                if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) pY.add(Integer.parseInt(s.trim()));
                                break;
                            case "pathSpeed":
                                if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) pSpd.add(Double.parseDouble(s.trim()));
                                break;
                            case "pathSpeedIsSpeed":
                                if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) pIsSpd.add(Boolean.parseBoolean(s.trim()));
                                break;
                            case "triggeredPathX":
                                if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) tPX.add(Integer.parseInt(s.trim()));
                                break;
                            case "triggeredPathY":
                                if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) tPY.add(Integer.parseInt(s.trim()));
                                break;
                            case "triggeredPathSpeed":
                                if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) tPSpd.add(Double.parseDouble(s.trim()));
                                break;
                            case "triggeredSpeedIsSpeed":
                                if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) tPIsSpd.add(Boolean.parseBoolean(s.trim()));
                                break;
                        }
                    }
                    if(triggeredX == -1 && triggeredY == -1){ triggeredX = x; triggeredY = y; }
                    if (moveX == 0 && moveY == 0) { moveX = x; moveY = y; }
                    if (triggerMoveX == 0 && triggerMoveY == 0) { triggerMoveX = triggeredX; triggerMoveY = triggeredY; }

                    masterList.add(new GameObject(x, y, width, height, type, mapID, isTriggered, triggeredX, triggeredY, 
                        triggeredDisplay, triggeredBy, activatesChannel, time, timeElapsed, move, returnReverse, moveX, moveY, triggerMoveX, triggerMoveY, x, y,
                        pX, pY, pSpd, pIsSpd, tPX, tPY, tPSpd, tPIsSpd));
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
                    "\"time\":%d, \"timeElapsed\":%d, \"move\":\"%s\", \"returnReverse\":%b, \"moveX\":%d, \"moveY\":%d, \"triggerMoveX\":%d, \"triggerMoveY\":%d, " +
                    "\"pathX\":%s, \"pathY\":%s, \"pathSpeed\":%s, \"pathSpeedIsSpeed\":%s, " +
                    "\"triggeredPathX\":%s, \"triggeredPathY\":%s, \"triggeredPathSpeed\":%s, \"triggeredPathSpeedIsSpeed\":%s}", 
                    obj.x, obj.y, obj.width, obj.height, obj.type.name(), obj.mapID, obj.isTriggered, 
                    obj.triggeredX, obj.triggeredY, obj.triggeredDisplay, obj.triggeredBy, obj.activatesChannel,
                    obj.time, obj.timeElapsed, obj.move.name(),obj.returnReverse, obj.moveX, obj.moveY, obj.triggerMoveX, obj.triggerMoveY,
                    obj.pathX.toString(), obj.pathY.toString(), obj.pathSpeed.toString(), obj.pathSpeedIsSpeed.toString(),
                    obj.triggeredPathX.toString(), obj.triggeredPathY.toString(), obj.triggeredPathSpeed.toString(), obj.triggeredPathSpeedIsSpeed.toString()
                );
                writer.write(" "+ jsonStr);
                if(i < masterList.size() - 1){
                    writer.write(",\n");
                }else{
                    writer.write("\n");
                }
            }
            writer.write("]");
        }catch (java.io.IOException e){}
    }
    
    private void loadMapManual(String filePath) {
        ArrayList<GameObject> loadedObjects = new ArrayList<>();
        java.io.File file = new java.io.File(filePath);
        String jsonStringData = "";

        if (file.exists()) {
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
                jsonStringData = content.toString().trim();
            } catch (Exception e) {}
        } else {
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
            } catch (Exception e) {}
        }

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
                    boolean returnReverse = false;

                    ArrayList<Integer> pX = new ArrayList<>();
                    ArrayList<Integer> pY = new ArrayList<>();
                    ArrayList<Double> pSpd = new ArrayList<>();
                    ArrayList<Boolean> pIsSpd = new ArrayList<>();

                    ArrayList<Integer> tPX = new ArrayList<>();
                    ArrayList<Integer> tPY = new ArrayList<>();
                    ArrayList<Double> tPSpd = new ArrayList<>();
                    ArrayList<Boolean> tPIsSpd = new ArrayList<>();

                    String[] properties = rawObj.split(",(?![^\\[]*\\])");
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
                            case "returnReverse" : returnReverse = Boolean.parseBoolean(val); break;
                            case "moveX": moveX = Integer.parseInt(val); break;
                            case "moveY": moveY = Integer.parseInt(val); break;
                            case "triggerMoveX": triggerMoveX = Integer.parseInt(val); break;
                            case "triggerMoveY": triggerMoveY = Integer.parseInt(val); break;
                            case "pathX":
                                if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) pX.add(Integer.parseInt(s.trim()));
                                break;
                            case "pathY":
                                if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) pY.add(Integer.parseInt(s.trim()));
                                break;
                            case "pathSpeed":
                                if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) pSpd.add(Double.parseDouble(s.trim()));
                                break;
                            case "pathSpeedIsSpeed":
                                if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) pIsSpd.add(Boolean.parseBoolean(s.trim()));
                                break;
                            case "triggeredPathX":
                                if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) tPX.add(Integer.parseInt(s.trim()));
                                break;
                            case "triggeredPathY":
                                if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) tPY.add(Integer.parseInt(s.trim()));
                                break;
                            case "triggeredPathSpeed":
                                if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) tPSpd.add(Double.parseDouble(s.trim()));
                                break;
                            case "triggeredSpeedIsSpeed":
                                if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) tPIsSpd.add(Boolean.parseBoolean(s.trim()));
                                break;
                        }
                    }
                    if (triggeredX == -1 && triggeredY == -1) { triggeredX = x; triggeredY = y; }
                    if (moveX == 0 && moveY == 0) { moveX = x; moveY = y; }
                    if (triggerMoveX == 0 && triggerMoveY == 0) { triggerMoveX = triggeredX; triggerMoveY = triggeredY; }
                    
                    loadedObjects.add(new GameObject(x, y, width, height, type, mapID, isTriggered, triggeredX, triggeredY,
                        triggeredDisplay, triggeredBy, activatesChannel, time, timeElapsed, move, returnReverse, moveX, moveY, triggerMoveX, triggerMoveY, x, y,
                        pX, pY, pSpd, pIsSpd, tPX, tPY, tPSpd, tPIsSpd));
                }
            }
        }

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

    //saving + loading enemies
    private void saveEnemiesManual(ArrayList<Enemy> currentEditingEnemies, String filePath){
        ArrayList<Enemy> masterEnemyList = new ArrayList<>();
        java.io.File file = new java.io.File(filePath);
        String json = "";

        if(file.exists()){
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))) {
                StringBuilder content = new StringBuilder();
                String line;
                while((line = reader.readLine()) != null){
                    content.append(line);
                }
                json = content.toString().trim();
            }catch (IOException e){}
        }
        if(!json.isEmpty()){
            if(json.startsWith("[") && json.endsWith("]")){
                json = json.substring(1, json.length() - 1).trim();
            }
            if(!json.isEmpty()){
                String[] enemiesRaw = json.split("\\},\\s*\\{");
                for(String rawEnemy : enemiesRaw){
                    rawEnemy = rawEnemy.replace("{", "").replace("}", "").trim();
                    int x = 0, y = 0, mapID = 1, width = 36, height = 36;
                    double jumpHeight = -15.5;
                    int intervalMin = 60, intervalMax = 180;
                    double velX = 0;
                    double xRange = 64.0, yRange = 32.0;
                    double damage = 10.0;
                    double maxHealth = 100.0, regeneration = 0.0;
                    String type = "PATROL";
                    Boolean hasPathfinding = false;

                    String[] properties = rawEnemy.split(",");
                    for(String prop : properties){
                        String[] kv = prop.split(":");
                        if(kv.length != 2) continue;
                        String key = kv[0].replace("\"", "").trim();
                        String val = kv[1].replace("\"", "").trim();
                        switch (key){
                            case "x": x = Integer.parseInt(val); break;
                            case "y": y = Integer.parseInt(val); break;
                            case "width" : width = Integer.parseInt(val); break;
                            case "height" : height = Integer.parseInt(val); break;
                            case "maxHealth": maxHealth = Double.parseDouble(val); break;
                            case "regeneration": regeneration = Double.parseDouble(val); break;
                            case "mapID" : mapID = Integer.parseInt(val); break;
                            case "velX" : velX = Double.parseDouble(val); break;
                            case "xRange" : xRange = Double.parseDouble(val); break;
                            case "yRange" : yRange = Double.parseDouble(val); break;
                            case "damage": damage = Double.parseDouble(val); break;
                            case "jumpHeight": jumpHeight = Double.parseDouble(val); break;
                            case "intervalMin": intervalMin = Integer.parseInt(val); break;
                            case "intervalMax": intervalMax = Integer.parseInt(val); break;
                            case "hasPathfinding" : hasPathfinding = Boolean.parseBoolean(val); break;
                            case "type": type = val; break;
                        }
                    }
                    Enemy enemy = EnemyRegistry.createFromString(type, x, y, width, height, mapID, velX);
                    if(enemy != null){
                        enemy.hasPathfinding = hasPathfinding;
                        enemy.maxHealth = maxHealth;
                        enemy.regeneration = regeneration;
                        enemy.damage = damage;
                    }
                    if(enemy instanceof FlyingEnemy){
                        ((FlyingEnemy) enemy).setXRange(xRange);
                        ((FlyingEnemy) enemy).setYRange(yRange);
                    }
                    if(enemy instanceof JumpingEnemy){
                        ((JumpingEnemy) enemy).setJumpHeight(jumpHeight);
                        ((JumpingEnemy) enemy).setIntervalMax(intervalMax);
                        ((JumpingEnemy) enemy).setIntervalMin(intervalMin);
                    }
                    if (enemy != null) masterEnemyList.add(enemy);
                }
            }
        }
        masterEnemyList.removeIf(enemy -> enemy.mapID == activeEditingMapID);
        for(Enemy e : currentEditingEnemies){
            if(e.mapID == activeEditingMapID){
                masterEnemyList.add(e);
            }
        }
        try(java.io.FileWriter writer = new java.io.FileWriter(filePath)){
            writer.write("[\n");
            for(int i = 0; i < masterEnemyList.size(); i++){
                Enemy e = masterEnemyList.get(i);
                String jsonStr;

                double initialSpeed = (e instanceof FlyingEnemy) ? ((FlyingEnemy) e).getXSpeed() : e.velX;

                if(e instanceof FlyingEnemy){
                    FlyingEnemy fe = (FlyingEnemy) e;
                    jsonStr = String.format("{\"x\":%d, \"y\":%d, \"width\":%d, \"height\":%d, \"velX\":%.4f, \"xRange\":%.2f, \"yRange\":%.2f, \"maxHealth\":%.2f, \"regeneration\":%.2f, \"hasPathfinding\":%b, \"damage\":%.2f, \"mapID\":%d, \"type\":\"%s\"}", 
                    e.startX, e.startY, e.width, e.height, initialSpeed, fe.getXRange(), fe.getYRange(), e.maxHealth, e.regeneration, e.hasPathfinding, e.damage, e.mapID, e.type);
                }else if(e instanceof JumpingEnemy){
                    JumpingEnemy je = (JumpingEnemy) e;
                    jsonStr = String.format("{\"x\":%d, \"y\":%d, \"width\":%d, \"height\":%d, \"velX\":%.4f, \"jumpHeight\":%.4f, \"intervalMax\":%d, \"intervalMin\":%d, \"maxHealth\":%.2f, \"regeneration\":%.2f, \"hasPathfinding\":%b, \"damage\":%.2f, \"mapID\":%d, \"type\":\"%s\"}", 
                    e.startX, e.startY, e.width, e.height, initialSpeed, je.getJumpHeight(), je.getIntervalMax(), je.getIntervalMin(), e.maxHealth, e.regeneration, e.hasPathfinding, e.damage, e.mapID, e.type);
                }else{
                    jsonStr = String.format("{\"x\":%d, \"y\":%d, \"width\":%d, \"height\":%d, \"velX\":%.4f, \"maxHealth\":%.2f, \"regeneration\":%.2f, \"hasPathfinding\":%b, \"damage\":%.2f, \"mapID\":%d, \"type\":\"%s\"}", 
                    e.startX, e.startY, e.width, e.height, initialSpeed, e.maxHealth, e.regeneration, e.hasPathfinding, e.damage, e.mapID, e.type);
                }
                writer.write(" " + jsonStr);
                if(i < masterEnemyList.size() - 1) writer.write(",\n");
                else writer.write("\n");
            }
            writer.write("]");
        }catch (java.io.IOException e){}
    }

    private void loadEnemiesManual(String filePath) {
        ArrayList<Enemy> loadedEnemies = new ArrayList<>();
        java.io.File file = new java.io.File(filePath);
        String jsonStringData = "";

        if (file.exists()) {
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
                jsonStringData = content.toString().trim();
            } catch (Exception e) {}
        }

        if (!jsonStringData.isEmpty()) {
            if (jsonStringData.startsWith("[") && jsonStringData.endsWith("]")) {
                jsonStringData = jsonStringData.substring(1, jsonStringData.length() - 1).trim();
            }
            if (!jsonStringData.isEmpty()) {
                String[] enemiesRaw = jsonStringData.split("\\},\\s*\\{");
                for (String rawEnemy : enemiesRaw) {
                    rawEnemy = rawEnemy.replace("{", "").replace("}", "").trim();
                    int x = 0, y = 0, mapID = 1, width = 36, height = 36;
                    double velX = 0.1;
                    double jumpHeight = -15.5;
                    int intervalMax = 180, intervalMin = 60;
                    double xRange = 64.0;
                    double yRange = 32.0;
                    double damage = 10.0;
                    double maxHealth = 100.0, regeneration = 0.0;
                    String type = "PATROL";
                    boolean hasPathfinding = false;

                    String[] properties = rawEnemy.split(",");
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
                            case "velX": velX = Double.parseDouble(val); break;
                            case "xRange": xRange = Double.parseDouble(val); break;
                            case "yRange": yRange = Double.parseDouble(val); break;
                            case "maxHealth": maxHealth = Double.parseDouble(val); break;
                            case "regeneration": regeneration = Double.parseDouble(val); break;
                            case "jumpHeight": jumpHeight = Double.parseDouble(val); break;
                            case "intervalMin": intervalMin = Integer.parseInt(val); break;
                            case "intervalMax": intervalMax = Integer.parseInt(val); break;
                            case "hasPathfinding" : hasPathfinding = Boolean.parseBoolean(val); break;
                            case "damage": damage = Double.parseDouble(val); break;
                            case "mapID": mapID = Integer.parseInt(val); break;
                            case "type": type = val; break;
                        }
                    }
                    Enemy enemy = EnemyRegistry.createFromString(type, x, y, width, height, mapID, velX);
                    if(enemy != null){
                        enemy.hasPathfinding = hasPathfinding;
                        enemy.currentHealth = maxHealth;
                        enemy.maxHealth = maxHealth;
                        enemy.regeneration = regeneration;
                        enemy.damage = damage;
                    }
                    if(enemy instanceof FlyingEnemy){
                        ((FlyingEnemy) enemy).setXRange(xRange);
                        ((FlyingEnemy) enemy).setYRange(yRange);
                    }
                    if(enemy instanceof JumpingEnemy){
                        ((JumpingEnemy)enemy).setJumpHeight(jumpHeight);
                        ((JumpingEnemy)enemy).setIntervalMax(intervalMax);
                        ((JumpingEnemy)enemy).setIntervalMin(intervalMin);
                    }
                    if (enemy != null) loadedEnemies.add(enemy);
                }
            }
        }

        if (gameCanvas.getMovement() != null) {
            gameCanvas.getMovement().getEnemies().clear();
            gameCanvas.getMovement().getEnemies().addAll(loadedEnemies);
            gameCanvas.repaint();
        }
    }

    //saving + loading items
    private void saveItemsManual(ArrayList<Item> currentEditingItems, String filePath){
        ArrayList<Item> masterItemList = new ArrayList<>();
        java.io.File file = new java.io.File(filePath);
        String json = "";

        if(file.exists()){
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))) {
                StringBuilder content = new StringBuilder();
                String line;
                while((line = reader.readLine()) != null){
                    content.append(line);
                }
                json = content.toString().trim();
            }catch(IOException e){}
        }
        if(!json.isEmpty()){
            if(json.startsWith("[") && json.endsWith("]")){
                json = json.substring(1, json.length() - 1).trim();
            }
            if(!json.isEmpty()){
                String[] itemsRaw = json.split("\\},\\s*\\{");
                for(String rawItem : itemsRaw){
                    rawItem = rawItem.replace("{", "").replace("}", "").trim();
                    int x = 0, y = 0, mapID = 1, width = 32, height = 32;
                    String name = "COIN", type = "MISC", effects = "";
                    Color color = Color.YELLOW;

                    Boolean reverse = false;

                    ArrayList<Integer> attX = new ArrayList<>();
                    ArrayList<Integer> attY = new ArrayList<>();
                    ArrayList<Double> spd = new ArrayList<>();
                    ArrayList<Boolean> isSpd = new ArrayList<>();

                    String[] properties = rawItem.split(",(?![^\\[]*\\])");
                    for(String prop : properties){
                        int colonIdx = prop.indexOf(":");
                        if(colonIdx == -1) continue;
                        String key = prop.substring(0, colonIdx).replace("\"", "").trim();
                        String val = prop.substring(colonIdx + 1).replace("\"", "").trim();
                        switch(key){
                            case "x": x = Integer.parseInt(val); break;
                            case "y": y = Integer.parseInt(val); break;
                            case "width": width = Integer.parseInt(val); break;
                            case "height": height = Integer.parseInt(val); break;
                            case "mapID": mapID = Integer.parseInt(val); break;
                            case "name": name = val; break;
                            case "type": type = val; break;
                            case "effects": effects = val; break;
                            case "reverseDirection": reverse = Boolean.parseBoolean(val); break;
                            case "colorRGB": color = new Color(Integer.parseInt(val)); break;
                            case "attackX":
                                if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) attX.add(Integer.parseInt(s.trim()));
                                break;
                            case "attackY":
                                if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) attY.add(Integer.parseInt(s.trim()));
                                break;
                            case "speed":
                                if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) spd.add(Double.parseDouble(s.trim()));
                                break;
                            case "isSpeed":
                                if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) isSpd.add(Boolean.parseBoolean(s.trim()));
                                break;
                        }
                    }
                    Item item = new Item(name, Item.ItemType.valueOf(type), color, x, y, width, height, mapID, effects, reverse, attX, attY, spd, isSpd);
                    masterItemList.add(item);
                }
            }
        }
        masterItemList.removeIf(item -> item.mapID == activeEditingMapID);
        for(Item i : currentEditingItems){
            if(i.mapID == activeEditingMapID){
                masterItemList.add(i);
            }
        }
        try(java.io.FileWriter writer = new java.io.FileWriter(filePath)){
            writer.write("[\n");
            for(int i = 0; i < masterItemList.size(); i++){
                Item item = masterItemList.get(i);
                String jsonStr = String.format("{\"name\":\"%s\", \"type\":\"%s\", \"colorRGB\":%d, \"x\":%d, \"y\":%d, \"width\":%d, \"height\":%d, \"mapID\":%d, \"effects\":\"%s\", \"reverseDirection\":\"%b\" ,\"attackX\":%s, \"attackY\":%s, \"speed\":%s, \"isSpeed\":%s}",
                    item.name, item.type.name(), item.color.getRGB(), item.startX, item.startY, item.width, item.height, item.mapID, item.effects, item.reverseDirection,
                    item.attackX.toString(), item.attackY.toString(), item.speed.toString(), item.isSpeed.toString());
                writer.write(" " + jsonStr);
                if(i < masterItemList.size() - 1) writer.write(",\n");
                else writer.write("\n");
            }
            writer.write("]");
        }catch (java.io.IOException e){}
    }

    private void loadItemsManual(String filePath){
        ArrayList<Item> loadedItems = new ArrayList<>();
        java.io.File file = new java.io.File(filePath);
        String jsonStringData = "";

        if(file.exists()){
            try(java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))){
                StringBuilder content = new StringBuilder();
                String line;
                while((line = reader.readLine()) != null){
                    content.append(line);
                }
                jsonStringData = content.toString().trim();
            }catch(IOException e){}
        }

        if(!jsonStringData.isEmpty()){
            if(jsonStringData.startsWith("[") && jsonStringData.endsWith("]")){
                jsonStringData = jsonStringData.substring(1, jsonStringData.length() - 1);
            }
            if(!jsonStringData.isEmpty()){
                String[] itemsRaw = jsonStringData.split("\\},\\s*\\{");
                for(String rawItem : itemsRaw){
                    rawItem = rawItem.replace("{", "").replace("}", "").trim();
                    int x = 0, y = 0, mapID = 1, width = 32, height = 32;
                    String name = "COIN", type = "MISC", effects = "";
                    Color color = Color.YELLOW;

                    Boolean reverse = false;

                    ArrayList<Integer> attX = new ArrayList<>();
                    ArrayList<Integer> attY = new ArrayList<>();
                    ArrayList<Double> spd = new ArrayList<>();
                    ArrayList<Boolean> isSpd = new ArrayList<>();

                    String[] properties = rawItem.split(",(?![^\\[]*\\])");
                    for(String prop : properties){
                        int colonIdx = prop.indexOf(":");
                        if(colonIdx == -1) continue;
                        String key = prop.substring(0, colonIdx).replace("\"", "").trim();
                        String val = prop.substring(colonIdx + 1).replace("\"", "").trim();
                        switch(key){
                            case "x": x = Integer.parseInt(val); break;
                            case "y": y = Integer.parseInt(val); break;
                            case "width": width = Integer.parseInt(val); break;
                            case "height": height = Integer.parseInt(val); break;
                            case "mapID": mapID = Integer.parseInt(val); break;
                            case "name": name = val; break;
                            case "type": type = val; break;
                            case "effects": effects = val; break;
                            case "reverseDirection": reverse = Boolean.parseBoolean(val); break;
                            case "colorRGB": color = new Color(Integer.parseInt(val)); break;
                            case "attackX":
                                if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) attX.add(Integer.parseInt(s.trim()));
                                break;
                            case "attackY":
                                if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) attY.add(Integer.parseInt(s.trim()));
                                break;
                            case "speed":
                                if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) spd.add(Double.parseDouble(s.trim()));
                                break;
                            case "isSpeed":
                                if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) isSpd.add(Boolean.parseBoolean(s.trim()));
                                break;
                        }
                    }
                    loadedItems.add(new Item(name, Item.ItemType.valueOf(type), color, x, y, width, height, mapID, effects, reverse, attX, attY, spd, isSpd));
                }
            }
        }
        if(gameCanvas.getMovement() != null){
            gameCanvas.getMovement().worldItems.clear();
            gameCanvas.getMovement().worldItems.addAll(loadedItems);
            gameCanvas.repaint();
        }
    }

    //export
    private void openExportDialog(){
        JDialog exportDialog = new JDialog(this, "Export Level", true);
        exportDialog.setLayout(new FlowLayout());
        JButton btnPlainText = new JButton("Plaintext");
        JButton btnFile = new JButton("Export as file");

        btnPlainText.addActionListener(e -> {
            exportDialog.dispose();
            String json = generateExportJson();

            JDialog textDialog = new JDialog(this, "Export Plaintext", true);
            textDialog.setLayout(new BorderLayout());
            textDialog.getRootPane().setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JTextArea textArea = new JTextArea(json, 20, 50);
            textArea.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(textArea);

            JButton btnCopy = new JButton("Copy to Clipboard");
            btnCopy.addActionListener(ce -> {
                java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(json);
                java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
                JOptionPane.showMessageDialog(textDialog, "Copied to clipboard", "Success", JOptionPane.INFORMATION_MESSAGE);
            });
            JPanel bottomPanel = new JPanel();
            bottomPanel.add(btnCopy);

            textDialog.add(scrollPane, BorderLayout.CENTER);
            textDialog.add(bottomPanel, BorderLayout.SOUTH);
            textDialog.pack();
            textDialog.setLocationRelativeTo(this);
            textDialog.setVisible(true);
        });

        btnFile.addActionListener(e -> {
            exportDialog.dispose();
            String json = generateExportJson();
            JFileChooser fileChooser = new JFileChooser();
            if(fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION){
                java.io.File file = fileChooser.getSelectedFile();
                try(java.io.FileWriter writer = new java.io.FileWriter(file)){
                    writer.write(json);
                    JOptionPane.showMessageDialog(this, "Level  exported successfully to " + file.getAbsolutePath());
                }catch (IOException ex){
                    JOptionPane.showMessageDialog(this, "Error saving file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        exportDialog.add(btnPlainText);
        exportDialog.add(btnFile);
        exportDialog.pack();
        exportDialog.setLocationRelativeTo(this);
        exportDialog.setVisible(true);
    }

    private String generateExportJson(){
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        boolean first = true;

        if(gameCanvas.getGameObjects() != null){
            for(GameObject obj : gameCanvas.getGameObjects()){
                if(obj.mapID == activeEditingMapID){
                    if(!first) sb.append(",\n");
                    first = false;
                    String jsonStr = String.format("  {\"kind\":\"object\", \"x\":%d, \"y\":%d, \"width\":%d, \"height\":%d, \"type\":\"%s\", \"isTriggered\":%b, " +
                        "\"triggeredX\":%d, \"triggeredY\":%d, \"triggeredDisplay\":%b, \"triggeredBy\":%d, \"activatesChannel\":%d, " +
                        "\"time\":%d, \"timeElapsed\":%d, \"move\":\"%s\", \"returnReverse\":%b, \"moveX\":%d, \"moveY\":%d, \"triggerMoveX\":%d, \"triggerMoveY\":%d, " +
                        "\"pathX\":%s, \"pathY\":%s, \"pathSpeed\":%s, \"pathSpeedIsSpeed\":%s, " +
                        "\"triggeredPathX\":%s, \"triggeredPathY\":%s, \"triggeredPathSpeed\":%s, \"triggeredPathSpeedIsSpeed\":%s}", 
                        obj.x, obj.y, obj.width, obj.height, obj.type.name(), obj.isTriggered, 
                        obj.triggeredX, obj.triggeredY, obj.triggeredDisplay, obj.triggeredBy, obj.activatesChannel,
                        obj.time, obj.timeElapsed, obj.move.name(), obj.returnReverse, obj.moveX, obj.moveY, obj.triggerMoveX, obj.triggerMoveY,
                        obj.pathX.toString(), obj.pathY.toString(), obj.pathSpeed.toString(), obj.pathSpeedIsSpeed.toString(),
                        obj.triggeredPathX.toString(), obj.triggeredPathY.toString(), obj.triggeredPathSpeed.toString(), obj.triggeredPathSpeedIsSpeed.toString()
                    );
                    sb.append(jsonStr);
                }
            }
        }

        if(gameCanvas.getMovement() != null && gameCanvas.getMovement().getEnemies() != null){
            for(Enemy e : gameCanvas.getMovement().getEnemies()){
                if(e.mapID == activeEditingMapID){
                    if(!first) sb.append(",\n");
                    first = false;
                    double initialSpeed = (e instanceof FlyingEnemy) ? ((FlyingEnemy) e).getXSpeed() : e.velX;
                    String jsonStr;
                    if(e instanceof FlyingEnemy){
                        FlyingEnemy fe = (FlyingEnemy) e;
                        jsonStr = String.format("  {\"kind\":\"enemy\", \"x\":%d, \"y\":%d, \"width\":%d, \"height\":%d, \"velX\":%.4f, \"xRange\":%.2f, \"yRange\":%.2f, \"maxHealth\":%.2f, \"regeneration\":%.2f, \"hasPathfinding\":%b, \"type\":\"%s\"}", 
                           e.startX, e.startY, e.width, e.height, initialSpeed, fe.getXRange(), fe.getYRange(), e.maxHealth, e.regeneration, e.hasPathfinding, e.type);
                    }else if(e instanceof JumpingEnemy){
                        JumpingEnemy je = (JumpingEnemy) e;
                        jsonStr = String.format("  {\"kind\":\"enemy\", \"x\":%d, \"y\":%d, \"width\":%d, \"height\":%d, \"velX\":%.4f, \"jumpHeight\":%.4f, \"intervalMax\":%d, \"intervalMin\":%d, \"maxHealth\":%.2f, \"regeneration\":%.2f, \"hasPathfinding\":%b, \"type\":\"%s\"}", 
                           e.startX, e.startY, e.width, e.height, initialSpeed, je.getJumpHeight(), je.getIntervalMax(), je.getIntervalMin(), e.maxHealth, e.regeneration, e.hasPathfinding, e.type);
                    }else{
                        jsonStr = String.format("  {\"kind\":\"enemy\", \"x\":%d, \"y\":%d, \"width\":%d, \"height\":%d, \"velX\":%.4f, \"maxHealth\":%.2f, \"regeneration\":%.2f, \"hasPathfinding\":%b, \"type\":\"%s\"}", 
                           e.startX, e.startY, e.width, e.height, initialSpeed, e.maxHealth, e.regeneration, e.hasPathfinding, e.type);
                    }
                    sb.append(jsonStr);
                }
            }
        }

        if(gameCanvas.getMovement() != null && gameCanvas.getMovement().worldItems != null){
            for(Item item : gameCanvas.getMovement().worldItems){
                if(item.mapID == activeEditingMapID){
                    if(!first) sb.append(",\n");
                    first = false;
                    String jsonStr = String.format("  {\"kind\":\"item\", \"name\":\"%s\", \"type\":\"%s\", \"colorRGB\":%d, \"x\":%d, \"y\":%d, \"width\":%d, \"height\":%d, \"effects\":\"%s\", \"reverseDirection\":\"%b\", \"attackX\":%s, \"attackY\":%s, \"speed\":%s, \"isSpeed\":%s}", 
                            item.name, item.type.name(), item.color.getRGB(), item.startX, item.startY, item.width, item.height, item.effects, item.reverseDirection,
                            item.attackX.toString(), item.attackY.toString(), item.speed.toString(), item.isSpeed.toString());
                    sb.append(jsonStr);
                }
            }
        }

        sb.append("\n]");
        return sb.toString();
    }

    //import
    private void openImportDialog(){
        JDialog importDialog = new JDialog(this, "Import Level", true);
        importDialog.setLayout(new FlowLayout());
        JButton btnPaste = new JButton("Paste PlainText");
        JButton btnFile = new JButton("Choose File");

        btnPaste.addActionListener(e -> {
            importDialog.dispose();
            JTextArea textArea = new JTextArea(15, 40);
            int result = JOptionPane.showConfirmDialog(this, new JScrollPane(textArea), "Paste Export Plaintext", JOptionPane.OK_CANCEL_OPTION);
            if(result == JOptionPane.OK_OPTION){
                processImportJson(textArea.getText());
            }
        });

        btnFile.addActionListener(e -> {
            importDialog.dispose();
            JFileChooser fileChooser = new JFileChooser();
            if(fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION){
                java.io.File file = fileChooser.getSelectedFile();
                try{
                    String content = new String(java.nio.file.Files.readAllBytes(file.toPath()), java.nio.charset.StandardCharsets.UTF_8);
                    processImportJson(content);
                }catch (IOException ex){
                    JOptionPane.showMessageDialog(this, "Error reading file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        importDialog.add(btnPaste);
        importDialog.add(btnFile);
        importDialog.pack();
        importDialog.setLocationRelativeTo(this);
        importDialog.setVisible(true);
    }
    
    private void processImportJson(String jsonStringData){
        if(jsonStringData == null || jsonStringData.trim().isEmpty()) return;
        jsonStringData.trim();
        if(jsonStringData.startsWith("[")){
            jsonStringData = jsonStringData.substring(1);
        }
        if(jsonStringData.endsWith("]")){
            jsonStringData = jsonStringData.substring(0, jsonStringData.length() - 1);
        }
        jsonStringData = jsonStringData.trim();
        if(jsonStringData.isEmpty()) return;

        int newMapID = getNextUnusedMapID();

        String[] rawItems = jsonStringData.split("\\},\\s*\\{");
        ArrayList<GameObject> importedObjects = new ArrayList<>();
        ArrayList<Enemy> importedEnemies = new ArrayList<>();
        ArrayList<Item> importedItems = new ArrayList<>();

        for(String rawItem : rawItems){
            rawItem = rawItem.replace("{", "").replace("}", "").trim();
            String kind = "object";
            String[] properties = rawItem.split(",(?![^\\[]*\\])");

            for(String prop : properties){
                String[] kv = prop.split(":", 2);
                if(kv.length == 2){
                    String key = kv[0].replace("\"", "").trim();
                    String val = kv[1].replace("\"", "").trim();
                    if(key.equals("kind")){
                        kind = val;
                        break;
                    }
                }
            }

            if(kind.equals("object")){
                int x = 0, y = 0, width = 0, height = 0;
                boolean isTriggered = false, triggeredDisplay = true;
                int triggeredX = -1, triggeredY = -1, triggeredBy = 0, activatesChannel = 0;
                int time = 0, timeElapsed = 0, moveX = 0, moveY = 0, triggerMoveX = 0, triggerMoveY = 0;
                GameObject.Type type = GameObject.Type.solid;
                GameObject.movement move = GameObject.movement.never;
                boolean returnReverse = false;

                ArrayList<Integer> pX = new ArrayList<>();
                ArrayList<Integer> pY = new ArrayList<>();
                ArrayList<Double> pSpd = new ArrayList<>();
                ArrayList<Boolean> pIsSpd = new ArrayList<>();

                ArrayList<Integer> tPX = new ArrayList<>();
                ArrayList<Integer> tPY = new ArrayList<>();
                ArrayList<Double> tPSpd = new ArrayList<>();
                ArrayList<Boolean> tPIsSpd = new ArrayList<>();

                for (String prop : properties) {
                    int colonIdx = prop.indexOf(":");
                    if (colonIdx == -1) continue;
                    String key = prop.substring(0, colonIdx).replace("\"", "").trim();
                    String val = prop.substring(colonIdx + 1).replace("\"", "").trim();
                    switch (key) {
                        case "x": x = Integer.parseInt(val); break;
                        case "y": y = Integer.parseInt(val); break;
                        case "width": width = Integer.parseInt(val); break;
                        case "height": height = Integer.parseInt(val); break;
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
                        case "returnReverse": returnReverse = Boolean.parseBoolean(val); break;
                        case "moveX": moveX = Integer.parseInt(val); break;
                        case "moveY": moveY = Integer.parseInt(val); break;
                        case "triggerMoveX": triggerMoveX = Integer.parseInt(val); break;
                        case "triggerMoveY": triggerMoveY = Integer.parseInt(val); break;
                        case "pathX":
                            if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) pX.add(Integer.parseInt(s.trim()));
                            break;
                        case "pathY":
                            if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) pY.add(Integer.parseInt(s.trim()));
                            break;
                        case "pathSpeed":
                            if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) pSpd.add(Double.parseDouble(s.trim()));
                            break;
                        case "pathSpeedIsSpeed":
                            if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) pIsSpd.add(Boolean.parseBoolean(s.trim()));
                            break;
                        case "triggeredPathX":
                            if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) tPX.add(Integer.parseInt(s.trim()));
                            break;
                        case "triggeredPathY":
                            if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) tPY.add(Integer.parseInt(s.trim()));
                            break;
                        case "triggeredPathSpeed":
                            if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) tPSpd.add(Double.parseDouble(s.trim()));
                            break;
                        case "triggeredSpeedIsSpeed":
                        case "triggeredPathSpeedIsSpeed":
                            if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) tPIsSpd.add(Boolean.parseBoolean(s.trim()));
                            break;
                    }
                }
                if (triggeredX == -1 && triggeredY == -1) { triggeredX = x; triggeredY = y; }
                if (moveX == 0 && moveY == 0) { moveX = x; moveY = y; }
                if (triggerMoveX == 0 && triggerMoveY == 0) { triggerMoveX = triggeredX; triggerMoveY = triggeredY; }

                importedObjects.add(new GameObject(x, y, width, height, type, newMapID, isTriggered, triggeredX, triggeredY, triggeredDisplay, triggeredBy, activatesChannel, time, timeElapsed, move, returnReverse, moveX, moveY, triggerMoveX, triggerMoveY, x, y, pX, pY, pSpd, pIsSpd, tPX, tPY, tPSpd, tPIsSpd));
            }else if(kind.equals("enemy")){
                int x = 0, y = 0, width = 36, height = 36;
                double velX = 0.1;
                double jumpHeight = -15.5;
                int intervalMax = 180, intervalMin = 60;
                double xRange = 64.0, yRange = 32.0;
                boolean hasPathfinding = false;
                String type = "PATROL";
                double maxHealth = 100.0, regeneration = 0.0;

                for (String prop : properties) {
                    int colonIdx = prop.indexOf(":");
                    if (colonIdx == -1) continue;
                    String key = prop.substring(0, colonIdx).replace("\"", "").trim();
                    String val = prop.substring(colonIdx + 1).replace("\"", "").trim();
                    switch (key) {
                        case "x": x = Integer.parseInt(val); break;
                        case "y": y = Integer.parseInt(val); break;
                        case "width": width = Integer.parseInt(val); break;
                        case "height": height = Integer.parseInt(val); break;
                        case "velX": velX = Double.parseDouble(val); break;
                        case "xRange": xRange = Double.parseDouble(val); break;
                        case "yRange": yRange = Double.parseDouble(val); break;
                        case "maxHealth": maxHealth = Double.parseDouble(val); break;
                        case "regeneration": regeneration = Double.parseDouble(val); break;
                        case "jumpHeight": jumpHeight = Double.parseDouble(val); break;
                        case "intervalMin": intervalMin = Integer.parseInt(val); break;
                        case "intervalMax": intervalMax = Integer.parseInt(val); break;
                        case "hasPathfinding": hasPathfinding = Boolean.parseBoolean(val); break;
                        case "type": type = val; break;
                    }
                }
                Enemy enemy = EnemyRegistry.createFromString(type, x, y, width, height, newMapID, velX);
                if (enemy != null) {
                    enemy.hasPathfinding = hasPathfinding;
                    enemy.maxHealth = maxHealth;
                    enemy.currentHealth = maxHealth;
                    enemy.regeneration = regeneration;
                    if (enemy instanceof FlyingEnemy) {
                        ((FlyingEnemy) enemy).setXRange(xRange);
                        ((FlyingEnemy) enemy).setYRange(yRange);
                    }
                    if (enemy instanceof JumpingEnemy) {
                        ((JumpingEnemy) enemy).setJumpHeight(jumpHeight);
                        ((JumpingEnemy) enemy).setIntervalMax(intervalMax);
                        ((JumpingEnemy) enemy).setIntervalMin(intervalMin);
                    }
                    importedEnemies.add(enemy);
                }
            }else if(kind.equals("item")){
                int x = 0, y = 0, width = 32, height = 32;
                String name = "COIN", type = "MISC", effects = "";
                Color color = Color.YELLOW;
                Boolean reverse = false;

                ArrayList<Integer> attX = new ArrayList<>();
                ArrayList<Integer> attY = new ArrayList<>();
                ArrayList<Double> spd = new ArrayList<>();
                ArrayList<Boolean> isSpd = new ArrayList<>();

                for(String prop : properties){
                    int colonIdx = prop.indexOf(":");
                    if(colonIdx == -1) continue;
                    String key = prop.substring(0, colonIdx).replace("\"", "").trim();
                    String val = prop.substring(colonIdx + 1).replace("\"", "").trim();
                    switch(key){
                        case "x": x = Integer.parseInt(val); break;
                        case "y": y = Integer.parseInt(val); break;
                        case "width": width = Integer.parseInt(val); break;
                        case "height": height = Integer.parseInt(val); break;
                        case "name": name = val; break;
                        case "type": type = val; break;
                        case "effects": effects = val; break;
                        case "reverseDirection": reverse = Boolean.parseBoolean(val); break;
                        case "colorRGB": color = new Color(Integer.parseInt(val)); break;
                        case "attackX":
                            if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) attX.add(Integer.parseInt(s.trim()));
                            break;
                        case "attackY":
                            if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) attY.add(Integer.parseInt(s.trim()));
                            break;
                        case "speed":
                            if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) spd.add(Double.parseDouble(s.trim()));
                            break;
                        case "isSpeed":
                            if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) isSpd.add(Boolean.parseBoolean(s.trim()));
                            break;
                    }
                }
                Item importedItem = new Item(name, Item.ItemType.valueOf(type), color, x, y, width, height, newMapID, effects, reverse, attX, attY, spd, isSpd);
                importedItems.add(importedItem);
            }
        }

        if(gameCanvas.getGameObjects() != null){
            gameCanvas.getGameObjects().addAll(importedObjects);
        }

        if(gameCanvas.getMovement().getEnemies() != null){
            gameCanvas.getMovement().getEnemies().addAll(importedEnemies);
        }

        if(gameCanvas.getMovement().worldItems != null){
            gameCanvas.getMovement().worldItems.addAll(importedItems);
        }

        activeEditingMapID = newMapID;
        currentlyLoadedMapID = newMapID;
        if(txtMapID != null){
            txtMapID.setText(String.valueOf(newMapID));
        }
        try{
            gameCanvas.loadLevel(activeEditingMapID);
        }catch (Exception ex){}
        gameCanvas.repaint();
        JOptionPane.showMessageDialog(this, "Level imported successfully into Map: " + newMapID);
    }

    private Item createItemFromTemplate(String templateName, Item.ItemType type, int x, int y, int mapID){
        for(ItemRegistry reg : ItemRegistry.values()){
            Item temp = reg.create(x, y, mapID);
            if(temp.name.equalsIgnoreCase(templateName) && temp.type == type){
                return temp;
            }
        }

        java.io.File file = new java.io.File("src/savedObjects.json");
        if(file.exists()){
            try(java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))){
                StringBuilder sb = new StringBuilder();
                String line;
                while((line = reader.readLine()) != null){
                    sb.append(line);
                }
                String content = sb.toString().trim();
                if(content.startsWith("[") && content.endsWith("]")){
                    content = content.substring(1, content.length() - 1).trim();
                }
                if(!content.isEmpty()){
                    String[] parts = content.split("\\},\\s*\\{");
                    for(String p : parts){
                        String jsonBlock = "{" + p.replace("{", "").replace("}", "") + "}";
                        if(jsonBlock.contains("\"kind\":\"item\"") && jsonBlock.contains("\"name\":\"" + templateName + "\"") && jsonBlock.contains("\"type\":\"" + type.name() + "\"")){
                            // Parse properties
                            int width = 32, height = 32;
                            String effects = "";
                            Color color = Color.YELLOW;
                            boolean reverse = false;
                            ArrayList<Integer> relX = new ArrayList<>();
                            ArrayList<Integer> relY = new ArrayList<>();
                            ArrayList<Double> spd = new ArrayList<>();
                            ArrayList<Boolean> isSpd = new ArrayList<>();

                            String[] props = jsonBlock.replace("{","").replace("}","").split(",(?![^\\[]*\\])(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                            for(String prop : props){
                                int colon = prop.indexOf(":");
                                if(colon == -1) continue;
                                String k = prop.substring(0, colon).replace("\"", "").trim();
                                String v = prop.substring(colon + 1).replace("\"", "").trim();
                                switch(k){
                                    case "width": width = Integer.parseInt(v); break;
                                    case "height": height = Integer.parseInt(v); break;
                                    case "effects": effects = v; break;
                                    case "colorRGB": color = new Color(Integer.parseInt(v)); break;
                                    case "reverseDirection": reverse = Boolean.parseBoolean(v); break;
                                    case "attackX":
                                        if(!v.isEmpty() && !v.equals("[]")) for(String s : v.replace("[","").replace("]","").split(",")) if(!s.trim().isEmpty()) relX.add(Integer.parseInt(s.trim()));
                                        break;
                                    case "attackY":
                                        if(!v.isEmpty() && !v.equals("[]")) for(String s : v.replace("[","").replace("]","").split(",")) if(!s.trim().isEmpty()) relY.add(Integer.parseInt(s.trim()));
                                        break;
                                    case "speed":
                                        if(!v.isEmpty() && !v.equals("[]")) for(String s : v.replace("[","").replace("]","").split(",")) if(!s.trim().isEmpty()) spd.add(Double.parseDouble(s.trim()));
                                        break;
                                    case "isSpeed":
                                        if(!v.isEmpty() && !v.equals("[]")) for(String s : v.replace("[","").replace("]","").split(",")) if(!s.trim().isEmpty()) isSpd.add(Boolean.parseBoolean(s.trim()));
                                        break;
                                }
                            }
                            ArrayList<Integer> absAttackX = new ArrayList<>();
                            ArrayList<Integer> absAttackY = new ArrayList<>();
                            for(int i = 0; i < relX.size(); i++){
                                absAttackX.add(x + relX.get(i));
                                absAttackY.add(y + relY.get(i));
                            }
                            Item created = new Item(templateName, type, color, x, y, width, height, mapID, effects, reverse, absAttackX, absAttackY, spd, isSpd);
                            // created.reverseDirection = reverse;
                            return created;
                        }
                    }
                }
            }catch(Exception ex){}
        }
        return new Item(templateName, type, Color.YELLOW, x, y, 32, 32, mapID, "", false, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    //clearing points
    public void clearSelectionPoints() {
        this.bottomLeftCorner = null;
        this.selectedObject = null;
        this.selectedEnemy = null;
        this.selectedItem = null;
        if (this.btnDelete != null) this.btnDelete.setEnabled(false);
        if (this.gameCanvas != null) this.gameCanvas.repaint();
    }
    
    public void restoreGameView(){
        if(windowLayoutSwitcher != null && mainContentCardContainer != null){
            btnPlay.setEnabled(true);
            btnExit.setEnabled(false);
            btnSave.setEnabled(true);
            btnLoad.setEnabled(true);
            btnFreePlace.setEnabled(true);
            btnNew.setEnabled(true);
            windowLayoutSwitcher.show(mainContentCardContainer, "GAME_CANVAS");
            gameCanvas.requestFocusInWindow();
        }
    }
    
    //editing objects
    private void showPropertyConfigDialog(GameObject obj){
        this.propertyEditingObject = obj;
        this.isEditingTriggeredPath = false;
        JDialog propDialog = new JDialog(this, "Configure Object Path & Properties", false);
        propDialog.setLayout(new BorderLayout());
        propDialog.getRootPane().setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JPanel formPanel = new JPanel(new GridLayout(0, 2, 10, 10));

        final boolean[] editingTriggeredPath = {false};

        JCheckBox chkIsTriggered = new JCheckBox("Edit trigggered Path", editingTriggeredPath[0]);
        formPanel.add(new JLabel("Path Mode:"));
        formPanel.add(chkIsTriggered);

        formPanel.add(new JLabel("Triggered By Channel ID:"));
        JTextField txtTriggeredBy = new JTextField(String.valueOf(obj.triggeredBy));
        formPanel.add(txtTriggeredBy);

        formPanel.add(new JLabel("Activates Channel ID:"));
        JTextField txtActivatesChannel = new JTextField(String.valueOf(obj.activatesChannel));
        txtActivatesChannel.setEnabled(obj.type == GameObject.Type.trigger);
        formPanel.add(txtActivatesChannel);

        formPanel.add(new JLabel("Triggered Start X:"));
        JTextField txtTriggeredX = new JTextField(String.valueOf(obj.triggeredX));
        formPanel.add(txtTriggeredX);

        formPanel.add(new JLabel("Triggered Start Y:"));
        JTextField txtTriggeredY = new JTextField(String.valueOf(obj.triggeredY));
        formPanel.add(txtTriggeredY);

        dialogSegmentDropdown = new JComboBox<>();
        formPanel.add(new JLabel("Target Segment:"));
        formPanel.add(dialogSegmentDropdown);

        dialogTxtX = new JTextField();
        dialogTxtY = new JTextField();
        dialogTxtSpeedVal = new JTextField();
        dialogCmbSpeedorTime = new JComboBox<>(new String[]{"Speed (pixels/frame)", "Time (frames)"});
    
        formPanel.add(new JLabel("Waypoint X;"));
        formPanel.add(dialogTxtX);
        formPanel.add(new JLabel("Waypoint Y:"));
        formPanel.add(dialogTxtY);
        formPanel.add(new JLabel("Speed / Time Value:"));
        formPanel.add(dialogTxtSpeedVal);
        formPanel.add(new JLabel("Value Type:"));
        formPanel.add(dialogCmbSpeedorTime);

        formPanel.add(new JLabel("Movement Activation Condition:"));
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
        formPanel.add(btnCycleMoveCondition);

        formPanel.add(new JLabel("Waypoint Return Style:"));
        JToggleButton btnReturnStyle = new JToggleButton(obj.returnReverse ? "Reverse Through Waypoints" : "Direct Return to Start", obj.returnReverse);
        btnReturnStyle.addActionListener(e -> {
            obj.returnReverse = btnReturnStyle.isSelected();
            btnReturnStyle.setText(obj.returnReverse ? "Reverse Through Waypoints" : "Direct Return to Start");
        });
        formPanel.add(btnReturnStyle);

        JButton btnAddPoint = new JButton("Add new Waypoint");
        JButton btnRemovePoint = new JButton("Remove current Waypoint");
        formPanel.add(btnAddPoint);
        formPanel.add(btnRemovePoint);

        Runnable[] refreshDropdowns = new Runnable[1];
        refreshDropdowns[0] = () -> {
            dialogSegmentDropdown.removeAllItems();
            boolean triggered = editingTriggeredPath[0];
            ArrayList<Integer> px = triggered ? obj.triggeredPathX : obj.pathX;

            for(int i = 0; i < px.size(); i++){
                int fromIdx = i;
                dialogSegmentDropdown.addItem("Segment " + (fromIdx + 1) + " (Point " + (i + 1) + ")");
            }
            if(px.isEmpty()){
                dialogSegmentDropdown.addItem("No Waypoints (Click Canvas to Add)");
            }
        };
        Runnable loadSelectedSegmentData = () -> {
            boolean triggered = editingTriggeredPath[0];
            ArrayList<Integer> px = triggered ? obj.triggeredPathX : obj.pathX;
            ArrayList<Integer> py = triggered ? obj.triggeredPathY : obj.pathY;
            ArrayList<Double> pSpd = triggered ? obj.triggeredPathSpeed : obj.pathSpeed;
            ArrayList<Boolean> pIsSpd = triggered ? obj.triggeredPathSpeedIsSpeed : obj.pathSpeedIsSpeed;

            int idx = dialogSegmentDropdown.getSelectedIndex();
            if(idx >= 0 && idx < px.size()){
                dialogTxtX.setText(String.valueOf(px.get(idx)));
                dialogTxtY.setText(String.valueOf(py.get(idx)));
                dialogTxtSpeedVal.setText(String.valueOf(idx < pSpd.size() ? pSpd.get(idx) : 2.0));
                boolean isSpeed = idx < pIsSpd.size() ? pIsSpd.get(idx) : true;
                dialogCmbSpeedorTime.setSelectedIndex(isSpeed ? 0 : 1);
                dialogTxtX.setEnabled(true);
                dialogTxtY.setEnabled(true);
                dialogTxtSpeedVal.setEnabled(true);
                dialogCmbSpeedorTime.setEnabled(true);
            }else{
                dialogTxtX.setText("");
                dialogTxtY.setText("");
                dialogTxtSpeedVal.setText("");
                dialogTxtY.setEnabled(false);
                dialogTxtY.setEnabled(false);
                dialogTxtSpeedVal.setEnabled(false);
                dialogCmbSpeedorTime.setEnabled(false);
            }
        };

        chkIsTriggered.addActionListener(e -> {
            editingTriggeredPath[0] = chkIsTriggered.isSelected();
            isEditingTriggeredPath = editingTriggeredPath[0];
            refreshDropdowns[0].run();
            loadSelectedSegmentData.run();
            gameCanvas.repaint();
        });

        dialogSegmentDropdown.addActionListener(e -> {
            if(dialogSegmentDropdown.getItemCount() > 0 && dialogSegmentDropdown.getSelectedIndex() != -1){
                selectedWaypointIndex = dialogSegmentDropdown.getSelectedIndex();
                loadSelectedSegmentData.run();
                gameCanvas.repaint();
            }
        });

        formPanel.add(new JLabel("Visible / Tangible When Triggered:"));
        JToggleButton btnToggleDisplay = new JToggleButton(obj.triggeredDisplay ? "TRUE (Stay Visible)" : "FALSE (Disappear / Intangible)", obj.triggeredDisplay);
        btnToggleDisplay.addActionListener(e -> {
            obj.triggeredDisplay = btnToggleDisplay.isSelected();
            btnToggleDisplay.setText(obj.triggeredDisplay ? "TRUE (Stay Visible)": "FALSE (Disappear / Intangible)");
        });
        formPanel.add(btnToggleDisplay);

        btnAddPoint.addActionListener(e -> {
            boolean triggered = editingTriggeredPath[0];
            ArrayList<Integer> px = triggered ? obj.triggeredPathX : obj.pathX;
            ArrayList<Integer> py = triggered ? obj.triggeredPathY : obj.pathY;
            ArrayList<Double> pSpd = triggered ? obj.triggeredPathSpeed : obj.pathSpeed;
            ArrayList<Boolean> pIsSpd = triggered ? obj.triggeredPathSpeedIsSpeed : obj.pathSpeedIsSpeed;

            int defaultX = px.isEmpty() ? obj.x + 100 : px.get(px.size() - 1) + 50;
            int defaultY = px.isEmpty() ? obj.y : px.get(px.size() - 1);

            px.add(defaultX);
            py.add(defaultY);
            pSpd.add(2.0);
            pIsSpd.add(true);

            refreshDropdowns[0].run();
            dialogSegmentDropdown.setSelectedIndex(px.size() - 1);
            selectedWaypointIndex = px.size() - 1;
            gameCanvas.repaint();
        });

        btnRemovePoint.addActionListener(e -> {
            boolean triggered = editingTriggeredPath[0];
            ArrayList<Integer> px = triggered ? obj.triggeredPathX : obj.pathX;
            ArrayList<Integer> py = triggered ? obj.triggeredPathY : obj.pathY;
            ArrayList<Double> pSpd = triggered ? obj.triggeredPathSpeed : obj.pathSpeed;
            ArrayList<Boolean> pIsSpd = triggered ? obj.triggeredPathSpeedIsSpeed : obj.pathSpeedIsSpeed;

            int idx = dialogSegmentDropdown.getSelectedIndex();
            if(idx >= 0 && idx < px.size()){
                px.remove(idx);
                py.remove(idx);
                if(idx < pSpd.size()) pSpd.remove(idx);
                if(idx < pIsSpd.size()) pIsSpd.remove(idx);

                refreshDropdowns[0].run();
                if(dialogSegmentDropdown.getItemCount() > 0) dialogSegmentDropdown.setSelectedIndex(Math.max(0, idx - 1));
                gameCanvas.repaint();
            }
        });

        refreshDropdowns[0].run();
        if(dialogSegmentDropdown.getItemCount() > 0) dialogSegmentDropdown.setSelectedIndex(0);

        JButton btnConfirm = new JButton("Confirm & Save Changes");
        btnConfirm.addActionListener(e -> {
            try{
                saveStateForUndo();
                obj.triggeredBy = Integer.parseInt(txtTriggeredBy.getText().trim());
                if (obj.type == GameObject.Type.trigger) {
                    obj.activatesChannel = Integer.parseInt(txtActivatesChannel.getText().trim());
                }
                obj.triggeredX = Integer.parseInt(txtTriggeredX.getText().trim());
                obj.triggeredY = Integer.parseInt(txtTriggeredY.getText().trim());

                int idx = dialogSegmentDropdown.getSelectedIndex();
                boolean triggered = editingTriggeredPath[0];
                // obj.isTriggered = triggered;

                ArrayList<Integer> px = triggered ? obj.triggeredPathX : obj.pathX;
                ArrayList<Integer> py = triggered ? obj.triggeredPathY : obj.pathY;
                ArrayList<Double> pSpd = triggered ? obj.triggeredPathSpeed : obj.pathSpeed;
                ArrayList<Boolean> pIsSpd = triggered ? obj.triggeredPathSpeedIsSpeed : obj.pathSpeedIsSpeed;

                if(idx >= 0 && idx < px.size() && !dialogTxtX.getText().isEmpty()){
                    px.set(idx, Integer.parseInt(dialogTxtX.getText().trim()));
                    py.set(idx, Integer.parseInt(dialogTxtY.getText().trim()));

                    while(pSpd.size() <= idx) pSpd.add(2.0);
                    while(pIsSpd.size() <= idx) pIsSpd.add(true);

                    pSpd.set(idx, Double.parseDouble(dialogTxtSpeedVal.getText().trim()));
                    pIsSpd.set(idx, dialogCmbSpeedorTime.getSelectedIndex() == 0);
                }
                selectedWaypointIndex = -1;
                propDialog.dispose();
                selectedObject = null;
                this.propertyEditingObject = null;
                gameCanvas.repaint();
            }catch (NumberFormatException ex){
                JOptionPane.showMessageDialog(propDialog, "Please enter valid numerical values.");
            }
        });

        JPanel bottomPanel = new JPanel();
        bottomPanel.add(btnConfirm);
        propDialog.add(formPanel, BorderLayout.CENTER);
        propDialog.add(bottomPanel, BorderLayout.SOUTH);
        propDialog.pack();
        propDialog.setLocationRelativeTo(this);
        propDialog.setVisible(true);
    }

    //editing enemies
    private void showEnemyConfigDialog(Enemy enemy){
        this.propertyEditingEnemy = enemy;
        
        enemyProperty = new JDialog(this, "Configure Enemy Properties", false);
        enemyProperty.setLayout(new BorderLayout());
        enemyProperty.getRootPane().setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel formPanel = new JPanel(new GridLayout(7, 2, 10, 10));

        formPanel.add(new JLabel("Speed"));
        double initialSpeed = enemy.velX;
        if(enemy instanceof FlyingEnemy){
            initialSpeed = ((FlyingEnemy) enemy).getXSpeed();
        }
        txtSpeed = new JTextField(String.valueOf(initialSpeed));
        formPanel.add(txtSpeed);

        formPanel.add(new JLabel("Size (width): "));
        txtWidth = new JTextField(String.valueOf(enemy.width));
        formPanel.add(txtWidth);

        formPanel.add(new JLabel("Size (height)"));
        txtHeight = new JTextField(String.valueOf(enemy.height));
        formPanel.add(txtHeight);

        formPanel.add(new JLabel("Max Health:"));
        JTextField txtMaxHealth = new JTextField(String.valueOf(enemy.maxHealth));
        formPanel.add(txtMaxHealth);

        formPanel.add(new JLabel("Regeneration:"));
        JTextField txtRegen = new JTextField(String.valueOf(enemy.regeneration));
        formPanel.add(txtRegen);

        formPanel.add(new JLabel("Enable Pathfinding:"));
        JCheckBox chkPathfinding = new JCheckBox("", enemy.hasPathfinding);
        formPanel.add(chkPathfinding);

        String[] choices = EnemyRegistry.getDisplayChoices();
        enemyTypeDropdown = new JComboBox<>(choices);

        for(EnemyRegistry reg : EnemyRegistry.values()){
            if(reg.name().equalsIgnoreCase(enemy.type)){
                enemyTypeDropdown.setSelectedItem(reg.getDisplayName());
                break;
            }
        }
        formPanel.add(new JLabel("Enemy Type"));
        formPanel.add(enemyTypeDropdown);

        JTextField txtDamage = new JTextField(propertyEditingEnemy != null ? String.valueOf(propertyEditingEnemy.damage) : "10", 10);
        formPanel.add(new JLabel("Damage:"));
        formPanel.add(txtDamage);

        JPanel flyingFieldsPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        flyingFieldsPanel.setBorder(BorderFactory.createTitledBorder("Flying Properties"));

        flyingFieldsPanel.add(new JLabel("X Range:"));
        JTextField txtXRange = new JTextField(enemy instanceof FlyingEnemy ? String.valueOf(((FlyingEnemy)enemy).getXRange()) : "64.0");
        flyingFieldsPanel.add(txtXRange);

        flyingFieldsPanel.add(new JLabel("Y Range:"));
        JTextField txtYRange = new JTextField(enemy instanceof FlyingEnemy ? String.valueOf(((FlyingEnemy)enemy).getYRange()) : "32.0");
        flyingFieldsPanel.add(txtYRange);

        JPanel conditionalPanelFlying = new JPanel(new BorderLayout());
        conditionalPanelFlying.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        conditionalPanelFlying.add(flyingFieldsPanel, BorderLayout.CENTER);

        conditionalPanelFlying.setVisible(enemy instanceof FlyingEnemy);

        enemyTypeDropdown.addActionListener(e -> {
            String selectedDisplayName = (String) enemyTypeDropdown.getSelectedItem();
            EnemyRegistry reg = EnemyRegistry.fromDisplayName(selectedDisplayName);
            boolean isFlying = (reg == EnemyRegistry.FLYING);
            conditionalPanelFlying.setVisible(isFlying);
            enemyProperty.pack();
        });
        
        JPanel jumpingFieldsPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        jumpingFieldsPanel.setBorder(BorderFactory.createTitledBorder("Jumping Properties"));

        jumpingFieldsPanel.add(new JLabel("Jump height:"));
        JTextField txtJumpHeight = new JTextField(enemy instanceof JumpingEnemy ? String.valueOf(((JumpingEnemy)enemy).getJumpHeight()) : "-15.5");
        jumpingFieldsPanel.add(txtJumpHeight);

        jumpingFieldsPanel.add(new JLabel("Min Jump Interval"));
        JTextField txtIntervalMin = new JTextField(enemy instanceof JumpingEnemy ? String.valueOf(((JumpingEnemy)enemy).getIntervalMin()) : "60");
        jumpingFieldsPanel.add(txtIntervalMin);

        jumpingFieldsPanel.add(new JLabel("Max Jump Interval"));
        JTextField txtIntervalMax = new JTextField(enemy instanceof JumpingEnemy ? String.valueOf(((JumpingEnemy)enemy).getIntervalMax()) : "180");
        jumpingFieldsPanel.add(txtIntervalMax);

        JPanel conditionalPanelJumping = new JPanel(new BorderLayout());
        conditionalPanelJumping.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        conditionalPanelJumping.add(jumpingFieldsPanel, BorderLayout.CENTER);

        conditionalPanelJumping.setVisible(enemy instanceof JumpingEnemy);

        enemyTypeDropdown.addActionListener(e -> {
            String selectedDisplayName = (String) enemyTypeDropdown.getSelectedItem();
            EnemyRegistry reg = EnemyRegistry.fromDisplayName(selectedDisplayName);
            boolean jumpingEnemy = (reg == EnemyRegistry.JUMPING);
            conditionalPanelJumping.setVisible(jumpingEnemy);
            enemyProperty.pack();
        });

        JPanel contentWrapper = new JPanel();
        contentWrapper.setLayout(new BoxLayout(contentWrapper, BoxLayout.Y_AXIS));
        contentWrapper.add(formPanel);
        contentWrapper.add(conditionalPanelFlying);
        contentWrapper.add(conditionalPanelJumping);

        JButton btnSaveEnemyTemplate = new JButton("Save as Template");
        btnSaveEnemyTemplate.addActionListener(e ->{
            String tName = JOptionPane.showInputDialog(enemyProperty, "Enter Enemy Template Name:");
            if(tName != null && !tName.trim().isEmpty()){
                saveEnemyTemplateManual(tName.trim(), enemy, "src/savedObjects.json");
                if(activeEnemyBrush != null && activeEnemyBrush.name().equalsIgnoreCase(enemy.type)){
                    cmbEnemyTemplate.addItem(tName.trim());
                }
                JOptionPane.showMessageDialog(enemyProperty, "Enemy template saved!");
            }
        });
        JPanel bottomPanel = new JPanel();
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        bottomPanel.add(btnSaveEnemyTemplate);

        JButton confirm = new JButton("Confirm");
        confirm.addActionListener(e -> {
            double newDamage = (propertyEditingEnemy != null) ? propertyEditingEnemy.damage : 10;
            try{
                saveStateForUndo();
                double newVelX = Double.parseDouble(txtSpeed.getText().trim());
                int newWidth = Integer.parseInt(txtWidth.getText().trim());
                int newHeight = Integer.parseInt(txtHeight.getText().trim());
                double newMaxHealth = Double.parseDouble(txtMaxHealth.getText().trim());
                double newRegen = Double.parseDouble(txtRegen.getText().trim());
                boolean newPathfinding = chkPathfinding.isSelected();
                newDamage = Double.parseDouble(txtDamage.getText().trim());

                // propertyEditingEnemy.velX = newVelX;
                // propertyEditingEnemy.width = newWidth;
                // propertyEditingEnemy.height = newHeight;
                // propertyEditingEnemy.maxHealth = newMaxHealth;
                // propertyEditingEnemy.regeneration = newRegen;
                // propertyEditingEnemy.hasPathfinding = newPathfinding;

                // if(propertyEditingEnemy instanceof FlyingEnemy && conditionalPanelFlying.isVisible()){
                //     try{
                //         ((FlyingEnemy)propertyEditingEnemy).setXRange(Double.parseDouble(txtXRange.getText().trim()));
                //         ((FlyingEnemy)propertyEditingEnemy).setYRange(Double.parseDouble(txtYRange.getText().trim()));
                //     }catch (NumberFormatException ex){
                //     }
                // }
                // if(propertyEditingEnemy instanceof JumpingEnemy && conditionalPanelJumping.isVisible()){
                //     try{
                //         ((JumpingEnemy)propertyEditingEnemy).setJumpHeight(Double.parseDouble(txtJumpHeight.getText().trim()));
                //         ((JumpingEnemy)propertyEditingEnemy).setIntervalMax(Integer.parseInt(txtIntervalMax.getText().trim()));
                //         ((JumpingEnemy)propertyEditingEnemy).setIntervalMin(Integer.parseInt(txtIntervalMin.getText().trim()));
                //     }catch(NumberFormatException ex){}
                // }

                String selectedDisplayName = (String) enemyTypeDropdown.getSelectedItem();
                EnemyRegistry selectedRegistry = EnemyRegistry.fromDisplayName(selectedDisplayName);

                ArrayList<Enemy> enemiesList = gameCanvas.getMovement().getEnemies();
                int index = enemiesList.indexOf(propertyEditingEnemy);
                if(selectedRegistry != null && index != -1){
                    Enemy updatedEnemy = selectedRegistry.create(
                        propertyEditingEnemy.x,
                        propertyEditingEnemy.y,
                        newWidth,
                        newHeight,
                        propertyEditingEnemy.mapID,
                        newVelX
                    );
                    if(updatedEnemy != null){
                        updatedEnemy.hasPathfinding = newPathfinding;
                        updatedEnemy.maxHealth = newMaxHealth;
                        updatedEnemy.currentHealth = newMaxHealth;
                        updatedEnemy.regeneration = newRegen;
                        updatedEnemy.damage = newDamage;

                        if(updatedEnemy instanceof FlyingEnemy && conditionalPanelFlying.isVisible()){
                            try{
                                ((FlyingEnemy)updatedEnemy).setXRange(Double.parseDouble(txtXRange.getText().trim()));
                                ((FlyingEnemy)updatedEnemy).setYRange(Double.parseDouble(txtYRange.getText().trim()));
                            }catch (NumberFormatException ex){}
                        }
                        if(updatedEnemy instanceof JumpingEnemy && conditionalPanelJumping.isVisible()){
                            try{
                                ((JumpingEnemy)updatedEnemy).setJumpHeight(Double.parseDouble(txtJumpHeight.getText().trim()));
                                ((JumpingEnemy)updatedEnemy).setIntervalMax(Integer.parseInt(txtIntervalMax.getText().trim()));
                                ((JumpingEnemy)updatedEnemy).setIntervalMin(Integer.parseInt(txtIntervalMin.getText().trim()));
                            }catch(NumberFormatException ex){}
                        }
                        enemiesList.set(index, updatedEnemy);
                        selectedEnemy = updatedEnemy;
                    }
                }else if(propertyEditingEnemy != null){
                    propertyEditingEnemy.velX = newVelX;
                    propertyEditingEnemy.width = newWidth;
                    propertyEditingEnemy.height = newHeight;
                    propertyEditingEnemy.hasPathfinding = newPathfinding;
                    propertyEditingEnemy.maxHealth = newMaxHealth;
                    propertyEditingEnemy.currentHealth = newMaxHealth;
                    propertyEditingEnemy.regeneration = newRegen;
                    propertyEditingEnemy.damage = newDamage;
                    if(propertyEditingEnemy instanceof FlyingEnemy && conditionalPanelFlying.isVisible()){
                        try{
                            ((FlyingEnemy)propertyEditingEnemy).setXRange(Double.parseDouble(txtXRange.getText().trim()));
                            ((FlyingEnemy)propertyEditingEnemy).setYRange(Double.parseDouble(txtYRange.getText().trim()));
                        }catch (NumberFormatException ex){
                        }
                    }
                    if(propertyEditingEnemy instanceof JumpingEnemy && conditionalPanelJumping.isVisible()){
                        try{
                            ((JumpingEnemy)propertyEditingEnemy).setJumpHeight(Double.parseDouble(txtJumpHeight.getText().trim()));
                            ((JumpingEnemy)propertyEditingEnemy).setIntervalMax(Integer.parseInt(txtIntervalMax.getText().trim()));
                            ((JumpingEnemy)propertyEditingEnemy).setIntervalMin(Integer.parseInt(txtIntervalMin.getText().trim()));
                        }catch(NumberFormatException ex){}
                    }
                }

                selectedEnemy = null;
                this.propertyEditingEnemy = null;
                enemyProperty.dispose();
                btnDelete.setEnabled(false);
                btnEditProperties.setEnabled(false);
                gameCanvas.repaint();
            }catch (NumberFormatException ex){
                JOptionPane.showMessageDialog(enemyProperty, "Please verify all numerical text entries are clean integer values");
            }
        });
        
        bottomPanel.add(confirm);

        enemyProperty.add(contentWrapper, BorderLayout.CENTER);
        enemyProperty.add(bottomPanel, BorderLayout.SOUTH);

        enemyProperty.pack();
        enemyProperty.setLocationRelativeTo(this);
        enemyProperty.setVisible(true);
    }
    
    //editing items
    private void showItemConfigDialog(Item item){
        this.propertyEditingItem = item;
        JDialog itemDialog = new JDialog(this, "Configure Item Properties", false);
        itemDialog.setLayout(new BorderLayout());
        itemDialog.getRootPane().setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel formPanel = new JPanel(new GridLayout(0, 2, 10, 10));

        formPanel.add(new JLabel("Item Name:"));
        JTextField txtName = new JTextField(item.name);
        formPanel.add(txtName);

        JTextField txtX = new JTextField(String.valueOf(item.x));
        formPanel.add(new JLabel("Position X:"));
        formPanel.add(txtX);

        formPanel.add(new JLabel("Position Y:"));
        JTextField txtY = new JTextField(String.valueOf(item.y));
        formPanel.add(txtY);

        formPanel.add(new JLabel("Width:"));
        JTextField txtWidth = new JTextField(String.valueOf(item.width));
        formPanel.add(txtWidth);

        formPanel.add(new JLabel("Height:"));
        JTextField txtHeight = new JTextField(String.valueOf(item.height));
        formPanel.add(txtHeight);

        formPanel.add(new JLabel("MapID:"));
        JTextField txtMapID = new JTextField(String.valueOf(item.mapID));
        formPanel.add(txtMapID);

        formPanel.add(new JLabel("Item Type:"));
        JComboBox<Item.ItemType> cmbType = new JComboBox<>(Item.ItemType.values());
        cmbType.setSelectedItem(item.type);
        formPanel.add(cmbType);

        formPanel.add(new JLabel("Color (RGB):"));
        JButton btnColor = new JButton("Choose Color");
        final Color[] chosenColor = {item.color};
        btnColor.setBackground(item.color);
        btnColor.addActionListener(ae -> {
            Color c = JColorChooser.showDialog(itemDialog, "Choose Item Color", chosenColor[0]);
            if(c != null){
                chosenColor[0] = c;
                btnColor.setBackground(c);
            }
        });
        formPanel.add(btnColor);

        JPanel effectsPanel = new JPanel(new BorderLayout(5, 5));
        effectsPanel.setBorder(BorderFactory.createTitledBorder("Item Effects Manager"));

        JPanel effectInputRow = new JPanel(new GridLayout(1, 4, 5, 0));
        JComboBox<String> cmbEffectType = new JComboBox<>(new String[]{"DAMAGE", "VALUE", "DURABILITY", "HEALING", "FIRE_DAMAGE", "COLD_DAMAGE", "LIGHTNING"});
        JTextField txtEffectVal = new JTextField(5);
        JButton btnAddOrUpdateEffect = new JButton("Set Effect");
        JButton btnRemoveEffect = new JButton("Remove Selected");

        effectInputRow.add(cmbEffectType);
        effectInputRow.add(txtEffectVal);
        effectInputRow.add(btnAddOrUpdateEffect);
        effectInputRow.add(btnRemoveEffect);

        java.util.Map<String, String> effectMap = new java.util.LinkedHashMap<>();
        if(item.effects != null && !item.effects.trim().isEmpty()){
            String[] pairs = item.effects.split(",");
            for(String p : pairs){
                String[] kv = p.split(":");
                if(kv.length == 2){
                    effectMap.put(kv[0].trim().toUpperCase(), kv[1].trim());
                }
            }
        }

        DefaultListModel<String> effectListModel = new DefaultListModel<>();
        Runnable refreshEffectList = () -> {
            effectListModel.clear();
            for(java.util.Map.Entry<String, String> entry : effectMap.entrySet()){
                effectListModel.addElement(entry.getKey() + " : " + entry.getValue());
            }
        };
        refreshEffectList.run();
        JList<String> lstEffects = new JList<>(effectListModel);
        JScrollPane effectScrollPane = new JScrollPane(lstEffects);
        effectScrollPane.setPreferredSize(new Dimension(200, 80));

        btnAddOrUpdateEffect.addActionListener(ae -> {
            String selectedType = (String) cmbEffectType.getSelectedItem();
            String val = txtEffectVal.getText().trim();
            if(!val.isEmpty()){
                effectMap.put(selectedType, val);
                refreshEffectList.run();
                txtEffectVal.setText("");
            }else{
                JOptionPane.showMessageDialog(itemDialog, "Please enter a value for the effect.");
            }
        });

        btnRemoveEffect.addActionListener(ae -> {
            String selectedItemStr = lstEffects.getSelectedValue();
            if(selectedItemStr != null){
                String key = selectedItemStr.split(":")[0].trim();
                effectMap.remove(key);
                refreshEffectList.run();
            }
        });

        lstEffects.addListSelectionListener(ae -> {
            if(!ae.getValueIsAdjusting()){
                String selectedItemStr = lstEffects.getSelectedValue();
                if(selectedItemStr != null){
                    String[] parts = selectedItemStr.split(":");
                    if(parts.length == 2){
                        cmbEffectType.setSelectedItem(parts[0].trim());
                        txtEffectVal.setText(parts[1].trim());
                    }
                }
            }
        });

        effectsPanel.add(effectInputRow, BorderLayout.NORTH);
        effectsPanel.add(effectScrollPane, BorderLayout.CENTER);

        JPanel waypointPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        waypointPanel.setBorder(BorderFactory.createTitledBorder("Weapon Attack Waypoints"));

        itemSegmentDropdown = new JComboBox<>();
        waypointPanel.add(new JLabel("Attack waypoint:"));
        waypointPanel.add(itemSegmentDropdown);

        itemTxtWpX = new JTextField();
        itemTxtWpY = new JTextField();
        itemTxtSpeedVal = new JTextField();
        itemCmbSpeedOrTime = new JComboBox<>(new String[]{"Speed (pixels/frame)", "Time (frames)"});

        waypointPanel.add(new JLabel("Waypoint X:"));
        waypointPanel.add(itemTxtWpX);
        waypointPanel.add(new JLabel("Waypoint Y:"));
        waypointPanel.add(itemTxtWpY);
        waypointPanel.add(new JLabel("Segment Speed/Time:"));
        waypointPanel.add(itemTxtSpeedVal);
        waypointPanel.add(new JLabel("Value Type:"));
        waypointPanel.add(itemCmbSpeedOrTime);

        JButton btnAddWp = new JButton("Add Attack Waypoint");
        JButton btnRemWp = new JButton("Remove Waypoint");
        waypointPanel.add(btnAddWp);
        waypointPanel.add(btnRemWp);

        Runnable updateWaypointVisibility = () -> {
            boolean isWeapon = (cmbType.getSelectedItem().equals(Item.ItemType.WEAPON));
            waypointPanel.setVisible(isWeapon);
            itemDialog.pack();
            gameCanvas.repaint();
        };

        cmbType.addActionListener(e -> updateWaypointVisibility.run());

        Runnable refreshWpDropdown = () -> {
            itemSegmentDropdown.removeAllItems();
            for(int i = 0; i < item.attackX.size(); i++){
                itemSegmentDropdown.addItem("Attack Point " + (i + 1));
            }
            if(item.attackX.isEmpty()){
                itemSegmentDropdown.addItem("No attack Waypoints");
            }
        };

        Runnable loadWpData = () -> {
            int idx = itemSegmentDropdown.getSelectedIndex();
            if(idx >= 0 && idx < item.attackX.size()){
                itemTxtWpX.setText(String.valueOf(item.attackX.get(idx)));
                itemTxtWpY.setText(String.valueOf(item.attackY.get(idx)));
                double spd = (idx < item.speed.size()) ? item.speed.get(idx) : 2.0;
                boolean isSpd = (idx < item.isSpeed.size()) ? item.isSpeed.get(idx) : true;
                itemTxtSpeedVal.setText(String.valueOf(spd));
                itemCmbSpeedOrTime.setSelectedIndex(isSpd ? 0 : 1);
            }
        };

        btnAddWp.addActionListener(e -> {
            int defaultX = item.attackX.isEmpty() ? item.x + 40 : item.attackX.get(item.attackX.size() - 1) + 30;
            int defaultY = item.attackY.isEmpty() ? item.y : item.attackY.get(item.attackY.size() - 1);
            item.attackX.add(defaultX);
            item.attackY.add(defaultY);
            item.speed.add(2.0);
            item.isSpeed.add(true);
            refreshWpDropdown.run();
            itemSegmentDropdown.setSelectedIndex(item.attackX.size() - 1);
            gameCanvas.repaint();
        });

        btnRemWp.addActionListener(e -> {
            int idx = itemSegmentDropdown.getSelectedIndex();
            if(idx >= 0 && idx < item.attackX.size()){
                item.attackX.remove(idx);
                item.attackY.remove(idx);
                if(idx < item.speed.size()) item.speed.remove(idx);
                if(idx < item.isSpeed.size()) item.isSpeed.remove(idx);
                refreshWpDropdown.run();
                if(itemSegmentDropdown.getItemCount() > 0) itemSegmentDropdown.setSelectedIndex(Math.max(0, idx - 1));
                gameCanvas.repaint();
            }
        });

        JToggleButton btnReverseDirection = new JToggleButton("Reverse Direction on Face: " + (item.reverseDirection ? "ON" : "OFF"), item.reverseDirection);
        btnReverseDirection.addActionListener(e -> {
            item.reverseDirection = btnReverseDirection.isSelected();
            btnReverseDirection.setText("Reverse Direction on Face: " + (item.reverseDirection ? "ON" : "OFF"));
        });
        waypointPanel.add(new JLabel("Attack Facing Mode:"));
        waypointPanel.add(btnReverseDirection);

        itemSegmentDropdown.addActionListener(e -> {
            if(itemSegmentDropdown.getItemCount() > 0 && itemSegmentDropdown.getSelectedIndex() != -1){
                selectedItemWaypointIndex = itemSegmentDropdown.getSelectedIndex();
                loadWpData.run();
                gameCanvas.repaint();
            }
        });
        updateWaypointVisibility.run();
        refreshWpDropdown.run();
        if(itemSegmentDropdown.getItemCount() > 0) itemSegmentDropdown.setSelectedIndex(0);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(formPanel, BorderLayout.NORTH);
        centerPanel.add(effectsPanel, BorderLayout.CENTER);
        centerPanel.add(waypointPanel, BorderLayout.SOUTH);

        JButton btnSaveTemplate = new JButton("Save as Template");
        btnSaveTemplate.addActionListener(e -> {
            try{
                item.name = txtName.getText().trim();
                item.width = Integer.parseInt(txtWidth.getText().trim());
                item.height = Integer.parseInt(txtHeight.getText().trim());
                item.type = (Item.ItemType) cmbType.getSelectedItem();
                item.color = chosenColor[0];

                saveItemTemplateManual(item, "src/savedObjects.json");
                JOptionPane.showMessageDialog(itemDialog, "Item template saved success");
            }catch (Exception ex){
                JOptionPane.showMessageDialog(itemDialog, "Error saving template: " + ex.getMessage());
            }
        });

        JButton btnRemoveTemplate = new JButton("Remove Template");
        btnRemoveTemplate.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(itemDialog, "Are you sure you want to delete template '" + item.name + "'?", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
            if(confirm == JOptionPane.YES_OPTION){
                removeItemTemplateManual(item.name, "src/savedObjects.json");
                JOptionPane.showMessageDialog(itemDialog, "Template removed successfully.");
                itemDialog.dispose();
            }
        });

        JButton btnConfirm = new JButton("Confirm");
        btnConfirm.addActionListener(e -> {
            saveStateForUndo();
            try{
                item.name = txtName.getText().trim();
                item.x = Integer.parseInt(txtX.getText().trim());
                item.y = Integer.parseInt(txtY.getText().trim());
                item.startX = item.x;
                item.startY = item.y;
                item.width = Integer.parseInt(txtWidth.getText().trim());
                item.height = Integer.parseInt(txtHeight.getText().trim());
                item.mapID = Integer.parseInt(txtMapID.getText().trim());
                item.type = (Item.ItemType) cmbType.getSelectedItem();
                item.color = chosenColor[0];

                StringBuilder sb = new StringBuilder();
                int idx = 0;
                for(java.util.Map.Entry<String, String> entry : effectMap.entrySet()){
                    if(idx > 0) sb.append(", ");
                    sb.append(entry.getKey()).append(":").append(entry.getValue());
                    idx++;
                }
                item.effects = sb.toString();

                int wpIdx = itemSegmentDropdown.getSelectedIndex();
                if(wpIdx >= 0 && wpIdx < item.attackX.size() && !itemTxtWpX.getText().isEmpty()){
                    item.attackX.set(wpIdx, Integer.parseInt(itemTxtWpX.getText().trim()));
                    item.attackY.set(wpIdx, Integer.parseInt(itemTxtWpY.getText().trim()));
                    while(item.speed.size() <= wpIdx) item.speed.add(2.0);
                    while(item.isSpeed.size() <= wpIdx) item.isSpeed.add(true);
                    item.speed.set(wpIdx, Double.parseDouble(itemTxtSpeedVal.getText().trim()));
                    item.isSpeed.set(wpIdx, itemCmbSpeedOrTime.getSelectedIndex() == 0);
                }

                propertyEditingItem = null;
                selectedItem = null;
                selectedItemWaypointIndex = -1;
                itemDialog.dispose();
                gameCanvas.repaint();
            }catch (NumberFormatException ex){
                JOptionPane.showMessageDialog(itemDialog, "Please enter valid numbers.");

            }
        });

        JPanel bottomPanel = new JPanel();
        bottomPanel.add(btnSaveTemplate);
        bottomPanel.add(btnRemoveTemplate);
        bottomPanel.add(btnConfirm);

        itemDialog.add(centerPanel, BorderLayout.CENTER);
        itemDialog.add(bottomPanel, BorderLayout.SOUTH);
        itemDialog.pack();
        itemDialog.setLocationRelativeTo(this);
        itemDialog.setVisible(true);
    }

    private void removeItemTemplateManual(String templateName, String filePath){
        java.io.File file = new java.io.File(filePath);
        if(!file.exists()) return;
        ArrayList<String> remainingTemplates = new ArrayList<>();
        try(java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))){
            StringBuilder sb = new StringBuilder();
            String line;
            while((line = reader.readLine()) != null) sb.append(line);
            String content = sb.toString().trim();
            if(content.startsWith("[") && content.endsWith("]")){
                content = content.substring(1, content.length() - 1).trim();
            }
            if(!content.isEmpty()){
                String[] parts = content.split("\\},\\s*\\{");
                for(String p : parts){
                    String block = "{" + p.replace("{", "").replace("}", "") + "}";
                    if(!block.contains("\"name\":\"" + templateName + "\"")){
                        remainingTemplates.add(block);
                    }
                }
            }
        }catch(Exception e){}

        try(java.io.FileWriter writer = new java.io.FileWriter(filePath)){
            writer.write("[\n");
            for(int i = 0; i < remainingTemplates.size(); i++){
                writer.write(" " + remainingTemplates.get(i));
                if(i < remainingTemplates.size() - 1) writer.write(",\n");
                else writer.write("\n");
            }
            writer.write("]");
        }catch(Exception e){}
    }
    
    private void saveItemTemplateManual(Item item, String filePath){
        java.io.File file = new java.io.File(filePath);
        ArrayList<String> existingTemplates = new ArrayList<>();

        if(file.exists()){
            try(java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))){
                StringBuilder sb = new StringBuilder();
                String line;
                while((line = reader.readLine()) != null){
                    sb.append(line);
                }
                String content = sb.toString().trim();
                if(content.startsWith("[") && content.endsWith("]")){
                    content = content.substring(1, content.length() - 1).trim();
                }
                if(!content.isEmpty()){
                    String[] parts = content.split("\\},\\s*\\{");
                    for(String p : parts){
                        existingTemplates.add("{" + p.replace("{", "").replace("}", "") + "}");
                    }
                }
            }catch (Exception e){}
        }

        ArrayList<Integer> relX = new ArrayList<>();
        ArrayList<Integer> relY = new ArrayList<>();
        for(int i = 0; i < item.attackX.size(); i++){
            relX.add(item.attackX.get(i) - item.x);
            relY.add(item.attackY.get(i) - item.y);
        }

        String jsonStr = String.format("{\"kind\":\"item\", \"name\":\"%s\", \"type\":\"%s\", \"colorRGB\":%d, \"width\":%d, \"height\":%d, \"effects\":\"%s\", \"attackX\":%s, \"attackY\":%s, \"speed\":%s, \"isSpeed\":%s, \"reverseDirection\":%b}",
            item.name, item.type.name(), item.color.getRGB(), item.width, item.height, item.effects,
            relX.toString(), relY.toString(), item.speed.toString(), item.isSpeed.toString(), item.reverseDirection);
        
        existingTemplates.add(jsonStr);

        try(java.io.FileWriter writer = new java.io.FileWriter(filePath)){
            writer.write("[\n");
            for(int i = 0; i < existingTemplates.size(); i++){
                writer.write(" " + existingTemplates.get(i));
                if(i < existingTemplates.size() - 1) writer.write(",\n");
                else writer.write("\n");
            }
            writer.write("]");
        }catch(java.io.IOException e){}
    }

    //for map button
    public int getNextUnusedMapID(){
        int id = 1;
        while(true){
            boolean occupied = false;
            if(gameCanvas.getGameObjects() != null){
                for(GameObject obj : gameCanvas.getGameObjects()){
                    if(obj.mapID == id){
                        occupied = true;
                        break;
                    }
                }
            }
            if(!occupied && gameCanvas.getMovement() != null && gameCanvas.getMovement().getEnemies() != null){
                for(Enemy e : gameCanvas.getMovement().getEnemies()){
                    if(e.mapID == id){
                        occupied = true;
                        break;
                    }
                }
            }
            if(!occupied && gameCanvas.getMovement().worldItems != null){
                for(Item i : gameCanvas.getMovement().worldItems){
                    if(i.mapID == id){
                        occupied = true;
                        break;
                    }
                }
            }
            if(!occupied){
                return id;
            }
            id++;
        }
    }
    
    public void updatePropertyDialogTextFields(){
        if (dialogTxtX != null && dialogTxtY != null && propertyEditingObject != null && selectedWaypointIndex >= 0) {
            boolean triggered = propertyEditingObject.isTriggered;
            ArrayList<Integer> px = triggered ? propertyEditingObject.triggeredPathX : propertyEditingObject.pathX;
            ArrayList<Integer> py = triggered ? propertyEditingObject.triggeredPathY : propertyEditingObject.pathY;
            if (selectedWaypointIndex < px.size()) {
                dialogTxtX.setText(String.valueOf(px.get(selectedWaypointIndex)));
                dialogTxtY.setText(String.valueOf(py.get(selectedWaypointIndex)));
            }
        }
    }

    public void openInventoryWindow(){
        JDialog inventoryDialog = new JDialog(this, "Player Inventory", true);
        inventoryDialog.setLayout(new BorderLayout());
        inventoryDialog.setSize(300, 400);

        Movement movement = gameCanvas.getMovement();
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for(Item item : movement.inventory){
            String status = item.isEquipped ? " [EQUIPPED]" : "";
            String numberSuffix = (item.inventoryNumber > 0) ? " (" + item.inventoryNumber + ")" : "";
            listModel.addElement(item.name + " (" + item.type + ")" + numberSuffix + status);
        }
        JList<String> itemListView = new JList<>(listModel);
        JScrollPane scrollPane = new JScrollPane(itemListView);

        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem equipItem = new JMenuItem("Equip");
        JMenuItem dropItem = new JMenuItem("Drop");
       
        equipItem.addActionListener(e -> {
            int index = itemListView.getSelectedIndex();
            if(index != -1){
                Item selected = movement.inventory.get(index);
                if(selected.type == Item.ItemType.WEAPON){
                    if(movement.equippedWeapon != null){
                        movement.equippedWeapon.isEquipped = false;
                    }
                    selected.isEquipped = true;
                    movement.equippedWeapon = selected;
                    inventoryDialog.dispose();
                    openInventoryWindow();
                }else{
                    // JOptionPane.showMess
                }
            }
        });

        dropItem.addActionListener(e -> {
            int index = itemListView.getSelectedIndex();
            if(index != -1){
                Item selected = movement.inventory.get(index);
                if(selected == movement.equippedWeapon){
                    movement.equippedWeapon = null;
                }
                selected.isInInventory = false;
                selected.isEquipped = false;
                selected.x = movement.getPlayerX();
                selected.y = movement.getPlayerY();

                movement.worldItems.add(selected);
                movement.inventory.remove(index);
                inventoryDialog.dispose();
                openInventoryWindow();
            }
        });

        popupMenu.add(equipItem);
        popupMenu.add(dropItem);

        itemListView.addMouseListener(new MouseAdapter(){
            public void mousePressed(MouseEvent e){
                if(SwingUtilities.isRightMouseButton(e) || e.getClickCount() == 2){
                    int index = itemListView.locationToIndex(e.getPoint());
                    if(index != -1){
                        itemListView.setSelectedIndex(index);
                        popupMenu.show(itemListView, e.getX(), e.getY());
                    }
                }
            }
        });
        
        inventoryDialog.add(new JLabel(" Right-click items to Equip or Drop:", JLabel.CENTER), BorderLayout.NORTH);
        inventoryDialog.add(scrollPane, BorderLayout.CENTER);
        inventoryDialog.setLocationRelativeTo(this);
        inventoryDialog.setVisible(true);

    }

    //handling winning
    public static void win() {
        if (instance == null) return;
        instance.gameCanvas.setEditorMode(true);
        instance.gameCanvas.setCameraX(0);
        instance.gameCanvas.setCameraY(0);
        instance.btnPlay.setEnabled(true);
        instance.btnExit.setEnabled(false);
        JOptionPane.showMessageDialog(instance, "Level Complete! You Win!", "Victory!", JOptionPane.INFORMATION_MESSAGE);
    }
    
    public void updateZoomLabel(){
        if(lblZoom != null && gameCanvas != null){
            int percent = (int) Math.round(gameCanvas.getEditorZoom() * 100);
            lblZoom.setText("Zoom: " + percent + "%");
        }
    }

    private java.util.List<String> getTemplatesForType(Item.ItemType targetType){
        java.util.List<String> result = new java.util.ArrayList<>();

        for(ItemRegistry reg : ItemRegistry.values()){
            Item tempItem = reg.create(0, 0, 0);
            if(tempItem.type == targetType){
                result.add(tempItem.name);
            }
        }

        java.io.File file = new java.io.File("src/savedObjects.json");
        if(file.exists()){
            try(java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))){
                StringBuilder sb = new StringBuilder();
                String line;
                while((line = reader.readLine()) != null){
                    sb.append(line);
                }
                String content = sb.toString().trim();
                if(content.startsWith("[") && content.endsWith("]")){
                    content = content.substring(1, content.length() - 1).trim();
                }
                if(!content.isEmpty()){
                    String[] parts = content.split("\\},\\s*\\{");
                    for(String p : parts){
                        String jsonBlock = "{" + p.replace("{", "").replace("}", "") + "}";
                        if(jsonBlock.contains("\"kind\":\"item\"") && jsonBlock.contains("\"type\":\"" + targetType.name() + "\"")){
                            int nameIdx = jsonBlock.indexOf("\"name\":\"");
                            if(nameIdx != -1){
                                int start = nameIdx + 8;
                                int end = jsonBlock.indexOf("\"", start);
                                if(end != -1){
                                    String name = jsonBlock.substring(start, end);
                                    if(!result.contains(name)){
                                        result.add(name);
                                    }
                                }
                            }
                        }
                    }
                }

            }catch(IOException ex){}
        }
        if(result.isEmpty()){
            result.add("Default " + targetType.name());
        }
        return result;
    }
 
    private java.util.List<String> getTemplatesForEnemy(EnemyRegistry targetType){
        java.util.List<String> result = new java.util.ArrayList<>();

        if(targetType == null || targetType == EnemyRegistry.NONE){
            result.add(EnemyRegistry.NONE.getDisplayName());
            return result;
        }

        result.add(targetType.getDisplayName());

        java.io.File file = new java.io.File("src/savedObjects.json");
        if(file.exists()){
            try(java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))){
                StringBuilder sb = new StringBuilder();
                String line;
                while((line = reader.readLine()) != null){
                    sb.append(line);
                }
                String content = sb.toString().trim();
                if(content.startsWith("[") && content.endsWith("]")){
                    content = content.substring(1, content.length() - 1).trim();
                }
                if(!content.isEmpty()){
                    String[] parts = content.split("\\},\\s*\\{");
                    for(String p : parts){
                        String jsonBlock = "{" + p.replace("{", "").replace("}", "") + "}";
                        if(jsonBlock.contains("\"kind\":\"enemy\"")){
                            int nameIdx = jsonBlock.indexOf("\"name\":\"");
                            if(nameIdx != -1){
                                int start = nameIdx + 8;
                                int end = jsonBlock.indexOf("\"", start);
                                if(end != -1){
                                    String name = jsonBlock.substring(start, end);
                                    if(!result.contains(name)){
                                        result.add(name);
                                    }
                                }
                            }
                        }
                    }
                }
            }catch(IOException ex){}
        }

        return result;
    }

    private void saveEnemyTemplateManual(String templateName, Enemy e, String filePath){
        java.io.File file = new java.io.File(filePath);
        ArrayList<String> existingTemplates = new ArrayList<>();

        if(file.exists()){
            try(java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))){
                StringBuilder sb = new StringBuilder();
                String line;
                while((line = reader.readLine()) != null) sb.append(line);
                String content = sb.toString().trim();
                if(content.startsWith("[") && content.endsWith("]")){
                    content = content.substring(1, content.length() - 1).trim();
                }
                if(!content.isEmpty()){
                    String[] parts = content.split("\\},\\s*\\{");
                    for(String p : parts){
                        existingTemplates.add("{" + p.replace("{", "").replace("}", "") + "}");
                    }
                }
            }catch(IOException ex){}
        }

        double initialSpeed = (e instanceof FlyingEnemy) ? ((FlyingEnemy) e).getXSpeed() : e.velX;
        String jsonStr;

        if(e instanceof FlyingEnemy){
            FlyingEnemy fe = (FlyingEnemy) e;
            jsonStr = String.format("{\"kind\":\"enemy\", \"name\":\"%s\", \"width\":%d, \"height\":%d, \"velX\":%.4f, \"xRange\":%.2f, \"yRange\":%.2f, \"maxHealth\":%.2f, \"regeneration\":%.2f, \"damage\":%.2f, \"hasPathfinding\":%b, \"type\":\"%s\"}",
                templateName, e.width, e.height, initialSpeed, fe.getXRange(), fe.getYRange(), e.maxHealth, e.regeneration, e.damage, e.hasPathfinding, e.type);
        }else if(e instanceof JumpingEnemy){
            JumpingEnemy je = (JumpingEnemy) e;
            jsonStr = String.format("{\"kind\":\"enemy\", \"name\":\"%s\", \"width\":%d, \"height\":%d, \"velX\":%.4f, \"jumpHeight\":%.4f, \"intervalMax\":%d, \"intervalMin\":%d, \"maxHealth\":%.2f, \"regeneration\":%.2f, \"damage\":%.2f, \"hasPathfinding\":%b, \"type\":\"%s\"}",
                templateName, e.width, e.height, initialSpeed, je.getJumpHeight(), je.getIntervalMax(), je.getIntervalMin(), e.maxHealth, e.regeneration, e.damage, e.hasPathfinding, e.type);
        }else{
            jsonStr = String.format("{\"kind\":\"enemy\", \"name\":\"%s\", \"width\":%d, \"height\":%d, \"velX\":%.4f, \"maxHealth\":%.2f, \"regeneration\":%.2f, \"damage\":%.2f, \"hasPathfinding\":%b, \"type\":\"%s\"}",
                templateName, e.width, e.height, initialSpeed, e.maxHealth, e.regeneration, e.damage, e.hasPathfinding, e.type);
        }

        existingTemplates.add(jsonStr);

        try(java.io.FileWriter writer = new java.io.FileWriter(filePath)){
            writer.write("[\n");
            for(int i = 0; i < existingTemplates.size(); i++){
                writer.write(" " + existingTemplates.get(i));
                if(i < existingTemplates.size() - 1) writer.write(",\n");
                else writer.write("\n");
            }
            writer.write("]");
        }catch(java.io.IOException ex){}
    }

    private Enemy createEnemyFromTemplate(String templateName, int x, int y, int mapID){
        EnemyRegistry reg = EnemyRegistry.fromDisplayName(templateName);
        if(reg != null && reg != EnemyRegistry.NONE){
            return reg.create(x, y, 36, 36, mapID, 0);
        }

        java.io.File file = new java.io.File("src/savedObjects.json");
        if(file.exists()){
            try(java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))){
                StringBuilder sb = new StringBuilder();
                String line;
                while((line = reader.readLine()) != null) sb.append(line);
                String content = sb.toString().trim();
                if(content.startsWith("[") && content.endsWith("]")){
                    content = content.substring(1, content.length() - 1).trim();
                }
                if(!content.isEmpty()){
                    String[] parts = content.split("\\},\\s*\\{");
                    for(String p : parts){
                        String jsonBlock = "{" + p.replace("{", "").replace("}", "") + "}";
                        if(jsonBlock.contains("\"kind\":\"enemy\"") && jsonBlock.contains("\"name\":\"" + templateName + "\"")){
                            int width = 36, height = 36, intervalMax = 180, intervalMin = 60;
                            double velX = 0.1, jumpHeight = -15.5, xRange = 64.0, yRange = 32.0;
                            double maxHealth = 100.0, regeneration = 0.02, damage = 10.0;
                            boolean hasPathfinding = false;
                            String type = "PATROL";

                            String[] props = jsonBlock.replace("{","").replace("}","").split(",");
                            for(String prop : props){
                                String[] kv = prop.split(":", 2);
                                if(kv.length != 2) continue;
                                String k = kv[0].replace("\"", "").trim();
                                String v = kv[1].replace("\"", "").trim();
                                switch(k){
                                    case "width": width = Integer.parseInt(v); break;
                                    case "height": height = Integer.parseInt(v); break;
                                    case "velX": velX = Double.parseDouble(v); break;
                                    case "xRange": xRange = Double.parseDouble(v); break;
                                    case "yRange": yRange = Double.parseDouble(v); break;
                                    case "damage": damage = Double.parseDouble(v); break;
                                    case "maxHealth": maxHealth = Double.parseDouble(v); break;
                                    case "regeneration": regeneration = Double.parseDouble(v); break;
                                    case "jumpHeight": jumpHeight = Double.parseDouble(v); break;
                                    case "intervalMin": intervalMin = Integer.parseInt(v); break;
                                    case "intervalMax": intervalMax = Integer.parseInt(v); break;
                                    case "hasPathfinding": hasPathfinding = Boolean.parseBoolean(v); break;
                                    case "type": type = v; break;
                                }
                            }
                            Enemy enemy = EnemyRegistry.createFromString(type, x, y, width, height, mapID, velX);
                            if(enemy != null){
                                enemy.hasPathfinding = hasPathfinding;
                                enemy.maxHealth = maxHealth;
                                enemy.currentHealth = maxHealth;
                                enemy.regeneration = regeneration;
                                enemy.damage = damage;
                                if(enemy instanceof FlyingEnemy){
                                    ((FlyingEnemy) enemy).setXRange(xRange);
                                    ((FlyingEnemy) enemy).setYRange(yRange);
                                }
                                if(enemy instanceof JumpingEnemy){
                                    ((JumpingEnemy) enemy).setJumpHeight(jumpHeight);
                                    ((JumpingEnemy) enemy).setIntervalMax(intervalMax);
                                    ((JumpingEnemy) enemy).setIntervalMin(intervalMin);
                                }
                                return enemy;
                            }
                        }
                    }
                }
            }catch(Exception ex){}
        }
        return null;
    }
 

    public void saveStateForUndo(){
        String currentState = generateExportJson();
        undoStack.add(currentState);
        redoStack.clear();
    }

    public void performUndo(){
        if(undoStack.isEmpty()) return;
        String currentState = generateExportJson();
        redoStack.add(currentState);
        String previousState = undoStack.remove(undoStack.size() - 1);
        loadStateFromJsonIntoActiveMap(previousState);
        clearSelectionPoints();
    }

    public void performRedo(){
        if(redoStack.isEmpty()) return;
        String currentState = generateExportJson();
        undoStack.add(currentState);
        String nextState = redoStack.remove(redoStack.size() - 1);
        loadStateFromJsonIntoActiveMap(nextState);
        clearSelectionPoints();
    }

    private void loadStateFromJsonIntoActiveMap(String jsonStringData){
        if(jsonStringData == null || jsonStringData.trim().isEmpty()) return;
        jsonStringData = jsonStringData.trim();
        if(jsonStringData.startsWith("[")) jsonStringData = jsonStringData.substring(1);
        if(jsonStringData.endsWith("]")) jsonStringData = jsonStringData.substring(0, jsonStringData.length() - 1);
        jsonStringData = jsonStringData.trim();

        int currentMap = activeEditingMapID;
        gameCanvas.getGameObjects().removeIf(obj -> obj.mapID == currentMap);
        gameCanvas.getMovement().getEnemies().removeIf(e -> e.mapID == currentMap);
        gameCanvas.getMovement().worldItems.removeIf(i -> i.mapID == currentMap);

        if(jsonStringData.isEmpty()){
            gameCanvas.loadLevel(currentMap);
            return;
        }

        processImportJsonIntoMap(jsonStringData, currentMap);
        gameCanvas.loadLevel(currentMap);
    }

    private void processImportJsonIntoMap(String jsonStringData, int targetMapID){
        if(jsonStringData == null || jsonStringData.trim().isEmpty()) return;
        String[] rawItems = jsonStringData.split("\\},\\s*\\{");

        for(String rawItem : rawItems){
            rawItem = rawItem.replace("{", "").replace("}", "").trim();
            if(rawItem.isEmpty()) continue;

            String kind = "object";
            String[] properties = rawItem.split(",(?![^\\[]*\\])");

            for(String prop : properties){
                String[] kv = prop.split(":", 2);
                if(kv.length == 2){
                    String key = kv[0].replace("\"", "").trim();
                    String val = kv[1].replace("\"", "").trim();
                    if(key.equals("kind")){
                        kind = val;
                        break;
                    }
                }
            }

            if(kind.equals("object")){
                int x = 0, y = 0, width = 0, height = 0;
                boolean isTriggered = false, triggeredDisplay = true;
                int triggeredX = -1, triggeredY = -1, triggeredBy = 0, activatesChannel = 0;
                int time = 0, timeElapsed = 0, moveX = 0, moveY = 0, triggerMoveX = 0, triggerMoveY = 0;
                GameObject.Type type = GameObject.Type.solid;
                GameObject.movement move = GameObject.movement.never;
                boolean returnReverse = false;

                ArrayList<Integer> pX = new ArrayList<>();
                ArrayList<Integer> pY = new ArrayList<>();
                ArrayList<Double> pSpd = new ArrayList<>();
                ArrayList<Boolean> pIsSpd = new ArrayList<>();

                ArrayList<Integer> tPX = new ArrayList<>();
                ArrayList<Integer> tPY = new ArrayList<>();
                ArrayList<Double> tPSpd = new ArrayList<>();
                ArrayList<Boolean> tPIsSpd = new ArrayList<>();

                for(String prop : properties){
                    int colonIdx = prop.indexOf(":");
                    if(colonIdx == -1) continue;
                    String key = prop.substring(0, colonIdx).replace("\"", "").trim();
                    String val = prop.substring(colonIdx + 1).replace("\"", "").trim();
                    switch(key){
                        case "x": x = Integer.parseInt(val); break;
                        case "y": y = Integer.parseInt(val); break;
                        case "width": width = Integer.parseInt(val); break;
                        case "height": height = Integer.parseInt(val); break;
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
                        case "returnReverse": returnReverse = Boolean.parseBoolean(val); break;
                        case "moveX": moveX = Integer.parseInt(val); break;
                        case "moveY": moveY = Integer.parseInt(val); break;
                        case "triggerMoveX": triggerMoveX = Integer.parseInt(val); break;
                        case "triggerMoveY": triggerMoveY = Integer.parseInt(val); break;
                        case "pathX":
                            if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) pX.add(Integer.parseInt(s.trim()));
                            break;
                        case "pathY":
                            if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) pY.add(Integer.parseInt(s.trim()));
                            break;
                        case "pathSpeed":
                            if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) pSpd.add(Double.parseDouble(s.trim()));
                            break;
                        case "pathSpeedIsSpeed":
                            if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) pIsSpd.add(Boolean.parseBoolean(s.trim()));
                            break;
                        case "triggeredPathX":
                            if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) tPX.add(Integer.parseInt(s.trim()));
                            break;
                        case "triggeredPathY":
                            if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) tPY.add(Integer.parseInt(s.trim()));
                            break;
                        case "triggeredPathSpeed":
                            if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) tPSpd.add(Double.parseDouble(s.trim()));
                            break;
                        case "triggeredSpeedIsSpeed":
                        case "triggeredPathSpeedIsSpeed":
                            if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) tPIsSpd.add(Boolean.parseBoolean(s.trim()));
                            break;
                    }
                }
                if (triggeredX == -1 && triggeredY == -1) { triggeredX = x; triggeredY = y; }
                if (moveX == 0 && moveY == 0) { moveX = x; moveY = y; }
                if (triggerMoveX == 0 && triggerMoveY == 0) { triggerMoveX = triggeredX; triggerMoveY = triggeredY; }

                if(gameCanvas.getGameObjects() != null){
                    gameCanvas.getGameObjects().add(new GameObject(x, y, width, height, type, targetMapID, isTriggered, triggeredX, triggeredY, triggeredDisplay, triggeredBy, activatesChannel, time, timeElapsed, move, returnReverse, moveX, moveY, triggerMoveX, triggerMoveY, x, y, pX, pY, pSpd, pIsSpd, tPX, tPY, tPSpd, tPIsSpd));
                }
            }else if(kind.equals("enemy")){
                int x = 0, y = 0, width = 36, height = 36;
                double velX = 0.1;
                double jumpHeight = -15.5;
                int intervalMax = 180, intervalMin = 60;
                double xRange = 64.0, yRange = 32.0;
                boolean hasPathfinding = false;
                String type = "PATROL";
                double maxHealth = 100.0, regeneration = 0.0, damage = 10.0;
                
                for(String prop : properties){
                    int colonIdx = prop.indexOf(":");
                    if(colonIdx == -1) continue;
                    String key = prop.substring(0, colonIdx).replace("\"", "").trim();
                    String val = prop.substring(colonIdx + 1).replace("\"", "").trim();
                    switch(key){
                        case "x": x = Integer.parseInt(val); break;
                        case "y": y = Integer.parseInt(val); break;
                        case "width": width = Integer.parseInt(val); break;
                        case "height": height = Integer.parseInt(val); break;
                        case "velX": velX = Double.parseDouble(val); break;
                        case "xRange": xRange = Double.parseDouble(val); break;
                        case "yRange": yRange = Double.parseDouble(val); break;
                        case "maxHealth": maxHealth = Double.parseDouble(val); break;
                        case "regeneration": regeneration = Double.parseDouble(val); break;
                        case "jumpHeight": jumpHeight = Double.parseDouble(val); break;
                        case "intervalMin": intervalMin = Integer.parseInt(val); break;
                        case "intervalMax": intervalMax = Integer.parseInt(val); break;
                        case "hasPathfinding": hasPathfinding = Boolean.parseBoolean(val); break;
                        case "damage": damage = Double.parseDouble(val); break;
                        case "type": type = val; break;
                    }
                }
                Enemy enemy = EnemyRegistry.createFromString(type, x, y, width, height, targetMapID, velX);
                if(enemy != null){
                    enemy.hasPathfinding = hasPathfinding;
                    enemy.maxHealth = maxHealth;
                    enemy.currentHealth = maxHealth;
                    enemy.regeneration = regeneration;
                    enemy.damage = damage;
                    if(enemy instanceof FlyingEnemy){
                        ((FlyingEnemy) enemy).setXRange(xRange);
                        ((FlyingEnemy) enemy).setYRange(yRange);
                    }
                    if(enemy instanceof JumpingEnemy){
                        ((JumpingEnemy) enemy).setJumpHeight(jumpHeight);
                        ((JumpingEnemy) enemy).setIntervalMax(intervalMax);
                        ((JumpingEnemy) enemy).setIntervalMin(intervalMin);
                    }
                    if(gameCanvas.getMovement() != null && gameCanvas.getMovement().getEnemies() != null){
                        gameCanvas.getMovement().getEnemies().add(enemy);
                    }
                }
            }else if(kind.equals("item")){
                int x = 0, y = 0, width = 32, height = 32;
                String name = "COIN", type = "MISC", effects = "";
                Color color = Color.YELLOW;
                Boolean reverse = false;

                ArrayList<Integer> attX = new ArrayList<>();
                ArrayList<Integer> attY = new ArrayList<>();
                ArrayList<Double> spd = new ArrayList<>();
                ArrayList<Boolean> isSpd = new ArrayList<>();

                for(String prop : properties){
                    int colonIdx = prop.indexOf(":");
                    if(colonIdx == -1) continue;
                    String key = prop.substring(0, colonIdx).replace("\"", "").trim();
                    String val = prop.substring(colonIdx + 1).replace("\"", "").trim();
                    switch(key){
                        case "x": x = Integer.parseInt(val); break;
                        case "y": y = Integer.parseInt(val); break;
                        case "width": width = Integer.parseInt(val); break;
                        case "height": height = Integer.parseInt(val); break;
                        case "name": name = val; break;
                        case "type": type = val; break;
                        case "effects": effects = val; break;
                        case "reverseDirection": reverse = Boolean.parseBoolean(val); break;
                        case "colorRGB": color = new Color(Integer.parseInt(val)); break;
                        case "attackX":
                            if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) attX.add(Integer.parseInt(s.trim()));
                            break;
                        case "attackY":
                            if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) attY.add(Integer.parseInt(s.trim()));
                            break;
                        case "speed":
                            if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) spd.add(Double.parseDouble(s.trim()));
                            break;
                        case "isSpeed":
                            if(!val.isEmpty() && !val.equals("[]")) for(String s : val.replace("[", "").replace("]", "").split(",")) if(!s.trim().isEmpty()) isSpd.add(Boolean.parseBoolean(s.trim()));
                            break;
                    }
                }
                Item importedItem = new Item(name, Item.ItemType.valueOf(type), color, x, y, width, height, targetMapID, effects, reverse, attX, attY, spd, isSpd);
                if(gameCanvas.getMovement() != null && gameCanvas.getMovement().worldItems != null){
                    gameCanvas.getMovement().worldItems.add(importedItem);
                }
            }
        }
    }


    public void updateMapIDField(String text){ if(txtMapID != null) txtMapID.setText(text);}
    public void setCurrentlyLoadedMapID(int id){ this.currentlyLoadedMapID = id;}
    
    //camera
    public int getCameraX(){ return this.cameraX;}
    public int getCameraY(){ return this.cameraY;}
    public void setCamLeft(boolean state) { this.camLeft = state;}
    public void setCamRight(boolean state) { this.camRight = state;}
    public void setCamUp(boolean state) { this.camUp = state;}
    public void setCamDown(boolean state) { this.camDown = state;}

    //points
    public Point getBottomLeftCorner(){ return this.bottomLeftCorner;}
    public Point getCurrentHoverPoint(){ return this.currentHoverPoint;}
    public Point getPathHoverPoint() { return this.pathHoverPoint;}

    public boolean getFreePlace(){ return freePlace;}

    //object editing
    public GameObject.Type getActiveBrushType(){ return this.activeBrushType;}
    public GameObject getPropertyEditingObject(){ return this.selectedObject;}
    public int getHighlightedBroadcastChannel(){ return this.highlightedBroadcastChannel;}

    public String getCurrentPathClickModeName() { return this.currentPathClickMode.name();}
    public boolean isSettingPathMode() { return this.currentPathClickMode != PathClickMode.NONE;}

    public Enemy getSelectedEnemy(){ return this.selectedEnemy;}  
    public Item getSelectedItem(){ return this.selectedItem;}
    
    public ResizeMode getCurrentHoverResizeMode(){ return currentHoverResizeMode;}
}