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
    // it is here so other parts of the program can use it
    static BufferedImage image = null;

    // this is for showing simple info to the user
    // later this will help for starting the use of thresholds
    static JLabel infoLabel = new JLabel("Click load image to choose a file");

    // bfs region growing prototype - storing the pixels and the starting pixels, how many are selected, how similar they need to be
    static boolean[][] selected = null;
    static int selectedCount = 0;

    static int startX = -1;
    static int startY = -1;

    // path of the currently loaded image (used when saving results)
    static String currentImagePath = "";
    static String currentSeriesId = "";

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

    static final int PERF_MAX_W = 512;
    static final int PERF_MAX_H = 512;
    static final long PERF_TARGET_MS = 2000;

    // hard upper bound on how many pixels region growing is allowed to accept
    // before it self-terminates. this is an image-independent runtime safeguard
    // against runaway expansion. a separate, image-dependent acceptance rule in
    // AcceptanceCriteria additionally rejects any result whose selected region
    // exceeds 80% of the image area. the two safeguards are complementary:
    // this one limits CPU work inside the inner loop; the other validates the
    // final region size against the image once growing has finished.
    static final int REGION_GROW_HARD_CAP = 80000;

    static long lastAnalysisDurationMs = -1;

    public static void main(String[] args)
     {

        // swing needs to run on its own thread
        // this helps avoid weird bugs with the window
        SwingUtilities.invokeLater(() -> {

            // for the main window
            JFrame frame = new JFrame("LesionTracker");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);

            // this button lets the user choose an image file
            JButton loadButton = new JButton("Load image");

            // new buttons for analysis and saving results and comparing latest two saved images
            JButton analyzeButton = new JButton("Analyze");
            JButton saveButton = new JButton("Save result");
            JButton compareButton = new JButton("Compare with Previous");
            JButton autoFillButton = new JButton("Auto-Fill RGB Range");
            JButton forecastButton = new JButton("Forecast Trend");


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
                        if (image.getWidth() <= PERF_MAX_W && image.getHeight() <= PERF_MAX_H) {
                            infoLabel.setText("Click on the image to see pixel values (within 512x512 performance target)");
                        } else {
                            infoLabel.setText("Click on the image to see pixel values (image is larger than 512x512 target range)");
                        }

                        // to tell swing to redraw the panel
                        imagePanel.repaint();
                    } catch (Exception ex) {
                        // this shows an error if the image fails to load
                        JOptionPane.showMessageDialog(frame,
                                "Image could not be loaded.");
                    }
                }
            });

            // this runs when the analyze button is clicked
            analyzeButton.addActionListener(e -> {

                if (image == null) {
                    infoLabel.setText("Load an image first.");
                    return;
                }

                if (startX < 0 || startY < 0) {
                    setStage("Click on the lesion first.");
                    return;
                }

                String today = LocalDate.now().toString();

                String nowTime = LocalTime.now().withSecond(0).withNano(0).toString();

                String dateInput = (String) JOptionPane.showInputDialog(
                        frame,
                        "Enter picture date (YYYY-MM-DD) or leave blank for today.",
                        "Picture date",
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        today
                );

                if (dateInput == null) {
                    infoLabel.setText("Analysis cancelled");
                    return;
                }

                String timeInput = (String) JOptionPane.showInputDialog(
                        frame,
                        "Enter picture time (HH:MM) or leave blank for current time",
                        "Picture time",
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        nowTime
                );

                if (timeInput == null) {
                    infoLabel.setText("Analysis cancelled");
                    return;
                }

                LocalDate date;
                try {
                    if (dateInput.trim().isEmpty()) {
                        date = LocalDate.now();
                    } else {
                        date = LocalDate.parse(dateInput.trim());
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame,
                            "Invalid date format, using today");
                    date = LocalDate.now();
                }

                LocalTime time;
                try {
                    if (timeInput.trim().isEmpty()) {
                        time = LocalTime.now().withNano(0);
                    } else {
                        time = LocalTime.parse(timeInput.trim());
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame,
                            "Invalid time format, using current time");
                    time = LocalTime.now().withSecond(0).withNano(0);
                }

                int minR = parseIntSafe(minRField.getText(), 0);
                int maxR = parseIntSafe(maxRField.getText(), 255);
                int minG = parseIntSafe(minGField.getText(), 0);
                int maxG = parseIntSafe(maxGField.getText(), 255);
                int minB = parseIntSafe(minBField.getText(), 0);
                int maxB = parseIntSafe(maxBField.getText(), 255);

                int tol = parseIntSafe(toleranceField.getText(), 20);

                RgbRange range = new RgbRange(minR, maxR, minG, maxG, minB, maxB);

                long analysisStartNs = System.nanoTime();

                // determinism sanity check:
                // run region growing twice with identical inputs and compare the resulting region sizes. since the algorithm is deterministic by
                // design (same seed, same range, same visitation matrix), these counts should always match. this check exists to catch any
                // future regression that accidentally introduces nondeterminism (e.g. parallel expansion, hash-based ordering, shared state
                // bleeding between runs). if the counts are different, the result is rejected before being analyzed or saved.

                runRegionGrow(startX, startY, range);
                int count1 = selectedCount;

                runRegionGrow(startX, startY, range);
                int count2 = selectedCount;

                if (count1 != count2) {
                    setStage("acceptance: FAIL stability (" + count1 + " vs " + count2 + ")");
                    infoLabel.setText("stability failed, try again or adjust thresholds");
                    return;
                }

                String seriesInput = (String) JOptionPane.showInputDialog(
                        frame,
                        "Enter lesion series ID (use the same ID for the same lesion over time)",
                        "Lesion series",
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        currentSeriesId.isBlank() ? "lesion1" : currentSeriesId
                );

                if (seriesInput == null) {
                    infoLabel.setText("Analysis cancelled");
                    return;
                }

                currentSeriesId = seriesInput.trim();
                if (currentSeriesId.isEmpty()) {
                    currentSeriesId = "lesion1";
                }

                // if bfs selection exists analyze that region
                if (selected != null && selectedCount > 0) {
                    currentResult = LesionAnalyzer.analyzeFromSelected(
                            image, selected, currentImagePath, currentSeriesId, date, time,
                            tol,
                            minR, maxR,
                            minG, maxG,
                            minB, maxB
                    );
                } else {
                    // no selection yet -> tell the user to click + select first
                    infoLabel.setText("Select a region first (click image) then analyze");
                    return;
                }

                lastAnalysisDurationMs = (System.nanoTime() - analysisStartNs) / 1_000_000;

                // acceptance criteria check (basic correctness rules)
                String verdict = AcceptanceCriteria.validate(
                        selected,
                        image.getWidth(),
                        image.getHeight(),
                        currentResult,
                        selectedCount
                );

                currentResult.acceptanceVerdict = verdict;

                // show pass/fail clearly
                setStage("acceptance: " + verdict);

                String perfText;

                if (isWithinPerformanceTargetSize()) {
                    if (lastAnalysisDurationMs <= PERF_TARGET_MS) {
                        perfText = " performance OK (" + lastAnalysisDurationMs + " ms)";
                    } else {
                        perfText = " performance above target (" + lastAnalysisDurationMs + " ms)";
                    }
                } else {
                    perfText = " runtime " + lastAnalysisDurationMs + " ms (image exceeds 512x512 target range)";
                }

                infoLabel.setText("Area " + currentResult.lesionPixelCount +
                        " perimeter " + currentResult.perimeterPixelCount +
                        " circularity " + String.format("%.4f", currentResult.circularity) +
                        " avg rgb " +
                        String.format("%.2f", currentResult.avgR) + " " +
                        String.format("%.2f", currentResult.avgG) + " " +
                        String.format("%.2f", currentResult.avgB) +
                        " var rgb " +
                        String.format("%.2f", currentResult.varR) + " " +
                        String.format("%.2f", currentResult.varG) + " " +
                        String.format("%.2f", currentResult.varB) + perfText);

                setStage("acceptance: " + verdict + " | " + lastAnalysisDurationMs + " ms");
                imagePanel.repaint();

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

                if (currentImagePath == null || currentImagePath.isBlank()) {
                    infoLabel.setText("Load the lesion image first");
                    return;
                }

                java.util.List<AnalysisResult> filtered = getRelevantSavedResults();

                if (filtered.size() < 2) {
                    JOptionPane.showMessageDialog(
                            frame,
                            "Not enough PASS results for the currently loaded image.\n" +
                                    "Current image: " + currentImageName() + "\n" +
                                    "Current series: " + currentSeriesId + "\n" +
                                    "Need at least 2 saved analyses in the same lesion series."
                    );
                    return;
                }

                showComparisonDialog(frame, filtered);
            });

            // this runs when forecast is clicked
            forecastButton.addActionListener(e -> {

                if (currentImagePath == null || currentImagePath.isBlank()) {
                    infoLabel.setText("Load the lesion image first");
                    return;
                }

                setStage("Loading relevant saved results...");

                java.util.List<AnalysisResult> filtered = getRelevantSavedResults();

                String daysInput = JOptionPane.showInputDialog(
                        frame,
                        "How many days ahead? (project the trend this far into the future)",
                        "7"
                );

                if (daysInput == null) {
                    infoLabel.setText("Forecast cancelled");
                    return;
                }

                int daysAhead;
                try {
                    daysAhead = Integer.parseInt(daysInput.trim());
                    if (daysAhead < 1) daysAhead = 7;
                } catch (Exception ex) {
                    daysAhead = 7;
                }

                if (filtered.size() < 2) {
                    setStage("Done");
                    JOptionPane.showMessageDialog(
                            frame,
                            "Not enough PASS results for forecasting on the currently loaded image.\n" +
                                    "Current image: " + currentImageName() + "\n" +
                                    "Need at least 2 saved accepted analyses in the same lesion series."
                    );
                    return;
                }

                setStage("Computing forecast...");

                TemporalForecaster.ForecastReport report =
                        TemporalForecaster.buildReport(filtered, daysAhead);

                setStage("Done");

                javax.swing.JTextArea textArea = new javax.swing.JTextArea(report.toText());
                textArea.setEditable(false);
                textArea.setLineWrap(true);
                textArea.setWrapStyleWord(true);
                textArea.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 13));
                textArea.setCaretPosition(0);

                javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane(textArea);
                scrollPane.setPreferredSize(new java.awt.Dimension(560, 480));

                JOptionPane.showMessageDialog(frame, scrollPane, "Forecast Report", JOptionPane.INFORMATION_MESSAGE);            });


            // this runs when the "use clicked color" button is pressed
            autoFillButton.addActionListener(e -> {

                if (image == null) {
                    infoLabel.setText("Load an image first");
                    return;
                }

                if (startX < 0 || startY < 0) {
                    infoLabel.setText("Click on the image first");
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
            topPanel.add(autoFillButton);
            topPanel.add(analyzeButton);
            topPanel.add(saveButton);
            topPanel.add(compareButton);
            topPanel.add(forecastButton);

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

            // wrap the top panel in a scroll pane
            JScrollPane topScroll = new JScrollPane(
                    topPanel,
                    JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
            );
            topScroll.setBorder(null);
            topScroll.getHorizontalScrollBar().setUnitIncrement(16);

            // lock the height so the scroll pane doesn't steal vertical space
            topScroll.setPreferredSize(new Dimension(
                    800,
                    topPanel.getPreferredSize().height + 18  // +18 for scrollbar
            ));

            // a panel to hold the label at the bottom
            JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            bottomPanel.add(infoLabel);

            // this arranges everything in the window
            frame.setLayout(new BorderLayout());
            frame.add(topScroll, BorderLayout.NORTH);
            frame.add(imagePanel, BorderLayout.CENTER);
            frame.add(bottomPanel, BorderLayout.SOUTH);

            // this centers the window on the screen
            frame.setLocationRelativeTo(null);

            // this makes the window visible
            frame.setVisible(true);
        });
    }

    static JLabel buildImagePreview(AnalysisResult r, int maxW, int maxH) {
        try {
            BufferedImage img = ImageIO.read(new File(r.imagePath));
            if (img != null) {
                Image scaled = img.getScaledInstance(maxW, maxH, Image.SCALE_SMOOTH);
                ImageIcon icon = new ImageIcon(scaled);

                String caption = "<html><center>"
                        + "Date: " + r.date + "<br>"
                        + "Time: " + r.time + "<br>"
                        + "Series: " + r.seriesId
                        + "</center></html>";

                JLabel label = new JLabel(caption, icon, SwingConstants.CENTER);
                label.setHorizontalTextPosition(SwingConstants.CENTER);
                label.setVerticalTextPosition(SwingConstants.BOTTOM);
                return label;
            }
        } catch (Exception ignored) {
        }

        String fallback = "<html><center>No image preview available<br>"
                + "Date: " + r.date + "<br>"
                + "Time: " + r.time + "<br>"
                + "Series: " + r.seriesId
                + "</center></html>";

        return new JLabel(fallback, SwingConstants.CENTER);
    }

    static void showComparisonDialog(JFrame frame, java.util.List<AnalysisResult> filtered) {
        if (filtered == null || filtered.size() < 2) return;

        AnalysisResult previous = filtered.get(filtered.size() - 2);
        AnalysisResult current = filtered.get(filtered.size() - 1);

        String text = Comparer.compareLatestTwo(filtered);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));

        JPanel imagesPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        imagesPanel.add(buildImagePreview(previous, 220, 220));
        imagesPanel.add(buildImagePreview(current, 220, 220));

        JTextArea textArea = new JTextArea(text);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(520, 260));

        mainPanel.add(imagesPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        JOptionPane.showMessageDialog(
                frame,
                mainPanel,
                "Comparison of Latest Two Accepted Results",
                JOptionPane.PLAIN_MESSAGE
        );
    }

    // keeps only PASS results from the same lesion series as the currently loaded analysis
    static java.util.List<AnalysisResult> getRelevantSavedResults() {

        java.util.List<AnalysisResult> all = ResultStorage.loadAll();
        java.util.List<AnalysisResult> filtered = new java.util.ArrayList<>();

        if (currentImagePath == null || currentImagePath.isBlank()) {
            return filtered;
        }

        for (AnalysisResult r : all) {
            if (r == null) continue;
            if (r.imagePath == null) continue;
            if (!currentSeriesId.equals(r.seriesId)) continue;
            if (r.acceptanceVerdict == null) continue;
            if (!r.acceptanceVerdict.startsWith("PASS")) continue;

            filtered.add(r);
        }

        return filtered;
    }

    // optional helper
    static String currentImageName() {
        if (currentImagePath == null || currentImagePath.isBlank()) return "(no image loaded)";
        return new java.io.File(currentImagePath).getName();
    }

    // small helper so we can show what stage we are in that uses SwingUtilities so it updates safely
    static void setStage(String text) {
        SwingUtilities.invokeLater(() -> infoLabel.setText("Stage: " + text));
    }

    static int parseIntSafe(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    static boolean isWithinPerformanceTargetSize() {
        return image != null &&
                image.getWidth() <= PERF_MAX_W &&
                image.getHeight() <= PERF_MAX_H;
    }

    // this starts selecting nearby pixels from the clicked one
    // runs region growing starting from the clicked pixel
    static void runRegionGrow(int startX, int startY, RgbRange range) {

        if (image == null || range == null) return;

        // show what is happening in the UI
        setStage("region growing...");

        int w = image.getWidth();
        int h = image.getHeight();

        selected = new boolean[h][w];
        selectedCount = 0;

        Deque<Point> queue = new ArrayDeque<>();

        // start from the seed
        queue.add(new Point(startX, startY));
        selected[startY][startX] = true;
        selectedCount = 1;

        while (!queue.isEmpty()) {

            Point p = queue.removeFirst();
            int x = p.x;
            int y = p.y;

            // expand to 4 neighbors
            tryAdd(x + 1, y, range, queue, w, h);
            tryAdd(x - 1, y, range, queue, w, h);
            tryAdd(x, y + 1, range, queue, w, h);
            tryAdd(x, y - 1, range, queue, w, h);

            // image-independent runtime safeguard: see REGION_GROW_HARD_CAP
            // declaration for the relationship to AcceptanceCriteria's 80% rule.
            if (selectedCount > REGION_GROW_HARD_CAP) break;
        }

        // show end stage + result size
        setStage("region growing done (selected " + selectedCount + ")");
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