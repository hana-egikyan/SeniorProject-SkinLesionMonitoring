import java.awt.Color;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.LocalTime;

// this class contains the main image analysis algorithm
// it analyzes only the user-selected region produced by region growing
public class LesionAnalyzer {

    // analyze inside a user selection mask
    // selected[y][x] == true means that pixel is included
    public static AnalysisResult analyzeFromSelected(BufferedImage img,
                                                     boolean[][] selected,
                                                     String imagePath,
                                                     String seriesId,
                                                     LocalDate date,
                                                     LocalTime time,
                                                     int toleranceUsed,
                                                     int minR, int maxR,
                                                     int minG, int maxG,
                                                     int minB, int maxB) {

        if (img == null || selected == null) {
            return new AnalysisResult(
                    imagePath, seriesId, date, time,
                    0,    // lesionPixelCount
                    0,    // perimeterPixelCount
                    0,    // circularity
                    0, 0, 0,   // avgR, avgG, avgB
                    0, 0, 0,   // varR, varG, varB
                    toleranceUsed,
                    minR, maxR,
                    minG, maxG,
                    minB, maxB,
                    "NOT_CHECKED"
            );
        }

        int count = 0;

        long sumR = 0;
        long sumG = 0;
        long sumB = 0;

        int h = img.getHeight();
        int w = img.getWidth();

        // FIRST PASS: compute averages
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {

                if (y < selected.length &&
                        x < selected[y].length &&
                        selected[y][x]) {

                    int rgb = img.getRGB(x, y);
                    Color c = new Color(rgb);

                    sumR += c.getRed();
                    sumG += c.getGreen();
                    sumB += c.getBlue();
                    count++;
                }
            }
        }

        if (count == 0) {
            return new AnalysisResult(
                    imagePath, seriesId, date, time,
                    0,    // lesionPixelCount
                    0,    // perimeterPixelCount
                    0,    // circularity
                    0, 0, 0,   // avgR, avgG, avgB
                    0, 0, 0,   // varR, varG, varB
                    toleranceUsed,
                    minR, maxR,
                    minG, maxG,
                    minB, maxB,
                    "NOT_CHECKED"
            );
        }

        double avgR = (double) sumR / count;
        double avgG = (double) sumG / count;
        double avgB = (double) sumB / count;

        //SECOND PASS: compute variance
        double sumSqR = 0;
        double sumSqG = 0;
        double sumSqB = 0;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {

                if (y < selected.length &&
                        x < selected[y].length &&
                        selected[y][x]) {

                    int rgb = img.getRGB(x, y);
                    Color c = new Color(rgb);

                    double dr = c.getRed() - avgR;
                    double dg = c.getGreen() - avgG;
                    double db = c.getBlue() - avgB;

                    sumSqR += dr * dr;
                    sumSqG += dg * dg;
                    sumSqB += db * db;
                }
            }
        }

        double varR = sumSqR / count;
        double varG = sumSqG / count;
        double varB = sumSqB / count;
        int perimeter = computePerimeter4(selected, w, h);
        double circularity = computeCircularity(count, perimeter);

        return new AnalysisResult(
                imagePath, seriesId, date, time,
                count, perimeter, circularity,
                avgR, avgG, avgB,
                varR, varG, varB,
                toleranceUsed,
                minR, maxR,
                minG, maxG,
                minB, maxB,
                "NOT_CHECKED"
        );

    }
        // counts lesion pixels that touch at least one non-lesion 4-neighbor
        static int computePerimeter4(boolean[][] selected, int w, int h) {
            int perimeter = 0;

            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (!selected[y][x]) continue;

                    if (x == 0 || y == 0 || x == w - 1 || y == h - 1 ||
                            !selected[y][x - 1] ||
                            !selected[y][x + 1] ||
                            !selected[y - 1][x] ||
                            !selected[y + 1][x]) {
                        perimeter++;
                    }
                }
            }

            return perimeter;
        }

        static double computeCircularity(int area, int perimeter) {
            if (area <= 0 || perimeter <= 0) return 0;

            double c = (4.0 * Math.PI * area) / (perimeter * (double) perimeter);

            if (c < 0) return 0;
            return Math.min(c, 1.0);
        }

}