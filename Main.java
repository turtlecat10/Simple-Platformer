import javax.swing.JFrame;
public class Main{
    public static void main(String[] args){
        JFrame window = new JFrame("Simple Platformer");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(true);
        Graphics gameGraphics = new Graphics();
        window.add(gameGraphics);
        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);
    }
}