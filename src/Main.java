import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.util.ArrayDeque;
import java.util.Deque;

public class Main {

    // this variable will store the image after we load it
    // we keep it here so other parts of the program can use it
    static BufferedImage image = null;

    // this is for showing simple info to the user
    // later this will help when we start using thresholds
    static JLabel infoLabel = new JLabel("Click load image to choose a file");

    // bfs region growing prototype - storing the pixels and the starting pixels, how many are selected, how similar they need to be
    static boolean[][] selected = null;
    static int selectedCount = 0;
    static int threshold = 35;
    
    static int startX = -1;
    static int startY = -1;

    public static void main(String[] args) {

        // swing needs to run on its own thread
        // this helps avoid weird bugs with the window
        SwingUtilities.invokeLater(() -> {

            // for the main window
            JFrame frame = new JFrame("LesionTracker");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);

            // this button lets the user choose an image file
            JButton loadButton = new JButton("load image");
            
            // this panel will be used to draw the image
            ImagePanel imagePanel = new ImagePanel();

            // this runs when the button is clicked
            loadButton.addActionListener(e -> {

                // this opens a file picker
                JFileChooser chooser = new JFileChooser();
                int result = chooser.showOpenDialog(frame);

                // this checks if the user actually selected a file
                if (result == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();

                    try {
                        // to read the image from the file
                        image = ImageIO.read(file);
                        
                        // reset bfs data for the new image
                        selected = new boolean[image.getHeight()][image.getWidth()];
                        selectedCount = 0;
                        startX = -1;
                        startY = -1;

                        // this tells the user what to do next
                        infoLabel.setText("Click on the image to see pixel values");

                        // to tell swing to redraw the panel
                        imagePanel.repaint();
                    } catch (Exception ex) {
                        // this shows an error if the image fails to load
                        JOptionPane.showMessageDialog(frame,
                                "image could not be loaded");
                    }
                }
            });

            // a panel to hold the button at the top
            JPanel topPanel = new JPanel();
            topPanel.add(loadButton);

            // a panel to hold the label at the bottom
            JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            bottomPanel.add(infoLabel);

            // this arranges everything in the window
            frame.setLayout(new BorderLayout());
            frame.add(topPanel, BorderLayout.NORTH);
            frame.add(imagePanel, BorderLayout.CENTER);
            frame.add(bottomPanel, BorderLayout.SOUTH);

            // this centers the window on the screen
            frame.setLocationRelativeTo(null);

            // this makes the window visible
            frame.setVisible(true);
        });
    }

    // this starts selecting nearby pixels from the clicked one
    static void runRegionGrow(int startX, int startY) {

        int w = image.getWidth();
        int h = image.getHeight();

        selected = new boolean[h][w];
        selectedCount = 0;

        int startRgb = image.getRGB(startX, startY);

        Deque<Point> queue = new ArrayDeque<>();
        queue.add(new Point(startX, startY));
        selected[startY][startX] = true;
        selectedCount = 1;

        while (!queue.isEmpty()) {
            Point p = queue.removeFirst();

            int x = p.x;
            int y = p.y;

            tryAdd(x + 1, y, startRgb, queue, w, h);
            tryAdd(x - 1, y, startRgb, queue, w, h);
            tryAdd(x, y + 1, startRgb, queue, w, h);
            tryAdd(x, y - 1, startRgb, queue, w, h);

            if (selectedCount > 80000) break;
        }
    }

    // this checks a neighbor pixel and adds it if it is similar enough
    static void tryAdd(int x, int y, int startRgb, Deque<Point> queue, int w, int h) {

        if (x < 0 || y < 0 || x >= w || y >= h) return;
        if (selected[y][x]) return;

        int rgb = image.getRGB(x, y);

        if (closeEnough(rgb, startRgb)) {
            selected[y][x] = true;
            selectedCount++;
            queue.add(new Point(x, y));
        }
    }

    // this compares two pixels and checks if their colors are close
    static boolean closeEnough(int a, int b) {

        Color c1 = new Color(a);
        Color c2 = new Color(b);

        int diff =
                Math.abs(c1.getRed() - c2.getRed()) +
                Math.abs(c1.getGreen() - c2.getGreen()) +
                Math.abs(c1.getBlue() - c2.getBlue());

        return diff <= threshold;
    }
    
        // checks if this pixel is on the border of the selected area
        static boolean isEdge(int x, int y, int w, int h) {
            if (!selected[y][x]) return false;
        
            if (x == 0 || y == 0 || x == w - 1 || y == h - 1) return true;
        
            if (!selected[y][x - 1]) return true;
            if (!selected[y][x + 1]) return true;
            if (!selected[y - 1][x]) return true;
            if (!selected[y + 1][x]) return true;
        
            return false;
        }
}