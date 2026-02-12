import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Deque;
import java.time.LocalTime;



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
    static int threshold = 60;
    
    static int startX = -1;
    static int startY = -1;

    // path of the currently loaded image (used when saving results)
    static String currentImagePath = "";

    // stores the most recent analysis result
    static AnalysisResult currentResult = null;


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

            // new buttons for analysis and saving results and comparing latest two saved images
            JButton analyzeButton = new JButton("analyze");
            JButton saveButton = new JButton("save");
            JButton compareButton = new JButton("compare latest 2");


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

                    // store the path of the loaded image for saving results later
                    currentImagePath = file.getAbsolutePath();

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

            // this runs when the analyze button is clicked
            analyzeButton.addActionListener(e -> {

                if (image == null) {
                    infoLabel.setText("load an image first");
                    return;
                }

                String today = LocalDate.now().toString();

                String dateInput = (String) JOptionPane.showInputDialog(
                        frame,
                        "enter date (YYYY-MM-DD) or leave blank for today",
                        "date",
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        today
                );

                LocalDate date;
                try {
                    if (dateInput == null || dateInput.trim().isEmpty()) {
                        date = LocalDate.now();
                    } else {
                        date = LocalDate.parse(dateInput.trim());
                    }
                } catch (Exception ex) {
                    date = LocalDate.now();
                }

                LocalTime time = LocalTime.now();


                // if bfs selection exists analyze that region
                if (selected != null && selectedCount > 0) {
                    currentResult = LesionAnalyzer.analyzeFromSelected(
                            image, selected, currentImagePath, date, time);
                } else {
                    // no selection yet -> tell the user to click + select first
                    infoLabel.setText("select a region first (click image) then analyze");
                    return;
                }


                infoLabel.setText("pixels " + currentResult.lesionPixelCount +
                        " avg rgb " +
                        String.format("%.2f", currentResult.avgR) + " " +
                        String.format("%.2f", currentResult.avgG) + " " +
                        String.format("%.2f", currentResult.avgB));
            });

            // this runs when the save button is clicked
            saveButton.addActionListener(e -> {

                if (currentResult == null) {
                    infoLabel.setText("run analyze first");
                    return;
                }

                // save to file (txt)
                ResultStorage.save(currentResult);
                infoLabel.setText("saved (check data folder)");

            });

            // this runs when compare is clicked
            compareButton.addActionListener(e -> {

                java.util.List<AnalysisResult> all = ResultStorage.loadAll();

                String text = Comparer.compareLatestTwo(all);

                JOptionPane.showMessageDialog(frame, text);
            });

            // a panel to hold the button at the top
            JPanel topPanel = new JPanel();
            topPanel.add(loadButton);
            topPanel.add(analyzeButton);
            topPanel.add(saveButton);
            topPanel.add(compareButton);

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

            // already selected?
            if (!selected[y][x]) return false;

            // border of image is automatically an edge
            if (x == 0 || y == 0 || x == w - 1 || y == h - 1) return true;

            // edge if any 4-neighbor is not selected
            return !selected[y][x - 1] || !selected[y][x + 1] || !selected[y - 1][x] || !selected[y + 1][x];
        }
}