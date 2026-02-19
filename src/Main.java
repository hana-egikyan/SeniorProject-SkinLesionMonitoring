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

    static int startX = -1;
    static int startY = -1;

    // path of the currently loaded image (used when saving results)
    static String currentImagePath = "";

    // stores the most recent analysis result
    static AnalysisResult currentResult = null;

    // rgb range input files - ui for color range detection
    static JTextField minRField = new JTextField("0", 3);
    static JTextField maxRField = new JTextField("255", 3);
    static JTextField minGField = new JTextField("0", 3);
    static JTextField maxGField = new JTextField("255", 3);
    static JTextField minBField = new JTextField("0", 3);
    static JTextField maxBField = new JTextField("255", 3);

    static JTextField toleranceField = new JTextField("20", 3);

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
            JButton autoFillButton = new JButton("use clicked color");


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

                int minR = parseIntSafe(minRField.getText(), 0);
                int maxR = parseIntSafe(maxRField.getText(), 255);
                int minG = parseIntSafe(minGField.getText(), 0);
                int maxG = parseIntSafe(maxGField.getText(), 255);
                int minB = parseIntSafe(minBField.getText(), 0);
                int maxB = parseIntSafe(maxBField.getText(), 255);

                RgbRange range = new RgbRange(minR, maxR, minG, maxG, minB, maxB);

                // if user has already clicked on the image, re-run region growing
                if (startX >= 0 && startY >= 0) {
                    runRegionGrow(startX, startY, range);
                }

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


            // this runs when the "use clicked color" button is pressed
            autoFillButton.addActionListener(e -> {

                if (image == null) {
                    infoLabel.setText("load an image first");
                    return;
                }

                if (startX < 0 || startY < 0) {
                    infoLabel.setText("click on the image first");
                    return;
                }

                int tol = parseIntSafe(toleranceField.getText(), 20);     // if the user types something invalid to use 20 as default


                int rgb = image.getRGB(startX, startY);
                Color c = new Color(rgb);

                int r = c.getRed();
                int g = c.getGreen();
                int b = c.getBlue();

                // new rgb range around the clicked pixel and tol decides how wide it should be
                RgbRange range = RgbRange.around(r, g, b, tol);

                minRField.setText(String.valueOf(range.minR));
                maxRField.setText(String.valueOf(range.maxR));
                minGField.setText(String.valueOf(range.minG));
                maxGField.setText(String.valueOf(range.maxG));
                minBField.setText(String.valueOf(range.minB));
                maxBField.setText(String.valueOf(range.maxB));

                infoLabel.setText("RGB range auto-filled from clicked pixel");
            });


            // a panel to hold the button at the top
            JPanel topPanel = new JPanel();
            topPanel.add(loadButton);
            topPanel.add(analyzeButton);
            topPanel.add(saveButton);
            topPanel.add(compareButton);
            topPanel.add(autoFillButton);

            topPanel.add(new JLabel("tol:"));
            topPanel.add(toleranceField);

            // rgb range controls
            topPanel.add(new JLabel("R:"));
            topPanel.add(minRField);
            topPanel.add(new JLabel("-"));
            topPanel.add(maxRField);

            topPanel.add(new JLabel("G:"));
            topPanel.add(minGField);
            topPanel.add(new JLabel("-"));
            topPanel.add(maxGField);

            topPanel.add(new JLabel("B:"));
            topPanel.add(minBField);
            topPanel.add(new JLabel("-"));
            topPanel.add(maxBField);


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

    static int parseIntSafe(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    // this starts selecting nearby pixels from the clicked one
    static void runRegionGrow(int startX, int startY, RgbRange range) {

        if (image == null || range == null) return;

        int w = image.getWidth();
        int h = image.getHeight();

        selected = new boolean[h][w];
        selectedCount = 0;

        Deque<Point> queue = new ArrayDeque<>();
        queue.add(new Point(startX, startY));
        selected[startY][startX] = true;
        selectedCount = 1;

        while (!queue.isEmpty()) {

            Point p = queue.removeFirst();
            int x = p.x;
            int y = p.y;

            tryAdd(x + 1, y, range, queue, w, h);
            tryAdd(x - 1, y, range, queue, w, h);
            tryAdd(x, y + 1, range, queue, w, h);
            tryAdd(x, y - 1, range, queue, w, h);

            if (selectedCount > 80000) break;
        }
    }


    // this checks a neighbor pixel and adds it if it matches the rgb range
    static void tryAdd(int x, int y,
                       RgbRange range,
                       Deque<Point> queue,
                       int w, int h) {

        if (x < 0 || y < 0 || x >= w || y >= h) return;
        if (selected[y][x]) return;

        int rgb = image.getRGB(x, y);
        Color c = new Color(rgb);

        if (range.contains(c.getRed(), c.getGreen(), c.getBlue())) {
            selected[y][x] = true;
            selectedCount++;
            queue.add(new Point(x, y));
        }
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