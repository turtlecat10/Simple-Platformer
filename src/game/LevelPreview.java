package src.game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class LevelPreview extends JPanel {
    private final int targetMapID;
    private final MapEditorWindow windowReference;
    private final Graphics canvasReference;
    
    public LevelPreview(int mapID, MapEditorWindow window, Graphics canvas) {
        this.targetMapID = mapID;
        this.windowReference = window;
        this.canvasReference = canvas;
        
        // Setup individual card geometry
        setPreferredSize(new Dimension(200, 150));
        setMaximumSize(new Dimension(200, 150));
        setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));
        setBackground(Color.BLACK);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Click interaction: Load selected map automatically into active workspace
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                windowReference.activeEditingMapID = targetMapID;
                windowReference.updateMapIDField(String.valueOf(targetMapID));
                canvasReference.loadLevel(targetMapID);
                windowReference.setCurrentlyLoadedMapID(targetMapID);
                windowReference.restoreGameView(); // Closes menu
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                setBorder(BorderFactory.createLineBorder(Color.GREEN, 2));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));
            }
        });
    }

    @Override
    protected void paintComponent(java.awt.Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Render target map level outline without showing player position
        canvasReference.drawLevelCanvas(g2, targetMapID, false, getWidth(), getHeight());
        g2.dispose();
        
        // Draw card footer info overlay
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, getHeight() - 24, getWidth(), 24);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.drawString("Level Map ID: " + targetMapID, 10, getHeight() - 7);
    }
}
