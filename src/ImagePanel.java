import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ImagePanel extends JPanel {

    public ImagePanel() {

        // this listens for mouse clicks on the panel
        // we do this here because the image is drawn inside this panel
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {

                // if no image is loaded we do nothing
                if (Main.image == null) {
                    Main.infoLabel.setText("load an image first");
                    return;
                }

                int x = e.getX();
                int y = e.getY();

                // this checks if the click is inside the image
                if (x < 0 || y < 0 || x >= Main.image.getWidth() || y >= Main.image.getHeight()) {
                    Main.infoLabel.setText("click inside the image area");
                    return;
                }

                // this gets the rgb value from the image
                int rgb = Main.image.getRGB(x, y);

                // this makes it easier to read r g b separately
                Color c = new Color(rgb);

                // start region growing from clicked pixel
                Main.startX = x;
                Main.startY = y;

                // new for bfs
                Main.runRegionGrow(x, y);

                // this shows the click position and pixel color
                // later we can use this for region growing thresholds
                Main.infoLabel.setText("x " + x + " y " + y +
                        "  r " + c.getRed() +
                        " g " + c.getGreen() +
                        " b " + c.getBlue() +
                        " selected " + Main.selectedCount);

                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // if no image is loaded we show a message
        if (Main.image == null) {
            return;
        }

        // this draws the image starting from the top left corner
        g.drawImage(Main.image, 0, 0, null);

        // this draws the selected region on top of the image
        if (Main.selected != null) {
            int w = Main.image.getWidth();
            int h = Main.image.getHeight();

            g.setColor(new Color(255, 0, 0, 180));

            for (int yy = 0; yy < h; yy++) {
                for (int xx = 0; xx < w; xx++) {
                    if (Main.isEdge(xx, yy, w, h)) {
                        g.fillRect(xx, yy, 1, 1);
                    }
                }
            }
        }
    }
}
